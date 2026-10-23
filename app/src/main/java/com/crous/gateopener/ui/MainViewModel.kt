package com.crous.gateopener.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crous.gateopener.data.AppPreferences
import com.crous.gateopener.data.GateSettings
import com.crous.gateopener.data.TriggerLog
import com.crous.gateopener.data.TriggerLogRepository
import com.crous.gateopener.geofence.GeofenceBroadcastReceiver
import com.crous.gateopener.geofence.GeofenceManager
import com.crous.gateopener.telephony.CallTriggerHelper
import com.crous.gateopener.wifi.WifiChecker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PermissionState(
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasCallPhone: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasPostNotifications: Boolean = false
) {
    val isAllCriticalGranted: Boolean
        get() = hasFineLocation && hasBackgroundLocation && hasCallPhone && hasOverlayPermission && isIgnoringBatteryOptimizations
}

class MainViewModel(
    private val appPreferences: AppPreferences,
    private val logRepository: TriggerLogRepository
) : ViewModel() {

    val settings: StateFlow<GateSettings> = appPreferences.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GateSettings()
    )

    val logs: StateFlow<List<TriggerLog>> = logRepository.logsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _permissions = MutableStateFlow(PermissionState())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _currentWifi = MutableStateFlow<Pair<String?, String?>>(Pair(null, null))
    val currentWifi: StateFlow<Pair<String?, String?>> = _currentWifi.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshState(context: Context) {
        checkPermissions(context)
        refreshWifi(context)
    }

    fun checkPermissions(context: Context) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val hasCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        _permissions.value = PermissionState(
            hasFineLocation = hasFine,
            hasBackgroundLocation = hasBg,
            hasCallPhone = hasCall,
            hasOverlayPermission = hasOverlay,
            isIgnoringBatteryOptimizations = isIgnoringBattery,
            hasPostNotifications = hasNotifications
        )
    }

    fun refreshWifi(context: Context) {
        _currentWifi.value = WifiChecker.getCurrentWifiInfo(context)
    }

    fun captureCurrentWifi(context: Context) {
        val info = WifiChecker.getCurrentWifiInfo(context)
        val ssid = info.first ?: ""
        val bssid = info.second ?: ""
        viewModelScope.launch {
            appPreferences.updateHomeWifi(ssid, bssid)
            _statusMessage.value = if (ssid.isNotBlank() || bssid.isNotBlank()) {
                "Wi-Fi capturé : SSID=\"$ssid\", BSSID=$bssid"
            } else {
                "Aucun Wi-Fi actif détecté à capturer."
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun useCurrentGpsLocation(context: Context) {
        viewModelScope.launch {
            if (!_permissions.value.hasFineLocation) {
                _statusMessage.value = "Permission de localisation requise pour capturer la position GPS."
                return@launch
            }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()

                if (location != null) {
                    val currentSettings = settings.value
                    appPreferences.updateGeofenceCoordinates(
                        lat = location.latitude,
                        lng = location.longitude,
                        radius = currentSettings.geofenceRadius,
                        responsivenessMs = currentSettings.responsivenessMs
                    )
                    _statusMessage.value = "Position GPS capturée : ${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}"
                } else {
                    _statusMessage.value = "Impossible de fixer le GPS actuellement. Réessayez à l'extérieur."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Erreur GPS : ${e.message}"
            }
        }
    }

    fun toggleGeofence(context: Context, targetActive: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            if (targetActive) {
                if (!_permissions.value.hasFineLocation || !_permissions.value.hasBackgroundLocation) {
                    _statusMessage.value = "Veuillez accorder la localisation en arrière-plan avant d'armer le geofence."
                    return@launch
                }
                val result = GeofenceManager.registerGeofence(
                    context = context,
                    latitude = current.geofenceLat,
                    longitude = current.geofenceLng,
                    radiusMeters = current.geofenceRadius,
                    responsivenessMs = current.responsivenessMs
                )
                if (result.isSuccess) {
                    appPreferences.setGeofenceActive(true)
                    logRepository.addLog("GEOFENCE_ARM", true, "Geofence armé avec succès (rayon ${current.geofenceRadius.toInt()}m)")
                    _statusMessage.value = "Geofence activé et armé avec succès !"
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    _statusMessage.value = "Échec d'armement : $err"
                }
            } else {
                GeofenceManager.removeGeofence(context)
                appPreferences.setGeofenceActive(false)
                logRepository.addLog("GEOFENCE_DISARM", true, "Geofence désarmé manuellement")
                _statusMessage.value = "Geofence désarmé."
            }
        }
    }

    fun simulateTrigger(context: Context) {
        val simIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_SIMULATE_TRIGGER
        }
        context.sendBroadcast(simIntent)
        _statusMessage.value = "Simulation de franchissement envoyée au récepteur passif !"
    }

    fun testDirectCall(context: Context) {
        viewModelScope.launch {
            val current = settings.value
            val res = CallTriggerHelper.executeGateCall(
                context = context,
                phoneNumber = current.targetPhoneNumber,
                isDryRun = current.isDryRunMode
            )
            if (res.isSuccess) {
                logRepository.addLog("CALL_MANUAL_TEST", true, res.getOrNull() ?: "Test validé")
                _statusMessage.value = res.getOrNull()
            } else {
                val err = res.exceptionOrNull()?.message ?: "Erreur"
                logRepository.addLog("CALL_MANUAL_TEST", false, "Échec test : $err")
                _statusMessage.value = "Échec : $err"
            }
        }
    }

    fun updatePhoneNumber(number: String) {
        viewModelScope.launch { appPreferences.updatePhoneNumber(number) }
    }

    fun updateCoordinates(lat: Double, lng: Double, radius: Float, responsiveness: Int) {
        viewModelScope.launch { appPreferences.updateGeofenceCoordinates(lat, lng, radius, responsiveness) }
    }

    fun updateCooldown(minutes: Int) {
        viewModelScope.launch { appPreferences.updateCooldownMinutes(minutes) }
    }

    fun updateWifi(ssid: String, bssid: String) {
        viewModelScope.launch { appPreferences.updateHomeWifi(ssid, bssid) }
    }

    fun setDryRun(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDryRunMode(enabled) }
    }

    fun clearLogs() {
        viewModelScope.launch { logRepository.clearLogs() }
    }
}

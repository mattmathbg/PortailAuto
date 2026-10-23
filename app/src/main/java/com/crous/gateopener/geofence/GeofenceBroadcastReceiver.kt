package com.crous.gateopener.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crous.gateopener.data.AppPreferences
import com.crous.gateopener.data.TriggerLogRepository
import com.crous.gateopener.telephony.CallTriggerHelper
import com.crous.gateopener.wifi.WifiChecker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GateGeofenceReceiver"
        const val ACTION_SIMULATE_TRIGGER = "com.crous.gateopener.ACTION_SIMULATE_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleEvent(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur dans le récepteur Geofence", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleEvent(context: Context, intent: Intent) {
        val logRepo = TriggerLogRepository(context)
        val prefs = AppPreferences(context)
        val settings = prefs.getSettings()

        val isSimulation = intent.action == ACTION_SIMULATE_TRIGGER
        val eventType = if (isSimulation) "SIMULATION_TEST" else "GEOFENCE_ENTER"

        if (!isSimulation) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent == null) {
                Log.w(TAG, "Événement Geofencing null reçu")
                return
            }

            if (geofencingEvent.hasError()) {
                val errorCode = geofencingEvent.errorCode
                logRepo.addLog(eventType, false, "Erreur Google Play Services (code: $errorCode)")
                return
            }

            if (geofencingEvent.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(TAG, "Transition ignorée : ${geofencingEvent.geofenceTransition}")
                return
            }
        }

        // 1. Verrou anti-faux positif : Wi-Fi du logement
        val wifiCheck = WifiChecker.checkHomeWifiFailSafe(
            context = context,
            targetSsid = settings.homeWifiSsid,
            targetBssid = settings.homeWifiBssid
        )

        if (wifiCheck.isHomeWifiConnected) {
            val reason = "Interrompu : Wi-Fi domicile détecté (${wifiCheck.reason})"
            Log.i(TAG, reason)
            logRepo.addLog(eventType, false, reason)
            return
        }

        // 2. Verrou anti-spam : Cooldown
        val now = System.currentTimeMillis()
        val elapsedMs = now - settings.lastCallTimestampMs
        val cooldownMs = settings.cooldownMinutes * 60 * 1000L

        if (elapsedMs < cooldownMs) {
            val remainingMin = ((cooldownMs - elapsedMs) / 60000L) + 1
            val reason = "Interrompu : Cooldown actif (reste $remainingMin min)"
            Log.i(TAG, reason)
            logRepo.addLog(eventType, false, reason)
            return
        }

        // 3. Déclenchement de l'appel (ou simulation dry-run)
        val callResult = CallTriggerHelper.executeGateCall(
            context = context,
            phoneNumber = settings.targetPhoneNumber,
            isDryRun = settings.isDryRunMode
        )

        if (callResult.isSuccess) {
            prefs.updateLastCallTimestamp(now)
            val message = callResult.getOrNull() ?: "Appel validé"
            logRepo.addLog(eventType, true, message)
            Log.i(TAG, "Succès déclenchement : $message")
        } else {
            val errorMsg = callResult.exceptionOrNull()?.message ?: "Erreur inconnue"
            logRepo.addLog(eventType, false, "Échec d'appel : $errorMsg")
            Log.e(TAG, "Échec appel : $errorMsg")
        }
    }
}

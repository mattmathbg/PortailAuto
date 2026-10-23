package com.crous.gateopener.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.crous.gateopener.data.AppPreferences
import com.crous.gateopener.data.TriggerLogRepository
import com.crous.gateopener.ui.screens.MainScreen
import com.crous.gateopener.ui.theme.PortailGateOpenerTheme

class MainActivity : ComponentActivity() {

    private val appPreferences by lazy { AppPreferences(applicationContext) }
    private val logRepository by lazy { TriggerLogRepository(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(appPreferences, logRepository) as T
            }
        }
    }

    // 1. Permission Localisation Premier Plan
    private val fineLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshState(this)
    }

    // 2. Permission Localisation Arrière-Plan ("Toujours")
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshState(this)
    }

    // 3. Permission Téléphonie directe
    private val callPhoneLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshState(this)
    }

    // 4. Notifications sous Android 13+
    private val notificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshState(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PortailGateOpenerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestFineLocation = { requestFineLocation() },
                    onRequestBackgroundLocation = { requestBackgroundLocation() },
                    onRequestCallPhone = { requestCallPhone() },
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestIgnoreBattery = { requestIgnoreBatteryOptimization() },
                    onRequestNotifications = { requestNotifications() },
                    onOpenOemSettings = { openOemBatterySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState(this)
    }

    private fun requestFineLocation() {
        fineLocationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            Toast.makeText(this, "Non requis sur cette version d'Android", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestCallPhone() {
        callPhoneLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            }
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOemBatterySettings() {
        val oemIntents = listOf(
            // Xiaomi / MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
            // Samsung / OneUI
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            // Huawei / Honor
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            // Oppo / Realme
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            // OnePlus
            Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
        )

        for (intent in oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            } catch (_: Exception) {
                // Essayer l'intent constructeur suivant
            }
        }

        // Repli universel : Paramètres généraux de l'application
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(appDetailsIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir les réglages constructeur", Toast.LENGTH_SHORT).show()
        }
    }
}

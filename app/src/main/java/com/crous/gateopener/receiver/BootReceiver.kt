package com.crous.gateopener.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crous.gateopener.data.AppPreferences
import com.crous.gateopener.data.TriggerLogRepository
import com.crous.gateopener.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GateBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        Log.i(TAG, "Événement de démarrage détecté : $action. Vérification de la restauration du Geofence.")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = AppPreferences(context)
                val settings = prefs.getSettings()
                val logRepo = TriggerLogRepository(context)

                if (settings.isGeofenceActive) {
                    val result = GeofenceManager.registerGeofence(
                        context = context,
                        latitude = settings.geofenceLat,
                        longitude = settings.geofenceLng,
                        radiusMeters = settings.geofenceRadius,
                        responsivenessMs = settings.responsivenessMs
                    )

                    if (result.isSuccess) {
                        Log.i(TAG, "Geofence ré-enregistré avec succès post-reboot.")
                        logRepo.addLog("BOOT_RESTORE", true, "Geofence ré-enregistré avec succès post-reboot")
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                        Log.e(TAG, "Échec de ré-enregistrement post-reboot : $err")
                        logRepo.addLog("BOOT_RESTORE", false, "Échec ré-enregistrement post-reboot : $err")
                    }
                } else {
                    Log.d(TAG, "Geofence inactif au moment du reboot, aucune action requise.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement post-reboot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

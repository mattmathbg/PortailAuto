package com.crous.gateopener.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

object GeofenceManager {

    const val GEOFENCE_ID = "CROUS_GATE_GEOFENCE_ID"
    const val ACTION_GEOFENCE_EVENT = "com.crous.gateopener.ACTION_GEOFENCE_EVENT"
    private const val GEOFENCE_REQUEST_CODE = 4242

    fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, GEOFENCE_REQUEST_CODE, intent, flags)
    }

    suspend fun registerGeofence(
        context: Context,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        responsivenessMs: Int
    ): Result<Unit> {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasFine || !hasBackground) {
            return Result.failure(SecurityException("Permissions de localisation en arrière-plan manquantes"))
        }

        return try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(latitude, longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(responsivenessMs)
                .build()

            // initialTrigger = 0 : Aucun tir artificiel à l'enregistrement dans l'appartement !
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(geofence)
                .build()

            val pendingIntent = getPendingIntent(context)
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeGeofence(context: Context): Result<Unit> {
        return try {
            val geofencingClient = LocationServices.getGeofencingClient(context)
            val pendingIntent = getPendingIntent(context)
            geofencingClient.removeGeofences(pendingIntent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

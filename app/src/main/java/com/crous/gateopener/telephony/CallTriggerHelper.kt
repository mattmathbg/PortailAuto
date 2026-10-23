package com.crous.gateopener.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.crous.gateopener.GateOpenerApp
import com.crous.gateopener.R

object CallTriggerHelper {

    fun executeGateCall(
        context: Context,
        phoneNumber: String,
        isDryRun: Boolean
    ): Result<String> {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        if (cleanNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Numéro de téléphone du portail non renseigné"))
        }

        if (isDryRun) {
            sendNotification(
                context = context,
                title = "🧪 Portail CROUS — Test Dry-Run",
                message = "Déclenchement simulé avec succès vers $cleanNumber (aucun appel réel émis)"
            )
            return Result.success("Appel simulé vers $cleanNumber (Mode Dry-Run actif)")
        }

        // Vérification de la permission CALL_PHONE
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            return Result.failure(SecurityException("Permission CALL_PHONE non accordée"))
        }

        return try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)

            sendNotification(
                context = context,
                title = "🚪 Ouverture Portail en cours",
                message = "Appel transparent émis vers $cleanNumber"
            )
            Result.success("Appel lancé vers $cleanNumber")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) return
        }

        try {
            val notification = NotificationCompat.Builder(context, GateOpenerApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(GateOpenerApp.NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // Ignorer si les notifications sont coupées
        }
    }
}

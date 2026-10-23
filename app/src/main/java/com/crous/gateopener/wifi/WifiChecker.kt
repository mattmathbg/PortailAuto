package com.crous.gateopener.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

data class WifiCheckResult(
    val isHomeWifiConnected: Boolean,
    val reason: String,
    val currentSsid: String?,
    val currentBssid: String?
)

object WifiChecker {

    /**
     * Récupère les informations détaillées du réseau Wi-Fi actuellement connecté.
     */
    fun getCurrentWifiInfo(context: Context): Pair<String?, String?> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return Pair(null, null)

        val activeNetwork = connectivityManager.activeNetwork ?: return Pair(null, null)
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return Pair(null, null)

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return Pair(null, null)
        }

        var ssid: String? = null
        var bssid: String? = null

        // Extraction via TransportInfo sous Android 10+ (API 29+) si disponible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val transportInfo = capabilities.transportInfo
            if (transportInfo is WifiInfo) {
                ssid = cleanSsid(transportInfo.ssid)
                bssid = transportInfo.bssid
            }
        }

        // Repli via WifiManager si non renseigné
        if (ssid.isNullOrBlank() || ssid == "<unknown ssid>" || bssid.isNullOrBlank()) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            if (connectionInfo != null) {
                if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") {
                    ssid = cleanSsid(connectionInfo.ssid)
                }
                if (bssid.isNullOrBlank()) {
                    bssid = connectionInfo.bssid
                }
            }
        }

        if (ssid == "<unknown ssid>") ssid = null
        if (bssid == "02:00:00:00:00:00") bssid = null

        return Pair(ssid, bssid)
    }

    /**
     * Vérifie si le Wi-Fi du logement est actuellement connecté pour bloquer les faux positifs.
     */
    fun checkHomeWifiFailSafe(
        context: Context,
        targetSsid: String,
        targetBssid: String
    ): WifiCheckResult {
        val (currentSsid, currentBssid) = getCurrentWifiInfo(context)

        // Si l'appareil n'est même pas connecté au Wi-Fi, le verrou ne s'applique pas
        if (currentSsid == null && currentBssid == null) {
            return WifiCheckResult(
                isHomeWifiConnected = false,
                reason = "Appareil non connecté au Wi-Fi (Données mobiles actives)",
                currentSsid = null,
                currentBssid = null
            )
        }

        val hasTargetSsid = targetSsid.isNotBlank()
        val hasTargetBssid = targetBssid.isNotBlank()

        // Si aucun Wi-Fi cible n'est configuré par l'utilisateur
        if (!hasTargetSsid && !hasTargetBssid) {
            return WifiCheckResult(
                isHomeWifiConnected = false,
                reason = "Aucun Wi-Fi domicile configuré dans les réglages",
                currentSsid = currentSsid,
                currentBssid = currentBssid
            )
        }

        // Vérification prioritaire et ultra-stricte par BSSID (adresse MAC)
        if (hasTargetBssid && currentBssid != null) {
            if (currentBssid.equals(targetBssid.trim(), ignoreCase = true)) {
                return WifiCheckResult(
                    isHomeWifiConnected = true,
                    reason = "Verrou activé : BSSID domicile détecté ($currentBssid)",
                    currentSsid = currentSsid,
                    currentBssid = currentBssid
                )
            }
        }

        // Vérification par SSID (nom de réseau)
        if (hasTargetSsid && currentSsid != null) {
            if (currentSsid.equals(targetSsid.trim(), ignoreCase = true)) {
                return WifiCheckResult(
                    isHomeWifiConnected = true,
                    reason = "Verrou activé : SSID domicile détecté ($currentSsid)",
                    currentSsid = currentSsid,
                    currentBssid = currentBssid
                )
            }
        }

        return WifiCheckResult(
            isHomeWifiConnected = false,
            reason = "Connecté à un Wi-Fi tiers ($currentSsid / $currentBssid)",
            currentSsid = currentSsid,
            currentBssid = currentBssid
        )
    }

    private fun cleanSsid(rawSsid: String?): String? {
        if (rawSsid == null) return null
        var s = rawSsid.trim()
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1)
        }
        return s
    }
}

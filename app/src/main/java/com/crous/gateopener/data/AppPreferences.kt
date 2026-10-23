package com.crous.gateopener.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gate_opener_settings")

data class GateSettings(
    val targetPhoneNumber: String = "",
    val geofenceLat: Double = 48.8566,
    val geofenceLng: Double = 2.3522,
    val geofenceRadius: Float = 90.0f,
    val responsivenessMs: Int = 3000,
    val cooldownMinutes: Int = 15,
    val homeWifiSsid: String = "",
    val homeWifiBssid: String = "",
    val isDryRunMode: Boolean = false,
    val isGeofenceActive: Boolean = false,
    val lastCallTimestampMs: Long = 0L
)

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_PHONE_NUMBER = stringPreferencesKey("target_phone_number")
        private val KEY_GEOFENCE_LAT = doublePreferencesKey("geofence_lat")
        private val KEY_GEOFENCE_LNG = doublePreferencesKey("geofence_lng")
        private val KEY_GEOFENCE_RADIUS = floatPreferencesKey("geofence_radius")
        private val KEY_RESPONSIVENESS_MS = intPreferencesKey("responsiveness_ms")
        private val KEY_COOLDOWN_MINUTES = intPreferencesKey("cooldown_minutes")
        private val KEY_HOME_WIFI_SSID = stringPreferencesKey("home_wifi_ssid")
        private val KEY_HOME_WIFI_BSSID = stringPreferencesKey("home_wifi_bssid")
        private val KEY_DRY_RUN_MODE = booleanPreferencesKey("dry_run_mode")
        private val KEY_GEOFENCE_ACTIVE = booleanPreferencesKey("geofence_active")
        private val KEY_LAST_CALL_TIMESTAMP = longPreferencesKey("last_call_timestamp")
    }

    val settingsFlow: Flow<GateSettings> = context.dataStore.data.map { prefs ->
        GateSettings(
            targetPhoneNumber = prefs[KEY_PHONE_NUMBER] ?: "",
            geofenceLat = prefs[KEY_GEOFENCE_LAT] ?: 48.8566,
            geofenceLng = prefs[KEY_GEOFENCE_LNG] ?: 2.3522,
            geofenceRadius = prefs[KEY_GEOFENCE_RADIUS] ?: 90.0f,
            responsivenessMs = prefs[KEY_RESPONSIVENESS_MS] ?: 3000,
            cooldownMinutes = prefs[KEY_COOLDOWN_MINUTES] ?: 15,
            homeWifiSsid = prefs[KEY_HOME_WIFI_SSID] ?: "",
            homeWifiBssid = prefs[KEY_HOME_WIFI_BSSID] ?: "",
            isDryRunMode = prefs[KEY_DRY_RUN_MODE] ?: false,
            isGeofenceActive = prefs[KEY_GEOFENCE_ACTIVE] ?: false,
            lastCallTimestampMs = prefs[KEY_LAST_CALL_TIMESTAMP] ?: 0L
        )
    }

    suspend fun getSettings(): GateSettings = settingsFlow.first()

    suspend fun updatePhoneNumber(number: String) {
        context.dataStore.edit { it[KEY_PHONE_NUMBER] = number }
    }

    suspend fun updateGeofenceCoordinates(lat: Double, lng: Double, radius: Float, responsivenessMs: Int) {
        context.dataStore.edit {
            it[KEY_GEOFENCE_LAT] = lat
            it[KEY_GEOFENCE_LNG] = lng
            it[KEY_GEOFENCE_RADIUS] = radius
            it[KEY_RESPONSIVENESS_MS] = responsivenessMs
        }
    }

    suspend fun updateCooldownMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_COOLDOWN_MINUTES] = minutes }
    }

    suspend fun updateHomeWifi(ssid: String, bssid: String) {
        context.dataStore.edit {
            it[KEY_HOME_WIFI_SSID] = ssid
            it[KEY_HOME_WIFI_BSSID] = bssid
        }
    }

    suspend fun setDryRunMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DRY_RUN_MODE] = enabled }
    }

    suspend fun setGeofenceActive(active: Boolean) {
        context.dataStore.edit { it[KEY_GEOFENCE_ACTIVE] = active }
    }

    suspend fun updateLastCallTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[KEY_LAST_CALL_TIMESTAMP] = timestamp }
    }
}

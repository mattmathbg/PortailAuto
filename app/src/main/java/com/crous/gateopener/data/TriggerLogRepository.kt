package com.crous.gateopener.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.logsDataStore: DataStore<Preferences> by preferencesDataStore(name = "gate_opener_logs")

data class TriggerLog(
    val id: Long,
    val timestamp: Long,
    val type: String, // "GEOFENCE_ENTER", "SIMULATION", "BOOT_RESTORE", "CALL_DIRECT"
    val success: Boolean,
    val reason: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(Date(timestamp))
}

class TriggerLogRepository(private val context: Context) {

    companion object {
        private val KEY_RAW_LOGS = stringPreferencesKey("raw_logs_v1")
        private const val MAX_LOGS = 30
        private const val DELIMITER = "||"
        private const val ROW_DELIMITER = "##"
    }

    val logsFlow: Flow<List<TriggerLog>> = context.logsDataStore.data.map { prefs ->
        val raw = prefs[KEY_RAW_LOGS] ?: ""
        parseLogs(raw)
    }

    suspend fun addLog(type: String, success: Boolean, reason: String) {
        val entry = TriggerLog(
            id = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            type = type,
            success = success,
            reason = reason
        )
        context.logsDataStore.edit { prefs ->
            val existing = parseLogs(prefs[KEY_RAW_LOGS] ?: "")
            val updated = (listOf(entry) + existing).take(MAX_LOGS)
            prefs[KEY_RAW_LOGS] = serializeLogs(updated)
        }
    }

    suspend fun clearLogs() {
        context.logsDataStore.edit { it.remove(KEY_RAW_LOGS) }
    }

    private fun serializeLogs(list: List<TriggerLog>): String {
        return list.joinToString(ROW_DELIMITER) {
            "${it.id}$DELIMITER${it.timestamp}$DELIMITER${it.type}$DELIMITER${it.success}$DELIMITER${it.reason}"
        }
    }

    private fun parseLogs(raw: String): List<TriggerLog> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ROW_DELIMITER).mapNotNull { row ->
            val parts = row.split(DELIMITER)
            if (parts.size >= 5) {
                TriggerLog(
                    id = parts[0].toLongOrNull() ?: 0L,
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    type = parts[2],
                    success = parts[3].toBooleanStrictOrNull() ?: false,
                    reason = parts[4]
                )
            } else null
        }
    }
}

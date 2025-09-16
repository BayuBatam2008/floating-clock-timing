package com.floatingclock.timing.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatingclock.timing.data.model.FloatingClockStyle
import com.floatingclock.timing.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DATA_STORE_NAME = "floating_clock_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SELECTED_SERVER = stringPreferencesKey("selected_server")
        val CUSTOM_SERVERS = stringSetPreferencesKey("custom_servers")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val STYLE_JSON = stringPreferencesKey("style_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            val selectedServer = preferences[Keys.SELECTED_SERVER] ?: DEFAULT_SERVERS.first()
            val customServers = preferences[Keys.CUSTOM_SERVERS]?.toList()?.sorted().orEmpty()
            val autoSyncEnabled = preferences[Keys.AUTO_SYNC_ENABLED] ?: true
            val syncIntervalMinutes = preferences[Keys.SYNC_INTERVAL_MINUTES] ?: 15
            val style = preferences[Keys.STYLE_JSON]?.let {
                runCatching { json.decodeFromString<FloatingClockStyle>(it) }.getOrNull()
            } ?: FloatingClockStyle()
            UserPreferences(
                selectedServer = selectedServer,
                customServers = customServers,
                autoSyncEnabled = autoSyncEnabled,
                syncIntervalMinutes = syncIntervalMinutes,
                floatingClockStyle = style
            )
        }
        .distinctUntilChanged()

    suspend fun updateSelectedServer(server: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_SERVER] = server
        }
    }

    suspend fun addCustomServer(server: String) {
        val normalized = server.trim()
        if (normalized.isEmpty()) return
        context.dataStore.edit { prefs ->
            val updated = prefs[Keys.CUSTOM_SERVERS]?.toMutableSet() ?: mutableSetOf()
            updated.add(normalized)
            prefs[Keys.CUSTOM_SERVERS] = updated
        }
    }

    suspend fun removeCustomServer(server: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_SERVERS]?.toMutableSet() ?: return@edit
            current.remove(server)
            prefs[Keys.CUSTOM_SERVERS] = current
        }
    }

    suspend fun updateAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun updateSyncIntervalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SYNC_INTERVAL_MINUTES] = minutes.coerceIn(1, 180)
        }
    }

    suspend fun updateStyle(style: FloatingClockStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STYLE_JSON] = json.encodeToString(style)
        }
    }

    fun availableServers(customServers: List<String>): List<String> {
        return (DEFAULT_SERVERS + customServers).distinct()
    }
}

val DEFAULT_SERVERS = listOf(
    "time.google.com",
    "pool.ntp.org",
    "time.cloudflare.com",
    "time.windows.com",
    "time.apple.com"
)

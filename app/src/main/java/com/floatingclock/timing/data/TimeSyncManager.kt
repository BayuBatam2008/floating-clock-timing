package com.floatingclock.timing.data

import android.os.SystemClock
import com.floatingclock.timing.data.model.UserPreferences
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TimeSyncManager(
    private val preferencesRepository: PreferencesRepository,
    private val ntpClient: NtpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(TimeSyncState())
    val state: StateFlow<TimeSyncState> = _state.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collectLatest { preferences ->
                applyPreferences(preferences)
            }
        }
    }

    private suspend fun applyPreferences(preferences: UserPreferences) {
        val availableServers = preferencesRepository.availableServers(preferences.customServers)
        val selectedServer = if (availableServers.contains(preferences.selectedServer)) {
            preferences.selectedServer
        } else {
            availableServers.first()
        }

        if (selectedServer != preferences.selectedServer) {
            preferencesRepository.updateSelectedServer(selectedServer)
        }

        _state.value = _state.value.copy(
            selectedServer = selectedServer,
            availableServers = availableServers,
            autoSyncEnabled = preferences.autoSyncEnabled,
            syncIntervalMinutes = preferences.syncIntervalMinutes
        )

        if (!_state.value.isInitialized) {
            syncNow()
        }

        restartAutoSync()
    }

    fun currentTimeMillis(): Long {
        val currentState = _state.value
        val elapsedDelta = SystemClock.elapsedRealtime() - currentState.baseElapsedRealtimeMillis
        return currentState.baseNetworkTimeMillis + elapsedDelta
    }

    fun currentInstant(): Instant = Instant.ofEpochMilli(currentTimeMillis())

    suspend fun syncNow(serverOverride: String? = null): Result<Unit> {
        return mutex.withLock {
            val server = serverOverride ?: _state.value.selectedServer
            _state.value = _state.value.copy(isSyncing = true, errorMessage = null)
            val result = ntpClient.requestTime(server)
            result.onSuccess { ntpResult ->
                val elapsed = SystemClock.elapsedRealtime()
                val networkNow = ntpResult.ntpTimeMillis
                val offset = networkNow - System.currentTimeMillis()
                val lastSync = Instant.ofEpochMilli(networkNow)
                _state.value = _state.value.copy(
                    isInitialized = true,
                    isSyncing = false,
                    lastSyncInstant = lastSync,
                    offsetMillis = offset,
                    roundTripMillis = ntpResult.roundTripMillis,
                    baseNetworkTimeMillis = networkNow,
                    baseElapsedRealtimeMillis = elapsed,
                    lastSuccessfulServer = server,
                    errorMessage = null
                ).withNextSync()
            }.onFailure { throwable ->
                _state.value = _state.value.copy(
                    isSyncing = false,
                    errorMessage = throwable.message
                )
            }
            if (result.isSuccess) {
                restartAutoSync()
            }
            result.map { }
        }
    }

    private fun TimeSyncState.withNextSync(): TimeSyncState {
        return if (autoSyncEnabled) {
            val intervalMillis = TimeUnit.MINUTES.toMillis(syncIntervalMinutes.toLong())
            copy(nextSyncAtMillis = lastSyncInstant?.toEpochMilli()?.plus(intervalMillis))
        } else {
            copy(nextSyncAtMillis = null)
        }
    }

    private fun restartAutoSync() {
        autoSyncJob?.cancel()
        val current = _state.value
        if (!current.autoSyncEnabled) {
            _state.value = current.copy(nextSyncAtMillis = null)
            return
        }
        val intervalMillis = TimeUnit.MINUTES.toMillis(current.syncIntervalMinutes.toLong())
        _state.value = current.withNextSync()
        autoSyncJob = scope.launch(dispatcher) {
            while (true) {
                val delayMillis = intervalMillis.coerceAtLeast(60_000L)
                delay(delayMillis)
                if (!_state.value.autoSyncEnabled) {
                    continue
                }
                syncNow()
            }
        }
    }
}

data class TimeSyncState(
    val isInitialized: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncInstant: Instant? = null,
    val offsetMillis: Long = 0L,
    val roundTripMillis: Long = 0L,
    val selectedServer: String = DEFAULT_SERVERS.first(),
    val availableServers: List<String> = DEFAULT_SERVERS,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val errorMessage: String? = null,
    val baseNetworkTimeMillis: Long = System.currentTimeMillis(),
    val baseElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    val lastSuccessfulServer: String? = null,
    val nextSyncAtMillis: Long? = null
)

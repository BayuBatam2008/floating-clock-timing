package com.floatingclock.timing.overlay

import android.content.Context
import android.content.Intent
import com.floatingclock.timing.data.PreferencesRepository
import com.floatingclock.timing.data.TimeSyncManager
import com.floatingclock.timing.data.model.FloatingClockStyle
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class FloatingClockController(
    private val appContext: Context,
    private val timeSyncManager: TimeSyncManager,
    preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val overlayActive = AtomicBoolean(false)

    private val _overlayState = MutableStateFlow(FloatingOverlayUiState())
    val overlayState: StateFlow<FloatingOverlayUiState> = _overlayState.asStateFlow()

    private val eventState = MutableStateFlow<EventState?>(null)

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collect { preferences ->
                _overlayState.update { current ->
                    current.copy(style = preferences.floatingClockStyle)
                }
            }
        }
        scope.launch {
            while (isActive) {
                val now = timeSyncManager.currentTimeMillis()
                var showProgress = false
                var progress = 0f
                var pulsingStartedAt: Long? = null
                var eventTime: Long? = null

                val event = eventState.value
                if (event != null) {
                    eventTime = event.eventTimeMillis
                    val triggeredAt = event.triggeredAtMillis
                    if (triggeredAt != null) {
                        pulsingStartedAt = triggeredAt
                        if (now - triggeredAt >= PULSE_DURATION_MILLIS) {
                            eventState.value = null
                            pulsingStartedAt = null
                            eventTime = null
                        }
                    } else {
                        val millisUntil = event.eventTimeMillis - now
                        if (millisUntil <= PROGRESS_WINDOW_MILLIS) {
                            showProgress = millisUntil > 0
                            progress = (1f - millisUntil / PROGRESS_WINDOW_MILLIS.toFloat()).coerceIn(0f, 1f)
                            if (millisUntil <= 0L) {
                                val triggerMoment = event.eventTimeMillis
                                eventState.value = event.copy(triggeredAtMillis = triggerMoment)
                                pulsingStartedAt = triggerMoment
                            }
                        }
                    }
                }

                val resolvedEvent = eventState.value
                if (resolvedEvent != null) {
                    eventTime = resolvedEvent.eventTimeMillis
                    pulsingStartedAt = resolvedEvent.triggeredAtMillis
                    if (pulsingStartedAt == null) {
                        val millisUntil = resolvedEvent.eventTimeMillis - now
                        if (millisUntil in 0..PROGRESS_WINDOW_MILLIS) {
                            showProgress = true
                            progress = (1f - millisUntil / PROGRESS_WINDOW_MILLIS.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }

                _overlayState.update { current ->
                    current.copy(
                        isVisible = overlayActive.get(),
                        currentTimeMillis = now,
                        eventTimeMillis = eventTime,
                        showProgressBar = showProgress,
                        progressFraction = progress,
                        pulsingStartedAtMillis = pulsingStartedAt
                    )
                }

                val delayMillis = if (overlayActive.get()) 16L else 200L
                delay(delayMillis)
            }
        }
    }

    fun showOverlay(context: Context = appContext) {
        if (overlayActive.compareAndSet(false, true)) {
            // Check if service is already running to prevent duplicates
            if (!FloatingClockService.isRunning()) {
                context.startService(Intent(context, FloatingClockService::class.java))
            }
        }
        _overlayState.update { it.copy(isVisible = true) }
    }

    fun hideOverlay(context: Context = appContext) {
        if (overlayActive.compareAndSet(true, false)) {
            context.stopService(Intent(context, FloatingClockService::class.java))
        }
        _overlayState.update {
            it.copy(isVisible = false, showProgressBar = false, progressFraction = 0f, pulsingStartedAtMillis = null)
        }
    }

    internal fun onServiceDestroyed() {
        overlayActive.set(false)
        _overlayState.update { it.copy(isVisible = false) }
    }

    fun scheduleEvent(epochMillis: Long) {
        val now = timeSyncManager.currentTimeMillis()
        val triggered = if (epochMillis <= now) epochMillis else null
        eventState.value = EventState(eventTimeMillis = epochMillis, triggeredAtMillis = triggered)
    }

    fun clearEvent() {
        eventState.value = null
        _overlayState.update {
            it.copy(eventTimeMillis = null, showProgressBar = false, progressFraction = 0f, pulsingStartedAtMillis = null)
        }
    }

    fun isOverlayActive(): Boolean = overlayActive.get()

    companion object {
        private const val PROGRESS_WINDOW_MILLIS = 5_000L
        private const val PULSE_DURATION_MILLIS = 10_000L
    }
}

data class FloatingOverlayUiState(
    val isVisible: Boolean = false,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val eventTimeMillis: Long? = null,
    val pulsingStartedAtMillis: Long? = null,
    val style: FloatingClockStyle = FloatingClockStyle(),
    val showProgressBar: Boolean = false,
    val progressFraction: Float = 0f
)

data class EventState(
    val eventTimeMillis: Long,
    val triggeredAtMillis: Long? = null
)

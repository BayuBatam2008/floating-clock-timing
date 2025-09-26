package com.floatingclock.timing.overlay

import android.content.Context
import android.content.Intent
import com.floatingclock.timing.data.PreferencesRepository
import com.floatingclock.timing.data.TimeSyncManager
import com.floatingclock.timing.data.EventRepository
import com.floatingclock.timing.data.model.FloatingClockStyle
import com.floatingclock.timing.data.model.Event
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
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
    private val eventRepository: EventRepository,
    preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val overlayActive = AtomicBoolean(false)

    private val _overlayState = MutableStateFlow(FloatingOverlayUiState())
    val overlayState: StateFlow<FloatingOverlayUiState> = _overlayState.asStateFlow()

    private val eventState = MutableStateFlow<EventState?>(null)
    private var currentActiveEventId: String? = null
    private var eventEndTimer: Job? = null

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
                
                // Check for events within next hour and schedule the closest one
                checkAndScheduleNextEvent(now)
                
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
                            // Don't delete here - let checkAndScheduleNextEvent handle cleanup after 5 minutes
                            // Just continue showing pulsing animation
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

    private fun checkAndScheduleNextEvent(now: Long) {
        val eventsWithinHour = eventRepository.getEventsWithinNextHour()
        val nextEvent = eventsWithinHour.firstOrNull()
        
        if (nextEvent != null) {
            val eventDateTime = nextEvent.getEventDateTime()
            if (eventDateTime != null) {
                // Check if this is a different event than currently active
                if (currentActiveEventId != nextEvent.id) {
                    // Cancel previous event timer if exists
                    eventEndTimer?.cancel()
                    
                    currentActiveEventId = nextEvent.id
                    val triggered = if (eventDateTime <= now) eventDateTime else null
                    eventState.value = EventState(eventTimeMillis = eventDateTime, triggeredAtMillis = triggered)
                    
                    // Schedule automatic event switch after 10 seconds if event is triggered
                    if (triggered != null) {
                        eventEndTimer = scope.launch {
                            delay(10_000) // 10 seconds
                            // Switch to next available event or clear if none
                            scheduleNextEventAfterCurrent()
                        }
                    }
                } else {
                    // Check if current event needs to be auto-switched after 10 seconds of pulsing
                    val currentEventState = eventState.value
                    if (currentEventState?.triggeredAtMillis != null) {
                        val timeSinceTrigger = now - currentEventState.triggeredAtMillis!!
                        if (timeSinceTrigger >= 10_000L && eventEndTimer?.isActive != true) {
                            // Time to switch to next event
                            eventEndTimer = scope.launch {
                                scheduleNextEventAfterCurrent()
                            }
                        }
                    }
                }
                
                // Auto-delete event if it's 5 minutes past its time
                if (eventDateTime + (5 * 60 * 1000) <= now) {
                    scope.launch {
                        eventRepository.deleteEvent(nextEvent.id)
                        // Clear current state if this was the active event
                        if (currentActiveEventId == nextEvent.id) {
                            currentActiveEventId = null
                            eventState.value = null
                            eventEndTimer?.cancel()
                        }
                    }
                }
            }
        } else {
            // No events within next hour, clear current state
            if (currentActiveEventId != null) {
                eventEndTimer?.cancel()
                currentActiveEventId = null
                eventState.value = null
            }
        }
    }
    
    private fun scheduleNextEventAfterCurrent() {
        val eventsWithinHour = eventRepository.getEventsWithinNextHour()
        // Find next event that's not the current one
        val nextEvent = eventsWithinHour.find { it.id != currentActiveEventId }
        
        if (nextEvent != null) {
            val eventDateTime = nextEvent.getEventDateTime()
            if (eventDateTime != null) {
                currentActiveEventId = nextEvent.id
                val now = timeSyncManager.currentTimeMillis()
                val triggered = if (eventDateTime <= now) eventDateTime else null
                eventState.value = EventState(eventTimeMillis = eventDateTime, triggeredAtMillis = triggered)
                
                // Schedule timer for this event too
                if (triggered != null) {
                    eventEndTimer = scope.launch {
                        delay(10_000) // 10 seconds
                        scheduleNextEventAfterCurrent()
                    }
                }
            }
        } else {
            // No more events, clear state
            currentActiveEventId = null
            eventState.value = null
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

    fun clearEvent() {
        eventState.value = null
        currentActiveEventId = null
        eventEndTimer?.cancel()
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

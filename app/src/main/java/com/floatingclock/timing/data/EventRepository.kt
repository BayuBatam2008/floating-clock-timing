package com.floatingclock.timing.data

import android.content.Context
import android.content.SharedPreferences
import com.floatingclock.timing.data.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class EventRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: Flow<List<Event>> = _events.asStateFlow()
    
    init {
        loadEvents()
        startBackgroundCleanup()
    }
    
    private fun startBackgroundCleanup() {
        // Start background cleanup coroutine
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (true) {
                try {
                    cleanupExpiredEvents()
                    delay(60_000) // Check every minute
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(300_000) // Wait 5 minutes before retry if error
                }
            }
        }
    }
    
    private fun cleanupExpiredEvents() {
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = now - (5 * 60 * 1000) // 5 minutes ago
        
        val currentEvents = _events.value.toMutableList()
        val eventsToRemove = currentEvents.filter { event ->
            val eventDateTime = event.getEventDateTime()
            // Remove events that are more than 5 minutes past their target time
            eventDateTime != null && eventDateTime < fiveMinutesAgo
        }
        
        if (eventsToRemove.isNotEmpty()) {
            eventsToRemove.forEach { event ->
                currentEvents.removeAll { it.id == event.id }
            }
            _events.value = currentEvents.sortedWith(compareBy({ it.date }, { it.targetTime }))
            saveEvents()
        }
    }
    
    private fun loadEvents() {
        val eventsJson = sharedPreferences.getString("events_list", null)
        if (eventsJson != null) {
            try {
                val eventsList = json.decodeFromString<List<Event>>(eventsJson)
                _events.value = eventsList.sortedWith(compareBy({ it.date }, { it.targetTime }))
            } catch (e: Exception) {
                e.printStackTrace()
                _events.value = emptyList()
            }
        } else {
            // Create sample events for first time
            _events.value = createSampleEvents()
            saveEvents()
        }
    }
    
    private fun createSampleEvents(): List<Event> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        
        return listOf(
            Event(
                name = "Morning Meeting",
                targetTime = "09:00:00.000",
                date = today.toString(),
                description = "Weekly team standup"
            ),
            Event(
                name = "Lunch Break",
                targetTime = "12:30:00.000",
                date = today.toString(),
                description = "Time for lunch"
            ),
            Event(
                name = "Project Deadline",
                targetTime = "17:00:00.000",
                date = tomorrow.toString(),
                description = "Submit project deliverables"
            )
        )
    }
    
    private fun saveEvents() {
        try {
            val eventsJson = json.encodeToString(_events.value)
            sharedPreferences.edit()
                .putString("events_list", eventsJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hasConflictingEvent(targetTime: String, date: String, excludeId: String? = null): Boolean {
        return _events.value.any { existingEvent ->
            existingEvent.targetTime == targetTime && 
            existingEvent.date == date &&
            existingEvent.id != excludeId
        }
    }

    suspend fun addEvent(event: Event): Boolean = withContext(Dispatchers.IO) {
        // Check for duplicate target time on same date
        if (!hasConflictingEvent(event.targetTime, event.date, event.id)) {
            val currentEvents = _events.value.toMutableList()
            currentEvents.add(event)
            _events.value = currentEvents.sortedWith(compareBy({ it.date }, { it.targetTime }))
            saveEvents()
            true
        } else {
            false
        }
    }
    
    suspend fun updateEvent(event: Event) = withContext(Dispatchers.IO) {
        val currentEvents = _events.value.toMutableList()
        val index = currentEvents.indexOfFirst { it.id == event.id }
        if (index != -1) {
            currentEvents[index] = event
            _events.value = currentEvents.sortedWith(compareBy({ it.date }, { it.targetTime }))
            saveEvents()
        }
    }
    
    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        val currentEvents = _events.value.toMutableList()
        currentEvents.removeAll { it.id == eventId }
        _events.value = currentEvents
        saveEvents()
    }
    
    suspend fun toggleEventEnabled(eventId: String) = withContext(Dispatchers.IO) {
        val currentEvents = _events.value.toMutableList()
        val index = currentEvents.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val event = currentEvents[index]
            currentEvents[index] = event.copy(isEnabled = !event.isEnabled)
            _events.value = currentEvents
            saveEvents()
        }
    }
    
    fun getUpcomingEvents(): List<Event> {
        return _events.value.filter { it.isEnabled && it.isUpcoming() }
    }
    
    // New functions for target time functionality
    fun getEventsWithinNextHour(): List<Event> {
        val now = System.currentTimeMillis()
        val oneHourLater = now + (60 * 60 * 1000) // 1 hour in milliseconds
        
        return _events.value.filter { event ->
            if (!event.isEnabled) return@filter false
            
            val eventDateTime = event.getEventDateTime()
            eventDateTime != null && 
            eventDateTime >= now && 
            eventDateTime <= oneHourLater
        }.sortedBy { it.getEventDateTime() }
    }
    
    fun getActiveTargetEvent(): Event? {
        val eventsWithinHour = getEventsWithinNextHour()
        return eventsWithinHour.firstOrNull() // Get the closest event within next hour
    }
    
    suspend fun deleteCompletedEvents() = withContext(Dispatchers.IO) {
        val currentEvents = _events.value.toMutableList()
        val now = System.currentTimeMillis()
        
        // Remove events that are past their target time
        val completedEventIds = currentEvents
            .filter { event ->
                val eventDateTime = event.getEventDateTime()
                eventDateTime != null && eventDateTime < now
            }
            .map { it.id }
        
        if (completedEventIds.isNotEmpty()) {
            currentEvents.removeAll { it.id in completedEventIds }
            _events.value = currentEvents
            saveEvents()
        }
    }
    
    // Function deleteEventById removed as it was unused
}
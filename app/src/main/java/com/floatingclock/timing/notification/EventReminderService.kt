package com.floatingclock.timing.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import com.floatingclock.timing.data.EventRepository
import com.floatingclock.timing.data.model.Event
import com.floatingclock.timing.FloatingClockApplication
import java.util.Calendar
import kotlinx.coroutines.flow.firstOrNull

class EventReminderService : Service() {
    
    private lateinit var eventRepository: EventRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private lateinit var notificationManager: EventNotificationManager  
    private val notifiedEvents = mutableSetOf<String>() // Track which events we've notified for
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependencies
        val app = application as FloatingClockApplication
        eventRepository = app.eventRepository
        notificationManager = EventNotificationManager(this)
        
        Log.d("EventReminderService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            startEventMonitoring()
        }
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startEventMonitoring() {
        isMonitoring = true
        serviceScope.launch {
            while (isMonitoring) {
                try {
                    checkUpcomingEvents()
                    delay(30_000) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e("EventReminderService", "Error monitoring events", e)
                    delay(60_000) // Wait longer if there's an error
                }
            }
        }
    }
    
    private suspend fun checkUpcomingEvents() {
        try {
            val events = eventRepository.events.firstOrNull() ?: emptyList()
            val currentTime = System.currentTimeMillis()
            
            Log.d("EventReminderService", "Checking ${events.size} events at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(currentTime))}")
            
            events.filter { event ->
                event.isEnabled
            }.forEach { event ->
                val eventKey = "${event.name}_${event.targetTime}_${event.date}"
                
                Log.d("EventReminderService", "Processing event: ${event.name} on ${event.date} at ${event.targetTime}, enabled: ${event.isEnabled}")
                
                // Skip if we already notified for this event
                if (notifiedEvents.contains(eventKey)) {
                    Log.d("EventReminderService", "Already notified for: ${event.name}")
                    return@forEach
                }
                
                // Get event datetime in milliseconds
                val eventDateTime = event.getEventDateTime()
                if (eventDateTime == null) {
                    Log.w("EventReminderService", "Cannot get datetime for event: ${event.name}")
                    return@forEach
                }
                
                val timeDifference = eventDateTime - currentTime
                val minutesUntilEvent = timeDifference / (60 * 1000) // Convert to minutes
                
                Log.d("EventReminderService", "Event ${event.name}: ${minutesUntilEvent} minutes remaining")
                
                // Show notification if event is 5 minutes away (with tolerance 0-6 minutes)
                if (minutesUntilEvent <= 6 && minutesUntilEvent >= 0) {
                    // Convert event data to LocalDateTime
                    val eventLocalDateTime = try {
                        val eventDate = java.time.LocalDate.parse(event.date)
                        val eventTime = java.time.LocalTime.parse(event.targetTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                        eventDate.atTime(eventTime)
                    } catch (e: Exception) {
                        Log.e("EventReminderService", "Error parsing event datetime: ${event.name}", e)
                        return@forEach
                    }
                    
                    Log.i("EventReminderService", "Sending notification for: ${event.name} (${minutesUntilEvent} minutes remaining)")
                    
                    notificationManager.showEventReminder(
                        eventName = event.name,
                        targetTime = eventLocalDateTime
                    )
                    notifiedEvents.add(eventKey)
                }
                
                // Clean up old notifications for past events (older than 1 hour)
                if (timeDifference < -3600000) { // 1 hour ago
                    notifiedEvents.remove(eventKey)
                }
            }
        } catch (e: Exception) {
            Log.e("EventReminderService", "Error checking events", e)
        }
    }
    

    
    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceScope.cancel()
        Log.d("EventReminderService", "Service destroyed")
    }
}
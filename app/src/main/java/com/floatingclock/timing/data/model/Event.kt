package com.floatingclock.timing.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class Event(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Event",
    val targetTime: String = "09:00:00.000", // Format: HH:mm:ss.SSS
    val date: String = LocalDate.now().toString(), // Format: yyyy-MM-dd
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val description: String = ""
) {
    // Helper functions for display
    fun getFormattedTime(): String = targetTime
    
    fun getFormattedDate(): String {
        return try {
            val localDate = LocalDate.parse(date)
            localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } catch (e: Exception) {
            date
        }
    }
    
    fun getDisplayDate(): String {
        return try {
            val localDate = LocalDate.parse(date)
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            
            when (localDate) {
                today -> "Today"
                tomorrow -> "Tomorrow"
                else -> localDate.format(DateTimeFormatter.ofPattern("MMM dd"))
            }
        } catch (e: Exception) {
            date
        }
    }
    
    // Check if event is today and upcoming
    fun isUpcoming(): Boolean {
        return try {
            val eventDate = LocalDate.parse(date)
            val eventTime = LocalTime.parse(targetTime, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            val today = LocalDate.now()
            val now = LocalTime.now()
            
            when {
                eventDate.isAfter(today) -> true
                eventDate.isEqual(today) -> eventTime.isAfter(now)
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Get time remaining until event
    fun getTimeUntilEvent(): String {
        return try {
            val eventDate = LocalDate.parse(date)
            val eventTime = LocalTime.parse(targetTime, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            val today = LocalDate.now()
            val now = LocalTime.now()
            
            when {
                eventDate.isAfter(today) -> {
                    val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
                    when (daysUntil) {
                        1L -> "Tomorrow"
                        else -> "In $daysUntil days"
                    }
                }
                eventDate.isEqual(today) -> {
                    if (eventTime.isAfter(now)) {
                        val duration = java.time.Duration.between(now, eventTime)
                        val hours = duration.toHours()
                        val minutes = duration.toMinutes() % 60
                        when {
                            hours > 0 -> "${hours}h ${minutes}m"
                            minutes > 0 -> "${minutes}m"
                            else -> "Now"
                        }
                    } else {
                        "Past"
                    }
                }
                else -> "Past"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
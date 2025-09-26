package com.floatingclock.timing.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Centralized date and time formatters to avoid duplication across the app
 */
object DateTimeFormatters {
    
    // Time formatters
    private val timeFormatterSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val timeFormatterMinutes = DateTimeFormatter.ofPattern("HH:mm")
    private val timeFormatterMillis = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    
    // Date formatters
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
    private val syncDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
    private val fullSyncFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS")
    
    /**
     * Format time with different precision levels
     */
    fun formatTime(epochMillis: Long, showSeconds: Boolean = true, showMillis: Boolean = false): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(ZoneId.systemDefault())
        return when {
            showMillis -> timeFormatterMillis.format(zoned)
            showSeconds -> timeFormatterSeconds.format(zoned)
            else -> timeFormatterMinutes.format(zoned)
        }
    }
    
    /**
     * Format date only
     */
    fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(ZoneId.systemDefault())
        return dateFormatter.format(zoned)
    }
    
    /**
     * Format for sync display (without milliseconds)
     */
    fun formatSyncTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(ZoneId.systemDefault())
        return syncDateFormatter.format(zoned)
    }
    
    /**
     * Format for full sync display (with milliseconds)
     */
    fun formatFullSyncTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(ZoneId.systemDefault())
        return fullSyncFormatter.format(zoned)
    }
    
    /**
     * Format target time for events and overlays
     */
    fun formatTargetTime(epochMillis: Long): String {
        return formatTime(epochMillis, showSeconds = true, showMillis = true)
    }
}
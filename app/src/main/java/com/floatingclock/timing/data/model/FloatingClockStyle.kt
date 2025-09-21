package com.floatingclock.timing.data.model

import kotlinx.serialization.Serializable

enum class Line2DisplayMode {
    DATE_ONLY,
    TARGET_TIME_ONLY, 
    BOTH,
    NONE
}

@Serializable
data class FloatingClockStyle(
    val fontScale: Float = 1f,
    val showMillis: Boolean = true,
    val progressActivationSeconds: Int = 5,
    val pulsingSpeedMs: Int = 500,
    val line2DisplayMode: String = Line2DisplayMode.DATE_ONLY.name
) {
    fun getLine2DisplayMode(): Line2DisplayMode = try {
        Line2DisplayMode.valueOf(line2DisplayMode)
    } catch (e: Exception) {
        Line2DisplayMode.DATE_ONLY
    }
}

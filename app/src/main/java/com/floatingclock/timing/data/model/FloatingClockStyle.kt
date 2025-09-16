package com.floatingclock.timing.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class FloatingClockStyle(
    val fontScale: Float = 1f,
    val cornerRadiusDp: Float = 28f,
    val backgroundOpacity: Float = 0.92f,
    val useDynamicColor: Boolean = true,
    val accentColor: Long? = null,
    val showSeconds: Boolean = true,
    val showMillis: Boolean = true
) {
    fun accentColor(): Color? = accentColor?.let { Color(it) }
}

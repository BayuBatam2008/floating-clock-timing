package com.floatingclock.timing.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.floatingclock.timing.data.model.Line2DisplayMode

/**
 * Reusable UI components to reduce code duplication
 */
object UIComponents {
    
    /**
     * Standard text style for time display
     */
    @Composable
    fun TimeText(
        text: String,
        fontSize: TextUnit = 24.sp,
        color: Color = MaterialTheme.colorScheme.primary,
        fontScale: Float = 1f
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = fontSize * fontScale,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            textAlign = TextAlign.Start
        )
    }
    
    /**
     * Standard text style for secondary information (date, target time)
     */
    @Composable
    fun SecondaryText(
        text: String,
        fontSize: TextUnit = 14.sp,
        color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign: TextAlign = TextAlign.Start,
        fontScale: Float = 1f
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize * fontScale,
                fontWeight = FontWeight.Medium
            ),
            color = color,
            textAlign = textAlign
        )
    }
    
    /**
     * Standard row layout for displaying date and target time with proper spacing
     */
    @Composable
    fun DateTargetTimeRow(
        currentDate: String,
        targetTime: String?,
        dateColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        targetColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontScale: Float = 1f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryText(
                text = currentDate,
                color = dateColor,
                textAlign = TextAlign.Start,
                fontScale = fontScale
            )
            targetTime?.let { target ->
                SecondaryText(
                    text = target,
                    color = targetColor,
                    textAlign = TextAlign.End,
                    fontScale = fontScale
                )
            }
        }
    }
    
    /**
     * Render line 2 content based on display mode
     */
    @Composable
    fun Line2Content(
        displayMode: Line2DisplayMode,
        currentDate: String,
        targetTime: String?,
        dateColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        targetColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontScale: Float = 1f
    ) {
        when (displayMode) {
            Line2DisplayMode.DATE_ONLY -> {
                SecondaryText(
                    text = currentDate,
                    color = dateColor,
                    fontScale = fontScale
                )
            }
            Line2DisplayMode.TARGET_TIME_ONLY -> {
                targetTime?.let { target ->
                    SecondaryText(
                        text = target,
                        color = targetColor,
                        fontScale = fontScale
                    )
                }
            }
            Line2DisplayMode.BOTH -> {
                DateTargetTimeRow(
                    currentDate = currentDate,
                    targetTime = targetTime,
                    dateColor = dateColor,
                    targetColor = targetColor,
                    fontScale = fontScale
                )
            }
            Line2DisplayMode.NONE -> {
                // Show nothing
            }
        }
    }
}
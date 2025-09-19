package com.floatingclock.timing.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun FloatingOverlaySurface(
    state: FloatingOverlayUiState,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val style = state.style
    val shape = RoundedCornerShape(style.cornerRadiusDp.dp)
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = style.accentColor() ?: colorScheme.primary
    val pulseColor = colorScheme.tertiaryContainer
    val surfaceColor = colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = style.backgroundOpacity)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (state.pulsingStartedAtMillis != null) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFraction"
    )
    val backgroundColor = if (state.pulsingStartedAtMillis != null) {
        pulseColor.copy(alpha = 0.35f + (pulseFraction * 0.45f))
    } else {
        surfaceColor
    }

    // Add smooth millisecond updates like in live preview
    var smoothCurrentMillis by remember { mutableLongStateOf(state.currentTimeMillis) }
    
    LaunchedEffect(style.showMillis) {
        if (style.showMillis) {
            while (true) {
                smoothCurrentMillis = System.currentTimeMillis()
                delay(16L) // 60 FPS for smooth milliseconds
            }
        } else {
            smoothCurrentMillis = state.currentTimeMillis
        }
    }
    
    // Use smooth time for display when showing milliseconds
    val displayTimeMillis = if (style.showMillis) smoothCurrentMillis else state.currentTimeMillis

    val timeText = remember(displayTimeMillis, style.showSeconds, style.showMillis) {
        formatTime(displayTimeMillis, style.showSeconds, style.showMillis)
    }
    val dateText = remember(displayTimeMillis) {
        formatDate(displayTimeMillis)
    }
    val eventInfo = remember(displayTimeMillis, state.eventTimeMillis) {
        state.eventTimeMillis?.let { eventMillis ->
            val diff = eventMillis - displayTimeMillis
            if (diff > 0) {
                "Event in ${formatDuration(diff)}"
            } else {
                val elapsed = abs(diff)
                if (elapsed < 10_000L) {
                    "Event triggered"
                } else {
                    "Event completed"
                }
            }
        }
    }

    Surface(
        color = backgroundColor,
        shape = shape,
        tonalElevation = 12.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .widthIn(min = 180.dp, max = 250.dp) // More compact for PiP style
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), // Reduced padding for compactness
            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
        ) {
            // Compact header with drag handle and close button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp) // Fixed height for compact header
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp) // Smaller icon
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp) // Smaller close button
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { // Reduced spacing
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = run {
                            // More compact font size for PiP
                            val scaled = 28.sp * style.fontScale
                            if (scaled < 18.sp) 18.sp else scaled
                        },
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accentColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelMedium, // Smaller font
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            eventInfo?.let { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(
                visible = state.showProgressBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { state.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private val timeFormatterSeconds: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val timeFormatterMinutes: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val timeFormatterMillis: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

private fun formatTime(epochMillis: Long, showSeconds: Boolean, showMillis: Boolean): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zoned = instant.atZone(ZoneId.systemDefault())
    return when {
        showMillis -> timeFormatterMillis.format(zoned)
        showSeconds -> timeFormatterSeconds.format(zoned)
        else -> timeFormatterMinutes.format(zoned)
    }
}

private fun formatDate(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val zoned = instant.atZone(ZoneId.systemDefault())
    return dateFormatter.format(zoned)
}

private fun formatDuration(millis: Long): String {
    val duration = Duration.ofMillis(millis)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    val seconds = duration.minusHours(hours).minusMinutes(minutes).seconds
    val milliseconds = duration.minusHours(hours).minusMinutes(minutes).minusSeconds(seconds).toMillis()
    val locale = Locale.getDefault()
    return buildString {
        if (hours > 0) {
            append(hours)
            append("h ")
        }
        append(String.format(locale, "%02d:%02d", minutes, seconds))
        append('.')
        append(String.format(locale, "%03d", milliseconds))
    }
}

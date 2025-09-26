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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import com.floatingclock.timing.data.AppDependencies
import com.floatingclock.timing.data.model.Line2DisplayMode
import com.floatingclock.timing.data.PerformanceOptimizations
import com.floatingclock.timing.utils.DateTimeFormatters
import com.floatingclock.timing.utils.UIComponents

@Composable
fun FloatingOverlaySurface(
    state: FloatingOverlayUiState,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val style = state.style
    val colorScheme = MaterialTheme.colorScheme
    // Always use Material 3 dynamic colors
    val accentColor = colorScheme.primary
    val secondaryAccentColor = colorScheme.secondary
    val pulseColor = secondaryAccentColor // Use secondary color for pulsing
    val surfaceColor = colorScheme.surfaceColorAtElevation(6.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (state.pulsingStartedAtMillis != null) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = style.pulsingSpeedMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFraction"
    )
    val backgroundColor = if (state.pulsingStartedAtMillis != null) {
        pulseColor.copy(alpha = 0.35f + (pulseFraction * 0.45f))
    } else {
        surfaceColor.copy(alpha = 0.9f)
    }

    var smoothCurrentMillis by remember { mutableLongStateOf(state.currentTimeMillis) }
    
    // Performance optimizations for overlay
    val frameOptimizer = PerformanceOptimizations.rememberFrameRateOptimizer()
    val throttledUpdater = remember { PerformanceOptimizations.ThrottledUpdater(8L) }
    
    LaunchedEffect(style.showMillis) {
        if (style.showMillis) {
            while (true) {
                // Skip frame if performance optimizer says so
                if (frameOptimizer.shouldSkipFrame()) {
                    delay(1L)
                    continue
                }
                
                // Throttled updates for better performance
                if (!throttledUpdater.shouldUpdate()) {
                    delay(2L)
                    continue
                }
                
                val timeSyncManager = AppDependencies.timeSyncManager
                val timeState = timeSyncManager.state.value
                
                smoothCurrentMillis = if (timeState.isInitialized) {
                    timeSyncManager.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
                delay(8L) // Optimized 120 FPS for smoother milliseconds
            }
        } else {
            smoothCurrentMillis = state.currentTimeMillis
        }
    }
    
    val displayTimeMillis = if (style.showMillis) smoothCurrentMillis else state.currentTimeMillis

    // Use centralized formatters for consistency
    val timeText = remember(displayTimeMillis, style.showMillis) {
        DateTimeFormatters.formatTime(displayTimeMillis, showSeconds = true, showMillis = style.showMillis)
    }
    val currentDate = remember(displayTimeMillis) {
        DateTimeFormatters.formatDate(displayTimeMillis)
    }
    val targetTime = remember(state.eventTimeMillis) {
        state.eventTimeMillis?.let { eventMillis ->
            DateTimeFormatters.formatTargetTime(eventMillis)
        }
    }

    Surface(
        modifier = Modifier
            .widthIn(min = 280.dp, max = 350.dp)
            .heightIn(min = 120.dp, max = 150.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { 
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = (18.sp * style.fontScale),
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accentColor,
                    textAlign = TextAlign.Center
                )
                
                when (style.getLine2DisplayMode()) {
                    Line2DisplayMode.DATE_ONLY -> {
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = MaterialTheme.typography.labelSmall.fontSize * style.fontScale
                            ),
                            color = secondaryAccentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                    Line2DisplayMode.TARGET_TIME_ONLY -> {
                        targetTime?.let { target ->
                            Text(
                                text = target,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = secondaryAccentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Line2DisplayMode.BOTH -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Date (tanggal left aligned)
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = secondaryAccentColor,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // Right: Target Time (target time right aligned dengan space di tengah)
                            targetTime?.let { target ->
                                Text(
                                    text = target,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = secondaryAccentColor,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                    Line2DisplayMode.NONE -> {
                        // Show nothing for line 2
                    }
                }
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
                    color = secondaryAccentColor,
                    trackColor = secondaryAccentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

// Formatters removed - now using centralized DateTimeFormatters utility



package com.floatingclock.timing.ui.performance

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max
import kotlin.math.min

/**
 * Performance optimizations untuk aplikasi
 */

/**
 * Optimized time ticker yang hanya update ketika perlu
 */
@Composable
fun rememberOptimizedTimeTicker(intervalMs: Long = 1000L): Flow<Long> {
    return remember(intervalMs) {
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(intervalMs)
            }
        }
    }
}

/**
 * Stable Color yang tidak trigger recomposition
 */
@Stable
data class StableColor(val value: Color)

/**
 * Optimized mutable state untuk performance
 */
@Composable
fun <T> rememberStableState(initial: T): MutableState<T> {
    return remember { mutableStateOf(initial) }
}

/**
 * Debounced state untuk menghindari terlalu banyak update
 */
@Composable
fun <T> rememberDebouncedState(
    value: T,
    delayMs: Long = 300L
): State<T> {
    val debouncedValue = remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        delay(delayMs)
        debouncedValue.value = value
    }
    
    return debouncedValue
}

/**
 * Cached derived state untuk expensive calculations
 */
@Composable
fun <T, R> rememberCachedDerivedState(
    key: T,
    calculation: (T) -> R
): State<R> {
    return remember(key) {
        derivedStateOf { calculation(key) }
    }
}

/**
 * Stable animation values untuk smooth performance
 */
@Stable
data class AnimationProgress(
    val value: Float,
    val isActive: Boolean
)

/**
 * Optimized layout measurements cache
 */
@Stable
data class LayoutCache(
    val width: Dp,
    val height: Dp,
    val offset: Offset
)

/**
 * Memory-efficient image cache key
 */
@Stable
data class ImageCacheKey(
    val id: String,
    val size: Int
)

/**
 * Throttled update function untuk high-frequency events
 */
class ThrottledUpdater(private val intervalMs: Long) {
    private var lastUpdateTime = 0L
    
    fun shouldUpdate(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastUpdateTime >= intervalMs) {
            lastUpdateTime = currentTime
            true
        } else {
            false
        }
    }
}

/**
 * Performance monitoring tools
 */
object PerformanceMonitor {
    private val frameTimes = mutableListOf<Long>()
    private var lastFrameTime = 0L
    
    @Composable
    fun TrackFrameRate() {
        LaunchedEffect(Unit) {
            while (true) {
                val currentTime = System.currentTimeMillis()
                if (lastFrameTime > 0) {
                    val frameTime = currentTime - lastFrameTime
                    frameTimes.add(frameTime)
                    
                    // Keep only last 100 frames
                    if (frameTimes.size > 100) {
                        frameTimes.removeAt(0)
                    }
                }
                lastFrameTime = currentTime
                delay(16) // ~60 FPS
            }
        }
    }
    
    fun getAverageFrameTime(): Float {
        return if (frameTimes.isNotEmpty()) {
            frameTimes.average().toFloat()
        } else 0f
    }
    
    fun getFPS(): Float {
        val avgFrameTime = getAverageFrameTime()
        return if (avgFrameTime > 0) 1000f / avgFrameTime else 0f
    }
}

/**
 * Optimized font scaling
 */
@Composable
fun rememberOptimizedFontSize(
    baseSizeEm: TextUnit,
    scale: Float
): TextUnit {
    val density = LocalDensity.current
    
    return remember(baseSizeEm, scale, density) {
        val scaledSize = baseSizeEm.value * scale
        // Clamp to reasonable bounds untuk performance
        val clampedSize = max(8f, min(scaledSize, 72f))
        clampedSize.sp
    }
}

/**
 * Efficient layout positioning
 */
@Stable
data class OptimizedLayoutData(
    val contentWidth: Dp,
    val contentHeight: Dp,
    val position: Offset,
    val scale: Float = 1f
)

/**
 * Resource-efficient string formatting
 */
object StringOptimizer {
    private val formatCache = mutableMapOf<String, String>()
    
    fun formatTime(
        timeMillis: Long,
        pattern: String,
        useCache: Boolean = true
    ): String {
        val cacheKey = "$timeMillis-$pattern"
        
        if (useCache && formatCache.containsKey(cacheKey)) {
            return formatCache[cacheKey]!!
        }
        
        val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        val result = formatter.format(java.util.Date(timeMillis))
        
        if (useCache) {
            // Limit cache size
            if (formatCache.size > 50) {
                formatCache.clear()
            }
            formatCache[cacheKey] = result
        }
        
        return result
    }
    
    fun clearCache() {
        formatCache.clear()
    }
}
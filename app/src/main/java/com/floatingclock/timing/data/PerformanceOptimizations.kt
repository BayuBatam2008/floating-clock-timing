package com.floatingclock.timing.data

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * Performance optimization utilities for smooth UI operation
 */
object PerformanceOptimizations {
    
    /**
     * Throttled updater for reducing excessive updates
     */
    class ThrottledUpdater(private val intervalMs: Long = 16L) {
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
     * Debounced state for reducing rapid state changes
     */
    @Composable
    fun <T> rememberDebouncedState(
        value: T,
        delayMs: Long = 100L
    ): State<T> {
        val debouncedValue = remember { mutableStateOf(value) }
        
        LaunchedEffect(value) {
            delay(delayMs)
            debouncedValue.value = value
        }
        
        return debouncedValue
    }
    
    /**
     * Optimized string formatting with caching
     */
    class StringFormatter {
        private val cache = mutableMapOf<String, String>()
        private val maxCacheSize = 50
        
        fun formatTime(timeMillis: Long, pattern: String): String {
            val key = "$timeMillis-$pattern"
            return cache.getOrPut(key) {
                if (cache.size >= maxCacheSize) {
                    cache.clear()
                }
                java.time.format.DateTimeFormatter.ofPattern(pattern)
                    .format(java.time.Instant.ofEpochMilli(timeMillis)
                        .atZone(java.time.ZoneId.systemDefault()))
            }
        }
    }
    
    /**
     * Frame rate optimizer for smooth animations
     */
    @Composable
    fun rememberFrameRateOptimizer(): FrameRateOptimizer {
        return remember { FrameRateOptimizer() }
    }
    
    class FrameRateOptimizer {
        private var lastFrameTime = System.nanoTime()
        private val targetFrameTime = 16_666_667L // 60 FPS in nanoseconds
        
        fun shouldSkipFrame(): Boolean {
            val currentTime = System.nanoTime()
            val timeDelta = currentTime - lastFrameTime
            
            return if (timeDelta >= targetFrameTime) {
                lastFrameTime = currentTime
                false
            } else {
                true
            }
        }
        
        fun getFrameRate(): Float {
            val currentTime = System.nanoTime()
            val timeDelta = currentTime - lastFrameTime
            return if (timeDelta > 0) {
                1_000_000_000f / timeDelta
            } else {
                60f
            }
        }
    }
    
    /**
     * Memory efficient update checker
     */
    fun <T> hasSignificantChange(old: T, new: T, threshold: Float = 0.01f): Boolean {
        return when {
            old is Float && new is Float -> abs(old - new) > threshold
            old is Double && new is Double -> abs(old - new) > threshold
            old is Long && new is Long -> abs(old - new) > (threshold * 1000).toLong()
            else -> old != new
        }
    }
    
    /**
     * Composable for monitoring performance
     */
    @Composable
    fun PerformanceMonitor(
        tag: String = "Performance",
        onMetrics: (frameRate: Float, memoryMb: Float) -> Unit = { _, _ -> }
    ) {
        val frameOptimizer = rememberFrameRateOptimizer()
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000L)
                val frameRate = frameOptimizer.getFrameRate()
                val runtime = Runtime.getRuntime()
                val memoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
                onMetrics(frameRate, memoryMb)
            }
        }
    }
}
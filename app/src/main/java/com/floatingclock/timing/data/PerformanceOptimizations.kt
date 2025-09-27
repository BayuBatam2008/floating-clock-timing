package com.floatingclock.timing.data

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * Performance optimization utilities for smooth UI operation
 */
object PerformanceOptimizations {
    
    /**
     * Throttled updater for reducing excessive updates - optimized for main thread
     */
    class ThrottledUpdater(private val intervalMs: Long = 16L) {
        private var lastUpdateTime = 0L
        
        fun shouldUpdate(): Boolean {
            val currentTime = System.nanoTime() / 1_000_000L // Use nanoTime for better precision
            return if (currentTime - lastUpdateTime >= intervalMs) {
                lastUpdateTime = currentTime
                true
            } else {
                false
            }
        }
        
        // shouldUpdateAdaptive function removed as it was unused
    }
    
    // rememberDebouncedState function removed as it was unused
    
    // StringFormatter class removed as it was unused
    
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
    
    // hasSignificantChange function removed as it was unused
    
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
    
    /**
     * Main thread work optimizer - moves heavy operations off main thread
     */
    object MainThreadOptimizer {
        private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        fun <T> executeOffMainThread(
            operation: suspend () -> T,
            onResult: (T) -> Unit
        ) {
            backgroundScope.launch {
                try {
                    val result = operation()
                    launch(Dispatchers.Main) {
                        onResult(result)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainThreadOptimizer", "Background operation failed", e)
                }
            }
        }
        
        // cleanup function removed as it was unused
    }
}
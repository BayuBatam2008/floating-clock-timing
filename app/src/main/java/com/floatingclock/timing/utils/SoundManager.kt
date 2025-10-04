package com.floatingclock.timing.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages sound triggers for event countdowns.
 * Plays preparation beeps before event and a different sound at target time.
 */
class SoundManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    
    companion object {
        // Tone frequencies
        private const val PREP_TONE = ToneGenerator.TONE_PROP_BEEP        // Lower pitch for preparation
        private const val START_TONE = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD  // Higher pitch for start
        
        // Durations in milliseconds
        private const val PREP_BEEP_DURATION = 100
        private const val START_BEEP_DURATION = 150
        private const val START_BEEP_TOTAL_DURATION = 3000 // 3 seconds of start beeps
    }
    
    /**
     * Count mode options:
     * - 3: 2 seconds of prep beeps + 3 seconds of start beeps
     * - 5: 4 seconds of prep beeps + 3 seconds of start beeps
     * - 10: 9 seconds of prep beeps + 3 seconds of start beeps
     */
    enum class CountMode(val prepSeconds: Int) {
        THREE(2),
        FIVE(4),
        TEN(9)
    }
    
    init {
        try {
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                ToneGenerator.MAX_VOLUME
            )
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Failed to initialize ToneGenerator: ${e.message}")
        }
    }
    
    /**
     * Starts the countdown sound sequence.
     * @param millisecondsUntilTarget Time in milliseconds until the target event
     * @param countMode The count mode (3, 5, or 10)
     */
    fun startCountdown(millisecondsUntilTarget: Long, countMode: CountMode) {
        stopCountdown()
        
        val prepDurationMs = countMode.prepSeconds * 1000L
        
        // Only start if we have enough time for prep beeps
        if (millisecondsUntilTarget < prepDurationMs) {
            return
        }
        
        countdownJob = scope.launch {
            try {
                // Wait until prep time starts
                val delayUntilPrep = millisecondsUntilTarget - prepDurationMs
                if (delayUntilPrep > 0) {
                    delay(delayUntilPrep)
                }
                
                if (!isActive) return@launch
                
                // Play preparation beeps (every second)
                val prepBeeps = countMode.prepSeconds
                for (i in 0 until prepBeeps) {
                    if (!isActive) break
                    
                    playTone(PREP_TONE, PREP_BEEP_DURATION)
                    android.util.Log.d("SoundManager", "Prep beep ${i + 1}/$prepBeeps")
                    
                    if (i < prepBeeps - 1) { // Don't delay after last prep beep
                        delay(1000L - PREP_BEEP_DURATION)
                    }
                }
                
                if (!isActive) return@launch
                
                // Wait for remaining time to target (should be minimal)
                delay(100L)
                
                // Play start beeps continuously for 3 seconds
                val startTime = System.currentTimeMillis()
                android.util.Log.d("SoundManager", "Target time reached! Playing start beeps for 3 seconds")
                
                while (isActive && (System.currentTimeMillis() - startTime) < START_BEEP_TOTAL_DURATION) {
                    playTone(START_TONE, START_BEEP_DURATION)
                    delay(200L) // Short pause between beeps
                }
                
                android.util.Log.d("SoundManager", "Sound sequence completed")
                
            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "Error during countdown: ${e.message}")
            }
        }
    }
    
    /**
     * Stops the current countdown sequence.
     */
    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }
    
    /**
     * Plays a tone with the specified type and duration.
     */
    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Failed to play tone: ${e.message}")
        }
    }
    
    /**
     * Releases resources. Call this when done with the SoundManager.
     */
    fun release() {
        stopCountdown()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Error releasing ToneGenerator: ${e.message}")
        }
    }
}

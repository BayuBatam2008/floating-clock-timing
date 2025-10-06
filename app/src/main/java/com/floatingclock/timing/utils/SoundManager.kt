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
 * Plays countdown beeps (3-2-1, 5-4-3-2-1, or 10-9-8-7-6-5-4-3-2-1) before event 
 * and a trigger sound at exact target time.
 * 
 * Note: You may see "attributionTag not declared" warnings in logcat.
 * These are harmless system warnings and do not affect functionality.
 */
class SoundManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var countdownJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    companion object {
        // DTMF tones for countdown numbers (higher pitch = lower number)
        private val COUNTDOWN_TONES = mapOf(
            10 to ToneGenerator.TONE_DTMF_0,
            9 to ToneGenerator.TONE_DTMF_9,
            8 to ToneGenerator.TONE_DTMF_8,
            7 to ToneGenerator.TONE_DTMF_7,
            6 to ToneGenerator.TONE_DTMF_6,
            5 to ToneGenerator.TONE_DTMF_5,
            4 to ToneGenerator.TONE_DTMF_4,
            3 to ToneGenerator.TONE_DTMF_3,
            2 to ToneGenerator.TONE_DTMF_2,
            1 to ToneGenerator.TONE_DTMF_1
        )
        
        // Trigger sound (star key - distinctive)
        private const val TRIGGER_TONE = ToneGenerator.TONE_DTMF_S
        
        // Durations in milliseconds
        private const val COUNTDOWN_BEEP_DURATION = 150  // Short beep for each number
        private const val TRIGGER_BEEP_DURATION = 500    // Longer beep for trigger
        
        // Volume percentage (0-100)
        private const val VOLUME_PERCENT = 100
    }
    
    /**
     * Count mode options:
     * - 3: Countdown from 3, 2, 1, then TRIGGER at target time
     * - 5: Countdown from 5, 4, 3, 2, 1, then TRIGGER at target time
     * - 10: Countdown from 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, then TRIGGER at target time
     */
    enum class CountMode(val countFrom: Int) {
        THREE(3),
        FIVE(5),
        TEN(10)
    }
    
    init {
        try {
            // Use STREAM_ALARM which is more likely to be audible
            // Calculate volume as percentage of MAX_VOLUME
            val volume = (VOLUME_PERCENT * ToneGenerator.MAX_VOLUME) / 100
            
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_ALARM,
                volume
            )
            
            android.util.Log.i("SoundManager", "✅ ToneGenerator initialized successfully with volume: $volume")
            android.util.Log.i("SoundManager", "   Stream: STREAM_ALARM")
            android.util.Log.i("SoundManager", "   Current alarm volume: ${audioManager.getStreamVolume(AudioManager.STREAM_ALARM)}")
            android.util.Log.i("SoundManager", "   Max alarm volume: ${audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)}")
            
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "❌ Failed to initialize ToneGenerator: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Starts the countdown sound sequence.
     * Plays countdown beeps at exactly 1 second intervals, then trigger sound at target time.
     * 
     * @param millisecondsUntilTarget Time in milliseconds until the target event
     * @param countMode The count mode (3, 5, or 10)
     */
    fun startCountdown(millisecondsUntilTarget: Long, countMode: CountMode) {
        stopCountdown()
        
        android.util.Log.i("SoundManager", "🔊 Starting countdown sequence")
        android.util.Log.i("SoundManager", "   Time until target: ${millisecondsUntilTarget}ms")
        android.util.Log.i("SoundManager", "   Count mode: $countMode (countdown from ${countMode.countFrom})")
        android.util.Log.i("SoundManager", "   ToneGenerator: ${if (toneGenerator != null) "Ready" else "NULL!"}")
        
        if (toneGenerator == null) {
            android.util.Log.e("SoundManager", "❌ Cannot play sound - ToneGenerator is null")
            return
        }
        
        val countdownSeconds = countMode.countFrom
        val totalCountdownMs = countdownSeconds * 1000L
        
        // Add 200ms tolerance for processing delays
        val minimumRequiredTime = totalCountdownMs - 200
        
        // Only start if we have enough time for countdown
        if (millisecondsUntilTarget < minimumRequiredTime) {
            android.util.Log.w("SoundManager", "⚠️ Not enough time for countdown (need ${minimumRequiredTime}ms, have ${millisecondsUntilTarget}ms)")
            return
        }
        
        android.util.Log.i("SoundManager", "✅ Time check passed (have ${millisecondsUntilTarget}ms, need ${minimumRequiredTime}ms)")
        
        countdownJob = scope.launch {
            try {
                // Calculate exact start time for countdown
                val currentTime = System.currentTimeMillis()
                val targetTime = currentTime + millisecondsUntilTarget
                val countdownStartTime = targetTime - totalCountdownMs
                
                // Wait until countdown should start
                val delayUntilCountdown = countdownStartTime - System.currentTimeMillis()
                if (delayUntilCountdown > 0) {
                    android.util.Log.d("SoundManager", "⏳ Waiting ${delayUntilCountdown}ms until countdown starts")
                    delay(delayUntilCountdown)
                }
                
                if (!isActive) {
                    android.util.Log.w("SoundManager", "⚠️ Countdown cancelled before start")
                    return@launch
                }
                
                android.util.Log.i("SoundManager", "🎵 Starting countdown: ${countMode.countFrom} → 1")
                
                // Play countdown beeps (count down from N to 1)
                for (count in countMode.countFrom downTo 1) {
                    if (!isActive) break
                    
                    val beepTime = System.currentTimeMillis()
                    val expectedTime = targetTime - (count * 1000L)
                    val timeDrift = beepTime - expectedTime
                    
                    // Play the countdown tone
                    val tone = COUNTDOWN_TONES[count] ?: ToneGenerator.TONE_DTMF_1
                    playTone(tone, COUNTDOWN_BEEP_DURATION)
                    android.util.Log.d("SoundManager", "   🔔 Countdown: $count (drift: ${timeDrift}ms)")
                    
                    // Wait until next second mark (with drift correction)
                    if (count > 1) {
                        val nextBeepTime = targetTime - ((count - 1) * 1000L)
                        val waitTime = nextBeepTime - System.currentTimeMillis()
                        if (waitTime > 0) {
                            delay(waitTime)
                        }
                    }
                }
                
                if (!isActive) {
                    android.util.Log.w("SoundManager", "⚠️ Countdown cancelled before trigger")
                    return@launch
                }
                
                // Wait until exactly the target time for trigger sound
                val timeUntilTrigger = targetTime - System.currentTimeMillis()
                if (timeUntilTrigger > 0) {
                    delay(timeUntilTrigger)
                }
                
                // Play trigger sound at exact target time
                val triggerTime = System.currentTimeMillis()
                val triggerDrift = triggerTime - targetTime
                
                playTone(TRIGGER_TONE, TRIGGER_BEEP_DURATION)
                android.util.Log.i("SoundManager", "🚀 TRIGGER! (drift: ${triggerDrift}ms)")
                
                // Play trigger sound 3 times for emphasis
                delay(100)
                playTone(TRIGGER_TONE, TRIGGER_BEEP_DURATION)
                delay(100)
                playTone(TRIGGER_TONE, TRIGGER_BEEP_DURATION)
                
                android.util.Log.i("SoundManager", "✅ Sound sequence completed")
                
            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "❌ Error during countdown: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Stops the current countdown sequence.
     */
    fun stopCountdown() {
        if (countdownJob != null) {
            android.util.Log.d("SoundManager", "⏹️ Stopping countdown")
            countdownJob?.cancel()
            countdownJob = null
        }
    }
    
    /**
     * Plays a tone with the specified type and duration.
     */
    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            val result = toneGenerator?.startTone(toneType, durationMs)
            if (result == false) {
                android.util.Log.w("SoundManager", "⚠️ ToneGenerator.startTone() returned false")
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "❌ Failed to play tone: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Releases resources. Call this when done with the SoundManager.
     */
    fun release() {
        android.util.Log.d("SoundManager", "🔇 Releasing SoundManager resources")
        stopCountdown()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "❌ Error releasing ToneGenerator: ${e.message}")
            e.printStackTrace()
        }
    }
}

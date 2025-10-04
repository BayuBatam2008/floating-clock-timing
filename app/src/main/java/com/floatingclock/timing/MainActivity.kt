package com.floatingclock.timing

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.floatingclock.timing.ui.FloatingClockApp
import com.floatingclock.timing.ui.theme.FloatingClockTheme
import com.floatingclock.timing.notification.EventNotificationManager
import com.floatingclock.timing.data.PerformanceOptimizations
import com.floatingclock.timing.utils.DateTimeFormatters
import com.floatingclock.timing.utils.UIComponents
import com.floatingclock.timing.data.PerformanceOptimizations.MainThreadOptimizer


// Data class for progress indicator information
data class ProgressInfo(
    val progress: Float,
    val isActive: Boolean
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    
    private var isPiPModeState: MutableState<Boolean> = mutableStateOf(false)
    private lateinit var eventNotificationManager: EventNotificationManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ POST_NOTIFICATIONS permission granted!")
            android.util.Log.d("MainActivity", "🎯 5-minute reminder system ready!")
        } else {
            android.util.Log.w("MainActivity", "❌ POST_NOTIFICATIONS permission denied")
            android.util.Log.w("MainActivity", "⚠️ Reminders may not work properly")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        eventNotificationManager = EventNotificationManager(this)
        requestAllNotificationPermissions()
        setupEfficientEventNotifications()
        
        android.util.Log.d("MainActivity", "✅ Notification system ready for real events!")
        
        setContent {
            FloatingClockTheme {
                val context = LocalContext.current
                var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                val isInPiP by isPiPModeState
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> {
                                hasOverlayPermission = Settings.canDrawOverlays(context)
                                isPiPModeState.value = isInPictureInPictureMode
                                // Re-sync time when resuming from background to ensure accuracy
                                android.util.Log.d("MainActivity", "App resumed - triggering time sync")
                                viewModel.syncNow()
                            }
                            Lifecycle.Event.ON_START -> {
                                // Also sync when the app becomes visible
                                android.util.Log.d("MainActivity", "App started - checking time accuracy")
                                viewModel.syncNow()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (isInPiP) {
                    android.util.Log.d("MainActivity", "Showing PictureInPictureFloatingClock")
                    PictureInPictureFloatingClock(viewModel)
                } else {
                    android.util.Log.d("MainActivity", "Showing FloatingClockApp")
                    FloatingClockApp(
                        viewModel = viewModel,
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        },
                        onEnterPip = { enterPictureInPicture() }
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        android.util.Log.d("MainActivity", "PiP mode changed to: $isInPictureInPictureMode")
        isPiPModeState.value = isInPictureInPictureMode
    }

    private fun enterPictureInPicture() {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(23, 10))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                    setSourceRectHint(android.graphics.Rect(0, 0, 100, 100))
                }
            }
            .build()
        enterPictureInPictureMode(params)
    }
    
    private var lastScheduledEventTime: Long? = null
    
    /**
     * Sets up event notifications with 5-minute reminders.
     */
    private fun setupEfficientEventNotifications() {
        android.util.Log.d("EfficientNotif", "🎯 Setting up real event notification monitoring...")
        
        lifecycleScope.launch {
            viewModel.overlayState.collect { overlayState ->
                val newEventTime = overlayState.eventTimeMillis
                
                if (newEventTime == lastScheduledEventTime) {
                    return@collect
                }
                
                lastScheduledEventTime = newEventTime
                
                if (newEventTime != null) {
                    val now = System.currentTimeMillis()
                    val eventName = getEventDisplayName(newEventTime)
                    val fiveMinutesInMillis = 5 * 60 * 1000L
                    val timeUntilEvent = newEventTime - now
                    
                    android.util.Log.i("EfficientNotif", "📅 Real event detected: $eventName at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(newEventTime))}")
                    android.util.Log.i("EfficientNotif", "   ⏳ Time until event: ${timeUntilEvent / 60000} minutes")
                    
                    if (timeUntilEvent > fiveMinutesInMillis) {
                        eventNotificationManager.scheduleEventNotification(eventName, newEventTime)
                        android.util.Log.i("EfficientNotif", "⏰ Scheduled 5-minute reminder for: $eventName")
                    } else if (timeUntilEvent > 0) {
                        android.util.Log.w("EfficientNotif", "⚠️ Event too close (${timeUntilEvent / 60000} minutes) - no 5-minute reminder needed")
                    } else {
                        android.util.Log.w("EfficientNotif", "⚠️ Event is in the past - no reminder needed")
                    }
                } else {
                    eventNotificationManager.cancelScheduledNotification()
                    android.util.Log.i("EfficientNotif", "❌ No active events - cancelled 5-minute reminders")
                }
            }
        }
    }
    
    private fun getEventDisplayName(eventTimeMillis: Long): String {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val timeString = formatter.format(java.util.Date(eventTimeMillis))
        return "Event at $timeString"
    }
    
    private fun requestAllNotificationPermissions() {
        android.util.Log.d("MainActivity", "=== REQUESTING ALL PERMISSIONS ===")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                android.util.Log.d("MainActivity", "POST_NOTIFICATIONS already granted")
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("MainActivity", "SCHEDULE_EXACT_ALARM permission not granted")
            } else {
                android.util.Log.d("MainActivity", "SCHEDULE_EXACT_ALARM already granted")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::eventNotificationManager.isInitialized) {
            eventNotificationManager.cancelScheduledNotification()
        }
    }
}

@Composable
fun PictureInPictureFloatingClock(viewModel: MainViewModel) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    
    var isPulsing by remember { mutableStateOf(false) }
    var pulsingStartTime by remember { mutableLongStateOf(0L) }
    var lastTimeDifference by remember { mutableLongStateOf(Long.MAX_VALUE) }
    
    var progressInfo by remember { mutableStateOf(ProgressInfo(0f, false)) }
    
    val frameOptimizer = PerformanceOptimizations.rememberFrameRateOptimizer()
    val throttledUpdater = remember { PerformanceOptimizations.ThrottledUpdater(16L) }
    
    val timeState by viewModel.timeState.collectAsState()
    val overlayState by viewModel.overlayState.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val scheduledEventTime = overlayState.eventTimeMillis
    
    LaunchedEffect(timeState, scheduledEventTime, userPreferences.floatingClockStyle.showMillis, userPreferences.floatingClockStyle.progressActivationSeconds) {
        while (true) {
            if (frameOptimizer.shouldSkipFrame()) {
                delay(1L)
                continue
            }
            
            if (!throttledUpdater.shouldUpdate()) {
                delay(4L)
                continue
            }
            
            val now = if (timeState.isInitialized) {
                val currentState = timeState
                val elapsedDelta = android.os.SystemClock.elapsedRealtime() - currentState.baseElapsedRealtimeMillis
                currentState.baseNetworkTimeMillis + elapsedDelta
            } else {
                System.currentTimeMillis()
            }
            
            MainThreadOptimizer.executeOffMainThread(
                operation = {
                    Pair(
                        DateTimeFormatters.formatTime(now, showSeconds = true, showMillis = userPreferences.floatingClockStyle.showMillis),
                        DateTimeFormatters.formatDate(now)
                    )
                },
                onResult = { (timeText: String, dateText: String) ->
                    currentTime = timeText
                    currentDate = dateText
                }
            )
            
            if (scheduledEventTime != null) {
                val timeDifference = scheduledEventTime - now
                
                if (!isPulsing && 
                    ((lastTimeDifference > 0L && timeDifference <= 0L) ||
                     (timeDifference == 0L) ||
                     (timeDifference >= -50L && timeDifference <= 50L && lastTimeDifference > 50L))) {
                    isPulsing = true
                    pulsingStartTime = now
                }
                
                lastTimeDifference = timeDifference
                
                if (isPulsing && (now - pulsingStartTime) >= 5000L) {
                    isPulsing = false
                }
                
                val activationMs = userPreferences.floatingClockStyle.progressActivationSeconds * 1000L
                progressInfo = when {
                    timeDifference > activationMs -> ProgressInfo(0f, false)
                    timeDifference >= 0L -> {
                        val progress = (activationMs - timeDifference) / activationMs.toFloat()
                        ProgressInfo(progress.coerceIn(0f, 1f), true)
                    }
                    timeDifference >= -activationMs -> {
                        val progress = (activationMs + timeDifference) / activationMs.toFloat()
                        ProgressInfo(progress.coerceIn(0f, 1f), true)
                    }
                    else -> ProgressInfo(0f, false)
                }
            } else {
                progressInfo = ProgressInfo(0f, false)
                isPulsing = false
                lastTimeDifference = Long.MAX_VALUE
            }
            
            delay(8)
        }
    }
    
    val targetTime = remember(scheduledEventTime) {
        scheduledEventTime?.let { eventMillis ->
            DateTimeFormatters.formatTargetTime(eventMillis)
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val passiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f)
    
    val pulsingColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    val normalColor = surfaceColor
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPulsing) pulsingColor else normalColor,
        animationSpec = if (isPulsing) {
            infiniteRepeatable(
                animation = tween(userPreferences.floatingClockStyle.pulsingSpeedMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "backgroundPulsing"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            UIComponents.TimeText(
                text = currentTime,
                color = primaryColor
            )
            
            UIComponents.Line2Content(
                displayMode = userPreferences.floatingClockStyle.getLine2DisplayMode(),
                currentDate = currentDate,
                targetTime = targetTime,
                dateColor = passiveColor,
                targetColor = if (progressInfo.isActive) primaryColor else passiveColor
            )
            
            if (scheduledEventTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (scheduledEventTime != null) {
                LinearProgressIndicator(
                    progress = { progressInfo.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (progressInfo.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

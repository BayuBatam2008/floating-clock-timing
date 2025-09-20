package com.floatingclock.timing

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import com.floatingclock.timing.ui.FloatingClockApp
import com.floatingclock.timing.ui.theme.FloatingClockTheme

// Data class for progress indicator information
data class ProgressInfo(
    val progress: Float,
    val isActive: Boolean
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    
    // State for PiP mode detection
    private var isPiPModeState: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    // Show floating clock in PiP mode
                    android.util.Log.d("MainActivity", "Showing PictureInPictureFloatingClock")
                    PictureInPictureFloatingClock(viewModel)
                } else {
                    // Show main app
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // Update PiP state immediately when mode changes
        android.util.Log.d("MainActivity", "PiP mode changed to: $isInPictureInPictureMode")
        isPiPModeState.value = isInPictureInPictureMode
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(23, 10)) // 2.3 aspect ratio - safely within valid range
                .build()
            enterPictureInPictureMode(params)
        } else {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }
}

@Composable
fun PictureInPictureFloatingClock(viewModel: MainViewModel) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    
    // Pulsing animation state
    var isPulsing by remember { mutableStateOf(false) }
    var pulsingStartTime by remember { mutableStateOf(0L) }
    var lastTimeDifference by remember { mutableStateOf(Long.MAX_VALUE) }
    
    // Get timeState to access accurate time
    val timeState by viewModel.timeState.collectAsState()
    
    LaunchedEffect(timeState) {
        while (true) {
            // Use accurate time from TimeSyncManager instead of system time
            val now = if (timeState.isInitialized) {
                val currentState = timeState
                val elapsedDelta = android.os.SystemClock.elapsedRealtime() - currentState.baseElapsedRealtimeMillis
                currentState.baseNetworkTimeMillis + elapsedDelta
            } else {
                System.currentTimeMillis()
            }
            
            val instant = java.time.Instant.ofEpochMilli(now)
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            
            // Use same format as live preview
            val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
            
            currentTime = timeFormatter.format(zoned)
            currentDate = dateFormatter.format(zoned)
            delay(16) // ~60 FPS for smooth milliseconds
        }
    }
    
    // Get overlay state to access scheduled event time
    val overlayState by viewModel.overlayState.collectAsState()
    val scheduledEventTime = overlayState.eventTimeMillis
    
    // Update progress info continuously when active
    var progressInfo by remember { mutableStateOf(ProgressInfo(0f, false)) }
    LaunchedEffect(scheduledEventTime, timeState) {
        while (scheduledEventTime != null) {
            // Use accurate time from TimeSyncManager
            val currentMillis = if (timeState.isInitialized) {
                val currentState = timeState
                val elapsedDelta = android.os.SystemClock.elapsedRealtime() - currentState.baseElapsedRealtimeMillis
                currentState.baseNetworkTimeMillis + elapsedDelta
            } else {
                System.currentTimeMillis()
            }
            
            val timeDifference = scheduledEventTime - currentMillis
            
            // Precise target time detection: trigger when we cross from positive to negative or at exact match
            if (!isPulsing && 
                ((lastTimeDifference > 0L && timeDifference <= 0L) || // Just crossed target time
                 (timeDifference == 0L) || // Exact match (rare but possible)
                 (timeDifference >= -50L && timeDifference <= 50L && lastTimeDifference > 50L))) { // Within 50ms after target
                isPulsing = true
                pulsingStartTime = currentMillis
            }
            
            // Update lastTimeDifference for next comparison
            lastTimeDifference = timeDifference
            
            // Stop pulsing after 5 seconds
            if (isPulsing && (currentMillis - pulsingStartTime) >= 5000L) {
                isPulsing = false
            }
            
            progressInfo = when {
                // Before 5 seconds: inactive
                timeDifference > 5000L -> ProgressInfo(0f, false)
                // Within 5 seconds before: filling up
                timeDifference >= 0L -> {
                    val progress = (5000L - timeDifference) / 5000f
                    ProgressInfo(progress.coerceIn(0f, 1f), true)
                }
                // Within 5 seconds after: emptying
                timeDifference >= -5000L -> {
                    val progress = (5000L + timeDifference) / 5000f
                    ProgressInfo(progress.coerceIn(0f, 1f), true)
                }
                // After 5 seconds: inactive
                else -> ProgressInfo(0f, false)
            }
            
            delay(16) // Update every 16ms for smooth 60 FPS progress
        }
        // Reset progress and pulsing when no scheduled event
        if (scheduledEventTime == null) {
            progressInfo = ProgressInfo(0f, false)
            isPulsing = false
            lastTimeDifference = Long.MAX_VALUE
        }
    }
    
    // Format target time if scheduled event exists
    val targetTime = remember(scheduledEventTime) {
        scheduledEventTime?.let { eventMillis: Long ->
            val instant = java.time.Instant.ofEpochMilli(eventMillis)
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            formatter.format(zoned)
        }
    }
    
    // Use Material3 color scheme
    val primaryColor = MaterialTheme.colorScheme.primary // Main theme color
    val passiveColor = MaterialTheme.colorScheme.onSurfaceVariant // Passive color
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f)
    
    // Pulsing animation colors
    val pulsingColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    val normalColor = surfaceColor
    
    // Animate background color based on pulsing state
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPulsing) pulsingColor else normalColor,
        animationSpec = if (isPulsing) {
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
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
            horizontalAlignment = Alignment.Start // Left align the entire column
        ) {
            // Line 1: Time - large font, main theme color, left aligned
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 24.sp, // Large font size
                    fontWeight = FontWeight.Bold
                ),
                color = primaryColor, // Main theme color
                textAlign = TextAlign.Start // Left aligned
            )
            
            // Line 2: Date (left) and Target Time (right) - small font, passive color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date - left aligned, small font, passive color
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp // Small font size
                    ),
                    color = passiveColor, // Passive color
                    textAlign = TextAlign.Start
                )
                
                // Target time - right aligned, small font, passive color (if exists)
                targetTime?.let { target ->
                    Text(
                        text = target,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp // Small font size
                        ),
                        color = passiveColor, // Passive color
                        textAlign = TextAlign.End
                    )
                }
            }
            
            // Add padding between line 2 and line 3
            if (scheduledEventTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Line 3: Standard Material 3 Linear Progress Indicator
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

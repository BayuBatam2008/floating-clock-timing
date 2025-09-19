package com.floatingclock.timing.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatingclock.timing.MainViewModel
import com.floatingclock.timing.R
import com.floatingclock.timing.data.TimeSyncState
import com.floatingclock.timing.data.model.FloatingClockStyle
import com.floatingclock.timing.data.model.UserPreferences
import com.floatingclock.timing.overlay.FloatingOverlaySurface
import com.floatingclock.timing.overlay.FloatingOverlayUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FloatingClockApp(
    viewModel: MainViewModel,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onEnterPip: () -> Unit
) {
    val timeState by viewModel.timeState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Clock) }
    val overlayActive = overlayState.isVisible || viewModel.isOverlayActive()
    
    // Track scroll states for FAB visibility
    val clockScrollState = rememberScrollState()
    val syncScrollState = rememberScrollState()
    val styleScrollState = rememberScrollState()
    
    val currentScrollState = when (selectedTab) {
        MainTab.Clock -> clockScrollState
        MainTab.Sync -> syncScrollState
        MainTab.Style -> styleScrollState
    }
    
    // FAB visibility based on scroll
    val fabExpanded = currentScrollState.value <= 100

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "topBar"
            ) { tab ->
                CenterAlignedTopAppBar(
                    title = { Text(text = stringResource(id = tab.titleRes)) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(text = stringResource(id = tab.titleRes)) }
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "fab"
            ) { tab ->
                when (tab) {
                    MainTab.Clock -> ClockFab(
                        overlayActive = overlayActive,
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onStartOverlay = {
                            viewModel.showOverlay()
                            onEnterPip()
                        },
                        onStopOverlay = viewModel::hideOverlay,
                        expanded = fabExpanded
                    )
                    MainTab.Sync -> SyncFab(
                        isSyncing = timeState.isSyncing,
                        onSync = viewModel::syncNow,
                        expanded = fabExpanded
                    )
                    MainTab.Style -> {}
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            modifier = Modifier.padding(innerPadding),
            label = "tabContent"
        ) { tab ->
            when (tab) {
                MainTab.Clock -> ClockTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(clockScrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    timeState = timeState,
                    overlayState = overlayState,
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onScheduleEvent = viewModel::scheduleEvent,
                    onClearEvent = viewModel::clearEvent
                )
                MainTab.Sync -> SyncTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(syncScrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    viewModel = viewModel,
                    timeState = timeState,
                    userPreferences = userPreferences
                )
                MainTab.Style -> CustomizationTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(styleScrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    viewModel = viewModel,
                    overlayState = overlayState,
                    style = userPreferences.floatingClockStyle
                )
            }
        }
    }
}

enum class MainTab(val titleRes: Int, val icon: ImageVector) {
    Clock(R.string.tab_clock, Icons.Default.Schedule),
    Sync(R.string.tab_settings, Icons.Default.Cloud),
    Style(R.string.tab_customization, Icons.Default.ColorLens)
}

@Composable
private fun ClockFab(
    overlayActive: Boolean,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    expanded: Boolean = true
) {
    val containerColor = if (overlayActive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (overlayActive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    ExtendedFloatingActionButton(
        onClick = {
            if (overlayActive) {
                onStopOverlay()
            } else if (hasOverlayPermission) {
                onStartOverlay()
            } else {
                onRequestOverlayPermission()
            }
        },
        icon = {
            Icon(
                imageVector = if (overlayActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
        },
        text = {
            Text(
                text = stringResource(
                    id = if (overlayActive) R.string.stop_overlay else R.string.start_overlay
                )
            )
        },
        containerColor = containerColor,
        contentColor = contentColor,
        expanded = expanded
    )
}

@Composable
private fun SyncFab(
    isSyncing: Boolean,
    onSync: () -> Unit,
    expanded: Boolean = true
) {
    ExtendedFloatingActionButton(
        text = {
            Text(
                text = if (isSyncing) stringResource(id = R.string.syncing) else stringResource(id = R.string.sync_now)
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null
            )
        },
        onClick = { if (!isSyncing) onSync() },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        expanded = expanded
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ClockTab(
    modifier: Modifier,
    timeState: TimeSyncState,
    overlayState: FloatingOverlayUiState,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onScheduleEvent: (Instant) -> Unit,
    onClearEvent: () -> Unit
) {
    var currentMillis by remember(timeState.baseNetworkTimeMillis, timeState.baseElapsedRealtimeMillis) {
        mutableLongStateOf(timeState.baseNetworkTimeMillis)
    }
    
    // Optimized time update with less frequent updates
    LaunchedEffect(timeState.baseNetworkTimeMillis, timeState.baseElapsedRealtimeMillis) {
        while (true) {
            currentMillis = timeState.baseNetworkTimeMillis +
                (SystemClock.elapsedRealtime() - timeState.baseElapsedRealtimeMillis)
            delay(10L) // Reduced from 16L for smoother updates
        }
    }
    val currentInstant = Instant.ofEpochMilli(currentMillis)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SyncedTimeCard(currentInstant = currentInstant, timeState = timeState)

        AnimatedVisibility(visible = !hasOverlayPermission, enter = fadeIn(), exit = fadeOut()) {
            PermissionCard(onRequest = onRequestOverlayPermission)
        }

        EventSchedulerCard(
            currentInstant = currentInstant,
            scheduledEvent = overlayState.eventTimeMillis?.let(Instant::ofEpochMilli),
            onSchedule = onScheduleEvent,
            onClear = onClearEvent
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SyncedTimeCard(
    currentInstant: Instant,
    timeState: TimeSyncState
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS") }
    
    // Optimize formatting to prevent excessive recomposition
    val formattedTime = remember(currentInstant.epochSecond, currentInstant.toEpochMilli() / 10) { 
        formatter.format(currentInstant.atZone(ZoneId.systemDefault())) 
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.synced_time_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Remove AnimatedContent to reduce flickering
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
            )
            AnimatedContent(
                targetState = Pair(timeState.offsetMillis, timeState.roundTripMillis),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "offset"
            ) { (offset, rtt) ->
                Text(
                    text = stringResource(id = R.string.offset_rtt, offset, rtt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.overlay_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(id = R.string.overlay_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRequest) {
                Text(text = stringResource(id = R.string.open_settings))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EventSchedulerCard(
    currentInstant: Instant,
    scheduledEvent: Instant?,
    onSchedule: (Instant) -> Unit,
    onClear: () -> Unit
) {
    var digits by rememberSaveable { mutableStateOf("000000000") }
    var showError by rememberSaveable { mutableStateOf(false) }

    val durationMillis = remember(digits) { digitsToDurationMillis(digits) }
    val (hours, minutes, seconds, millis) = remember(digits) { digitsToSegments(digits) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(id = R.string.schedule_event), style = MaterialTheme.typography.titleMedium)

            EventTimeDisplay(hours = hours, minutes = minutes, seconds = seconds, millis = millis)

            AnimatedVisibility(visible = showError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(id = R.string.invalid_time_format),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Optimized chip row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                remember { listOf(5L, 10L, 30L, 60L) }.forEach { secondsToAdd ->
                    AssistChip(
                        onClick = {
                            digits = durationToDigits(
                                (durationMillis + secondsToAdd * 1_000L).coerceAtMost(MAX_DURATION_MILLIS)
                            )
                        },
                        label = { Text(text = "+${secondsToAdd}s") }
                    )
                }
            }

            // Optimized keypad with remember for static data
            val keypadRows = remember {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("00", "0", "<")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keypadRows.forEachIndexed { rowIndex, row ->
                    key(rowIndex) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { keyIndex, key ->
                                key("$rowIndex-$keyIndex") {
                                    KeypadButton(
                                        modifier = Modifier.weight(1f),
                                        label = key,
                                        onClick = {
                                            when (key) {
                                                "<" -> digits = backspaceDigits(digits)
                                                else -> digits = appendDigits(digits, key)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Button(onClick = {
                    if (durationMillis > 0L) {
                        showError = false
                        onSchedule(currentInstant.plusMillis(durationMillis))
                    } else {
                        showError = true
                    }
                }) {
                    Text(text = stringResource(id = R.string.schedule_event))
                }
                TextButton(onClick = {
                    digits = "000000000"
                    showError = false
                    onClear()
                }) {
                    Text(text = stringResource(id = R.string.clear_event))
                }
            }

            AnimatedVisibility(visible = scheduledEvent != null, enter = fadeIn(), exit = fadeOut()) {
                scheduledEvent?.let { instant ->
                    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss.SSS") }
                    Text(
                        text = stringResource(
                            id = R.string.scheduled_time,
                            formatter.format(instant.atZone(ZoneId.systemDefault()))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTimeDisplay(
    hours: String,
    minutes: String,
    seconds: String,
    millis: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeSegment(value = hours, label = "h")
        TimeSegment(value = minutes, label = "m")
        TimeSegment(value = seconds, label = "s")
        TimeSegment(value = millis, label = "ms")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TimeSegment(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
            label = "segment$value"
        ) { displayValue ->
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = CircleShape,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (label == "<") {
                Icon(imageVector = Icons.Default.Backspace, contentDescription = null)
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SyncTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    timeState: TimeSyncState,
    userPreferences: UserPreferences
) {
    val syncFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss") }
    var customServer by rememberSaveable { mutableStateOf("") }
    var sliderValue by remember(userPreferences.syncIntervalMinutes) {
        mutableFloatStateOf(userPreferences.syncIntervalMinutes.toFloat())
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(id = R.string.select_ntp_server), style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.fillMaxWidth()) {
                    timeState.availableServers.forEach { server ->
                        val selected = userPreferences.selectedServer == server
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { viewModel.selectServer(server) })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = server, style = MaterialTheme.typography.bodyLarge)
                                if (timeState.lastSuccessfulServer == server) {
                                    Text(
                                        text = stringResource(id = R.string.last_success),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.selectServer(server) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                        Divider()
                    }
                }
                OutlinedTextField(
                    value = customServer,
                    onValueChange = { customServer = it },
                    label = { Text(text = stringResource(id = R.string.custom_server_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(onClick = {
                        if (customServer.isNotBlank()) {
                            viewModel.addCustomServer(customServer)
                            customServer = ""
                        }
                    }) {
                        Text(text = stringResource(id = R.string.add_custom_server))
                    }
                    TextButton(
                        onClick = {
                            viewModel.removeCustomServer(customServer)
                            customServer = ""
                        },
                        enabled = customServer.isNotBlank() && userPreferences.customServers.contains(customServer)
                    ) {
                        Text(text = stringResource(id = R.string.remove))
                    }
                }
                AnimatedVisibility(visible = userPreferences.customServers.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(id = R.string.custom_servers), style = MaterialTheme.typography.titleSmall)
                        userPreferences.customServers.forEach { server ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectServer(server) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = server, modifier = Modifier.weight(1f))
                                RadioButton(
                                    selected = userPreferences.selectedServer == server,
                                    onClick = { viewModel.selectServer(server) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.auto_sync_enabled),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Switch(
                        checked = userPreferences.autoSyncEnabled,
                        onCheckedChange = viewModel::setAutoSync
                    )
                }
                AnimatedVisibility(visible = userPreferences.autoSyncEnabled, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(id = R.string.auto_sync_interval),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AutoSyncIntervalSlider(
                            value = sliderValue.roundToInt(),
                            onValueChange = { sliderValue = it.toFloat() },
                            onValueChangeFinished = {
                                viewModel.setSyncIntervalMinutes(sliderValue.roundToInt())
                            }
                        )
                    }
                }
                Text(
                    text = timeState.lastSyncInstant?.let {
                        stringResource(id = R.string.last_synced, syncFormatter.format(it.atZone(ZoneId.systemDefault())))
                    } ?: stringResource(id = R.string.waiting_for_sync),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                timeState.nextSyncAtMillis?.let { next ->
                    val remainingMinutes = ((next - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                    Text(
                        text = stringResource(id = R.string.next_sync, remainingMinutes.toInt()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AnimatedVisibility(visible = timeState.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = timeState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CustomizationTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    overlayState: FloatingOverlayUiState,
    style: FloatingClockStyle
) {
    // Add smooth time updates for preview
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(10L) // Same smooth update as main clock
        }
    }
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(id = R.string.clock_style_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(id = R.string.font_scale), style = MaterialTheme.typography.titleSmall)
                ValueIndicatorSlider(
                    value = style.fontScale,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(fontScale = value.coerceIn(0.6f, 1.6f)) } },
                    valueRange = 0.6f..1.6f,
                    steps = 0,
                    label = { "" }
                )
                Text(text = stringResource(id = R.string.corner_radius), style = MaterialTheme.typography.titleSmall)
                ValueIndicatorSlider(
                    value = style.cornerRadiusDp,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(cornerRadiusDp = value.coerceIn(8f, 48f)) } },
                    valueRange = 8f..48f,
                    steps = 0,
                    label = { "" }
                )
                Text(text = stringResource(id = R.string.opacity), style = MaterialTheme.typography.titleSmall)
                ValueIndicatorSlider(
                    value = style.backgroundOpacity,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(backgroundOpacity = value.coerceIn(0.4f, 1f)) } },
                    valueRange = 0.4f..1f,
                    steps = 0,
                    label = { "" }
                )
                SettingSwitchRow(
                    title = stringResource(id = R.string.use_dynamic_color),
                    checked = style.useDynamicColor,
                    onCheckedChange = { enabled ->
                        viewModel.updateStyle {
                            it.copy(useDynamicColor = enabled, accentColor = if (enabled) null else it.accentColor)
                        }
                    }
                )
                SettingSwitchRow(
                    title = stringResource(id = R.string.show_seconds),
                    checked = style.showSeconds,
                    onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showSeconds = enabled) } }
                )
                SettingSwitchRow(
                    title = stringResource(id = R.string.show_millis),
                    checked = style.showMillis,
                    onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showMillis = enabled) } }
                )
                AnimatedVisibility(visible = !style.useDynamicColor, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(id = R.string.accent_color), style = MaterialTheme.typography.titleSmall)
                        HuePicker(
                            selectedColor = style.accentColor ?: 0xFF3DDC84,
                            onColorSelected = { colorLong ->
                                viewModel.updateStyle { it.copy(accentColor = colorLong) }
                            }
                        )
                    }
                }
            }
        }

        Text(text = stringResource(id = R.string.live_preview), style = MaterialTheme.typography.titleMedium)
        FloatingOverlaySurface(
            state = overlayState.copy(
                style = style, 
                isVisible = true,
                currentTimeMillis = currentMillis // Pass smooth time updates
            ),
            onClose = {},
            onDrag = { _, _ -> }
        )
    }
}

@Composable
private fun HuePicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val density = LocalDensity.current
    var currentHue by remember {
        val color = Color(selectedColor)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.value.toInt(), hsv)
        mutableFloatStateOf(hsv[0])
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hue bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val hue = (x / size.width) * 360f
                        currentHue = hue
                        val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                        onColorSelected(hsvColor.toULong().toLong())
                    }
                }
        ) {
            val gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            )
            drawRect(gradient)
            
            // Thumb indicator
            val thumbX = (currentHue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = with(density) { 8.dp.toPx() },
                center = Offset(thumbX, size.height / 2),
            )
            drawCircle(
                color = Color.Black,
                radius = with(density) { 6.dp.toPx() },
                center = Offset(thumbX, size.height / 2),
            )
        }
        
        // Selected color preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(selectedColor))
        ) {}
    }
}

@Composable
private fun AutoSyncIntervalSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val steps = listOf(2, 5, 10, 20, 30, 40, 50, 60)
    val currentIndex = steps.indexOfFirst { it >= value }.let { if (it == -1) steps.size - 1 else it }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val density = LocalDensity.current
        val indicatorWidth = 64.dp
        val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
        val fraction = currentIndex.toFloat() / (steps.size - 1).coerceAtLeast(1)
        val offsetPx = ((constraints.maxWidth - indicatorWidthPx).coerceAtLeast(0f) * fraction).roundToInt()

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(offsetPx, 0) }
        ) {
            Text(
                text = "${steps[currentIndex]}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { newIndex ->
                val index = newIndex.roundToInt().coerceIn(0, steps.size - 1)
                onValueChange(steps[index])
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onValueChangeFinished = { onValueChangeFinished?.invoke() }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ValueIndicatorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    label: (Float) -> String
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val density = LocalDensity.current
        val indicatorWidth = 64.dp
        val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
        val fraction = if (valueRange.endInclusive == valueRange.start) {
            0f
        } else {
            ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        }
        val offsetPx = ((constraints.maxWidth - indicatorWidthPx).coerceAtLeast(0f) * fraction).roundToInt()

        // Show the value indicator
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(offsetPx, 0) }
        ) {
            Text(
                text = when {
                    label(value).isNotEmpty() -> label(value)
                    value >= 1f -> String.format("%.1f", value)
                    else -> String.format("%.2f", value)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onValueChangeFinished = { onValueChangeFinished?.invoke() }
        )
    }
}

private const val MAX_DURATION_MILLIS = 359_999_999L

private fun digitsToSegments(digits: String): Quadruple<String, String, String, String> {
    val padded = digits.padStart(9, '0')
    val hours = padded.substring(0, 2)
    val minutes = padded.substring(2, 4)
    val seconds = padded.substring(4, 6)
    val millis = padded.substring(6, 9)
    return Quadruple(hours, minutes, seconds, millis)
}

private fun appendDigits(current: String, input: String): String {
    val filtered = input.filter { it.isDigit() }
    if (filtered.isEmpty()) return current
    val updated = (current + filtered).takeLast(9)
    return updated.padStart(9, '0')
}

private fun backspaceDigits(current: String): String {
    val updated = ("0" + current).dropLast(1)
    return updated.padStart(9, '0')
}

private fun digitsToDurationMillis(digits: String): Long {
    val padded = digits.padStart(9, '0')
    val hours = padded.substring(0, 2).toInt()
    val minutes = padded.substring(2, 4).toInt().coerceAtMost(59)
    val seconds = padded.substring(4, 6).toInt().coerceAtMost(59)
    val millis = padded.substring(6, 9).toInt()
    var totalMillis = millis.toLong()
    totalMillis += TimeUnit.SECONDS.toMillis(seconds.toLong())
    totalMillis += TimeUnit.MINUTES.toMillis(minutes.toLong())
    totalMillis += TimeUnit.HOURS.toMillis(hours.toLong())
    return totalMillis.coerceAtMost(MAX_DURATION_MILLIS)
}

private fun durationToDigits(durationMillis: Long): String {
    var remaining = durationMillis.coerceIn(0, MAX_DURATION_MILLIS)
    val hours = (remaining / TimeUnit.HOURS.toMillis(1)).toInt().coerceAtMost(99)
    remaining -= TimeUnit.HOURS.toMillis(hours.toLong())
    val minutes = (remaining / TimeUnit.MINUTES.toMillis(1)).toInt().coerceAtMost(59)
    remaining -= TimeUnit.MINUTES.toMillis(minutes.toLong())
    val seconds = (remaining / TimeUnit.SECONDS.toMillis(1)).toInt().coerceAtMost(59)
    remaining -= TimeUnit.SECONDS.toMillis(seconds.toLong())
    val millis = remaining.toInt().coerceAtMost(999)
    return String.format("%02d%02d%02d%03d", hours, minutes, seconds, millis)
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

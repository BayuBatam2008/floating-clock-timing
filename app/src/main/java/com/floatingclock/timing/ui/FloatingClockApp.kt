package com.floatingclock.timing.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatingclock.timing.MainViewModel
import com.floatingclock.timing.R
import com.floatingclock.timing.ui.events.EventsScreen
import com.floatingclock.timing.ui.events.EventViewModel
import com.floatingclock.timing.ui.events.EventEditModal
import com.floatingclock.timing.data.TimeSyncState
import com.floatingclock.timing.data.model.FloatingClockStyle
import com.floatingclock.timing.data.model.UserPreferences
import com.floatingclock.timing.data.model.Line2DisplayMode
import com.floatingclock.timing.data.PerformanceOptimizations.MainThreadOptimizer
import com.floatingclock.timing.overlay.FloatingOverlaySurface
import com.floatingclock.timing.overlay.FloatingOverlayUiState
import com.floatingclock.timing.utils.DateTimeFormatters
import com.floatingclock.timing.utils.UIComponents
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
    
    // EventViewModel for managing events
    val eventViewModel: EventViewModel = viewModel(
        factory = EventViewModel.Factory(
            (LocalContext.current.applicationContext as com.floatingclock.timing.FloatingClockApplication).eventRepository
        )
    )

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Clock) }
    val overlayActive = overlayState.isVisible || viewModel.isOverlayActive()
    
    // Events state management
    var eventsSelectionMode by remember { mutableStateOf(false) }
    var eventsSelectedEvents by remember { mutableStateOf(setOf<String>()) }
    
    // Track scroll states for FAB visibility
    val clockScrollState = rememberScrollState()
    val eventsScrollState = rememberScrollState()
    val syncScrollState = rememberScrollState()
    val styleScrollState = rememberScrollState()
    
    val currentScrollState = when (selectedTab) {
        MainTab.Clock -> clockScrollState
        MainTab.Events -> eventsScrollState
        MainTab.Sync -> syncScrollState
        MainTab.Style -> styleScrollState
    }
    
    // FAB visibility based on scroll
    val fabExpanded = currentScrollState.value <= 100
    
    // Global SnackbarHost state
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by eventViewModel.errorMessage.collectAsState()
    
    // Show snackbar when error message is available
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            eventViewModel.clearErrorMessage()
        }
    }

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
                MainTab.entries.forEach { tab ->
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
                            // Only enter PiP mode, don't start overlay service
                            onEnterPip()
                        },
                        onStopOverlay = viewModel::hideOverlay,
                        expanded = fabExpanded
                    )
                    MainTab.Events -> EventsFab(
                        isSelectionMode = eventsSelectionMode,
                        selectedCount = eventsSelectedEvents.size,
                        onCreateEvent = { 
                            eventViewModel.showCreateEventDialog()
                        },
                        onDeleteSelected = { 
                            eventsSelectedEvents.forEach { eventId ->
                                eventViewModel.deleteEvent(eventId)
                            }
                            eventsSelectedEvents = emptySet()
                            eventsSelectionMode = false
                        },
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
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            )
        }
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
                    onClearEvent = viewModel::clearEvent,
                    eventViewModel = eventViewModel
                )
                MainTab.Events -> EventsScreen(
                    modifier = Modifier.fillMaxSize(),
                    eventViewModel = eventViewModel,
                    isSelectionMode = eventsSelectionMode,
                    selectedEvents = eventsSelectedEvents,
                    onSelectionModeChange = { eventsSelectionMode = it },
                    onSelectedEventsChange = { eventsSelectedEvents = it }
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
    
    // EventEditModal for quick add functionality - only show when on Clock tab
    val showEventDialog by eventViewModel.showEventDialog.collectAsState()
    if (showEventDialog && selectedTab == MainTab.Clock) {
        EventEditModal(
            eventViewModel = eventViewModel,
            onDismiss = { eventViewModel.hideEventDialog() }
        )
    }
}

enum class MainTab(val titleRes: Int, val icon: ImageVector) {
    Clock(R.string.tab_clock, Icons.Default.Schedule),
    Events(R.string.tab_events, Icons.Default.Event),
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

@Composable
private fun EventsFab(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onCreateEvent: () -> Unit,
    onDeleteSelected: () -> Unit,
    expanded: Boolean = true
) {
    if (isSelectionMode && selectedCount > 0) {
        // Delete FAB in selection mode
        ExtendedFloatingActionButton(
            text = {
                Text(text = "Delete ($selectedCount)")
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            onClick = onDeleteSelected,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            expanded = expanded
        )
    } else if (!isSelectionMode) {
        // Add Event FAB in normal mode
        ExtendedFloatingActionButton(
            text = {
                Text(text = "Add Event")
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            onClick = onCreateEvent,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            expanded = expanded
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ClockTab(
    modifier: Modifier,
    timeState: TimeSyncState,
    overlayState: FloatingOverlayUiState,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onClearEvent: () -> Unit,
    eventViewModel: EventViewModel
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

        QuickAddEventCard(
            onAddEvent = { eventViewModel.showCreateEventDialog() }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SyncedTimeCard(
    currentInstant: Instant,
    timeState: TimeSyncState
) {
    // Optimize formatting to prevent excessive recomposition
    val formattedTime = remember(currentInstant.epochSecond, currentInstant.toEpochMilli() / 10) { 
        DateTimeFormatters.formatFullSyncTime(currentInstant.toEpochMilli())
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

@Composable
private fun InteractiveTimeDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int,
    millis: Int,
    selectedSegment: Int,
    onSegmentClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours segment (0)
        InteractiveTimeSegment(
            value = "%02d".format(hours),
            label = "h",
            isSelected = selectedSegment == 0,
            onClick = { onSegmentClick(0) }
        )
        
        // Minutes segment (1)
        InteractiveTimeSegment(
            value = "%02d".format(minutes),
            label = "m",
            isSelected = selectedSegment == 1,
            onClick = { onSegmentClick(1) }
        )
        
        // Seconds segment (2)
        InteractiveTimeSegment(
            value = "%02d".format(seconds),
            label = "s",
            isSelected = selectedSegment == 2,
            onClick = { onSegmentClick(2) }
        )
        
        // Milliseconds segment (3)
        InteractiveTimeSegment(
            value = "%03d".format(millis),
            label = "ms",
            isSelected = selectedSegment == 3,
            onClick = { onSegmentClick(3) }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun InteractiveTimeSegment(
    value: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "background_color"
    )
    
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "content_color"
    )

    val animatedLabelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "label_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBackground)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
            label = "segment$value"
        ) { displayValue ->
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = animatedContentColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = animatedLabelColor
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
                Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = null)
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
private fun InteractiveKeypad(
    selectedSegment: Int,
    hours: Int,
    minutes: Int,
    seconds: Int,
    millis: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onMillisChange: (Int) -> Unit
) {
    // State for editing the current segment as string for easier input handling
    var editingValue by rememberSaveable(selectedSegment) { 
        mutableStateOf(
            when (selectedSegment) {
                0 -> hours.toString()
                1 -> minutes.toString()
                2 -> seconds.toString()
                3 -> millis.toString()
                else -> "0"
            }
        )
    }

    // Update editing value when selected segment changes
    LaunchedEffect(selectedSegment, hours, minutes, seconds, millis) {
        editingValue = when (selectedSegment) {
            0 -> hours.toString()
            1 -> minutes.toString()
            2 -> seconds.toString()
            3 -> millis.toString()
            else -> "0"
        }
    }

    // Function to handle value input for the selected segment
    fun updateSelectedSegment(newValue: String) {
        editingValue = newValue
        val intValue = newValue.toIntOrNull() ?: 0
        
        when (selectedSegment) {
            0 -> onHoursChange(intValue.coerceIn(0, 23))
            1 -> onMinutesChange(intValue.coerceIn(0, 59))
            2 -> onSecondsChange(intValue.coerceIn(0, 59))
            3 -> onMillisChange(intValue.coerceIn(0, 999))
        }
    }

    // Optimized keypad with remember for static data
    val keypadRows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("Reset", "0", "⌫")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keypadRows.forEachIndexed { rowIndex, row ->
            key(rowIndex) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { keyIndex, key ->
                        key("$rowIndex-$keyIndex") {
                            InteractiveKeypadButton(
                                modifier = Modifier.weight(1f),
                                label = key,
                                onClick = {
                                    when (key) {
                                        "⌫" -> {
                                            if (editingValue.isNotEmpty()) {
                                                val newValue = editingValue.dropLast(1)
                                                updateSelectedSegment(newValue.ifEmpty { "0" })
                                            }
                                        }
                                        "Reset" -> {
                                            // Clear all time input values
                                            onHoursChange(0)
                                            onMinutesChange(0)
                                            onSecondsChange(0)
                                            onMillisChange(0)
                                            editingValue = "0"
                                        }
                                        else -> {
                                            val maxLength = when (selectedSegment) {
                                                0, 1, 2 -> 2  // Hours, minutes, seconds: max 2 digits
                                                3 -> 3        // Milliseconds: max 3 digits
                                                else -> 2
                                            }
                                            
                                            val maxValue = when (selectedSegment) {
                                                0 -> 23  // Hours: 0-23
                                                1, 2 -> 59  // Minutes, seconds: 0-59
                                                3 -> 999  // Milliseconds: 0-999
                                                else -> 23
                                            }
                                            
                                            // Build new value
                                            val newValue = if (editingValue == "0") {
                                                key
                                            } else {
                                                val candidate = (editingValue + key).take(maxLength)
                                                val candidateInt = candidate.toIntOrNull() ?: 0
                                                if (candidateInt <= maxValue) {
                                                    candidate
                                                } else {
                                                    editingValue // Keep old value if exceeds max
                                                }
                                            }
                                            
                                            updateSelectedSegment(newValue)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveKeypadButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    val isSpecial = label in listOf("Reset", "⌫")
    
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSpecial) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (isSpecial) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun SyncTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    timeState: TimeSyncState,
    userPreferences: UserPreferences
) {
    // Remove local formatter, use centralized utility
    var customServer by rememberSaveable { mutableStateOf("") }
    var sliderValue by remember(userPreferences.syncIntervalMinutes) {
        mutableFloatStateOf(userPreferences.syncIntervalMinutes.toFloat())
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Last Sync Status Card (similar to SyncedTimeCard)
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sync Status", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = timeState.lastSyncInstant?.let {
                        stringResource(id = R.string.last_synced, DateTimeFormatters.formatSyncTime(it.toEpochMilli()))
                    } ?: stringResource(id = R.string.waiting_for_sync),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                timeState.nextSyncAtMillis?.let { next ->
                    val remainingMinutes = ((next - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                    Text(
                        text = stringResource(id = R.string.next_sync, remainingMinutes.toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        HorizontalDivider()
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
                    Button(onClick = {
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
            }
        }
        
        // Add space at bottom to prevent FAB from blocking interactive elements
        Spacer(modifier = Modifier.height(80.dp))
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
                
                // Font & Display Group
                Text(text = stringResource(id = R.string.font_scale), style = MaterialTheme.typography.titleSmall)
                ValueIndicatorSlider(
                    value = style.fontScale,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(fontScale = value.coerceIn(0.6f, 1.6f)) } },
                    valueRange = 0.6f..1.6f,
                    steps = 9, // Creates steps at 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
                    label = { value -> 
                        "${String.format(java.util.Locale.US, "%.1f", value)}x"
                    }
                )
                
                // Divider between Font Scale and Show Millisecond
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingSwitchRow(
                    title = stringResource(id = R.string.show_millis),
                    checked = style.showMillis,
                    onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showMillis = enabled) } }
                )
                
                // Divider between Font & Progress groups
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Progress Indicator Group
                SettingSwitchRow(
                    title = "Enable Progress Indicator",
                    checked = style.showProgressIndicator,
                    onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showProgressIndicator = enabled) } }
                )
                
                // Progress Activation Timing (only show if progress indicator is enabled)
                if (style.showProgressIndicator) {
                    Text(text = "Progress Activation (seconds)", style = MaterialTheme.typography.titleSmall)
                    ValueIndicatorSlider(
                        value = style.progressActivationSeconds.toFloat(),
                        onValueChange = { value -> viewModel.updateStyle { currentStyle -> 
                            currentStyle.copy(progressActivationSeconds = value.toInt().coerceIn(1, 10)) 
                        } },
                        valueRange = 1f..10f,
                        steps = 8,
                        label = { "${it.toInt()}s" }
                    )
                }
                
                // Divider between Progress & Pulsing groups
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Pulsing Group
                SettingSwitchRow(
                    title = "Enable Pulsing",
                    checked = style.enablePulsing,
                    onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(enablePulsing = enabled) } }
                )
                
                // Pulsing Speed (only show if pulsing is enabled)
                if (style.enablePulsing) {
                    Text(text = "Pulsing Speed", style = MaterialTheme.typography.titleSmall)
                    PulsingSpeedSlider(
                        value = style.pulsingSpeedMs,
                        onValueChange = { speed -> viewModel.updateStyle { it.copy(pulsingSpeedMs = speed) } }
                    )
                }
                
                // Divider between Pulsing & Line 2 Display groups
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Line 2 Display Group
                Text(text = "Line 2 Display", style = MaterialTheme.typography.titleSmall)
                ConnectedButtonGroup(
                    selectedOption = style.line2DisplayMode,
                    onOptionSelected = { mode -> viewModel.updateStyle { it.copy(line2DisplayMode = mode) } },
                    options = listOf(
                        "DATE_ONLY" to "Date",
                        "TARGET_TIME_ONLY" to "Target", 
                        "BOTH" to "Both",
                        "NONE" to "None"
                    )
                )
            }
        }

        Text(text = stringResource(id = R.string.live_preview), style = MaterialTheme.typography.titleMedium)
        // Use the same layout as PictureInPictureFloatingClock for consistency
        LivePreviewClock(
            viewModel = viewModel,
            currentMillis = currentMillis
        )
    }
}



@Composable
private fun ConnectedButtonGroup(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    options: List<Pair<String, String>>
) {
    // Material 3 expressive connected button group
    Card(
        shape = RoundedCornerShape(20.dp), // More rounded for Material 3
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp), // Padding inside container
            horizontalArrangement = Arrangement.spacedBy(2.dp) // Small gap between buttons
        ) {
            options.forEachIndexed { index, (value, label) ->
                val isSelected = selectedOption == value
                
                Button(
                    onClick = { onOptionSelected(value) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp), // Standard Material 3 height
                    shape = RoundedCornerShape(16.dp), // Individual button rounding
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSelected) 2.dp else 0.dp,
                        pressedElevation = if (isSelected) 4.dp else 1.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingSpeedSelector(
    selectedSpeed: Int,
    onSpeedSelected: (Int) -> Unit
) {
    val speedOptions = listOf(
        300 to "Very Fast",
        500 to "Fast", 
        700 to "Medium",
        900 to "Slow",
        1200 to "Very Slow"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        speedOptions.forEach { (speed, label) ->
            AssistChip(
                onClick = { onSpeedSelected(speed) },
                label = { Text(label, fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedSpeed == speed) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PulsingSpeedSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    // Map slider values (1-5) to actual milliseconds
    val speedValues = listOf(300, 500, 700, 900, 1200)
    val speedLabels = listOf("Very Fast", "Fast", "Medium", "Slow", "Very Slow")
    
    // Convert current value to slider position (1-5)
    val currentPosition = speedValues.indexOfFirst { it == value }.takeIf { it >= 0 } ?: 1
    val currentLabel = speedLabels.getOrNull(currentPosition) ?: "Medium"
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ValueIndicatorSlider(
            value = (currentPosition + 1).toFloat(),
            onValueChange = { sliderValue -> 
                val index = (sliderValue.toInt() - 1).coerceIn(0, speedValues.size - 1)
                onValueChange(speedValues[index])
            },
            valueRange = 1f..5f,
            steps = 3,
            label = { currentLabel }
        )
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
                    value >= 1f -> String.format(java.util.Locale.US, "%.1f", value)
                    else -> String.format(java.util.Locale.US, "%.2f", value)
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
    val updated = "0$current".dropLast(1)
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

@Composable
private fun LivePreviewClock(
    viewModel: MainViewModel,
    currentMillis: Long
) {
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val scheduledEventTime = overlayState.eventTimeMillis
    
    // Use centralized time formatting utilities
    val currentTime = remember(currentMillis, userPreferences.floatingClockStyle.showMillis) {
        DateTimeFormatters.formatTime(
            currentMillis, 
            showSeconds = true, 
            showMillis = userPreferences.floatingClockStyle.showMillis
        )
    }
    
    val currentDate = remember(currentMillis) {
        DateTimeFormatters.formatDate(currentMillis)
    }
    
    // Format target time with milliseconds if scheduled event exists
    val targetTime = remember(scheduledEventTime) {
        scheduledEventTime?.let { eventMillis ->
            DateTimeFormatters.formatTargetTime(eventMillis)
        }
    }
    
    // Calculate progress info
    val progressInfo = remember(scheduledEventTime, currentMillis) {
        if (scheduledEventTime != null) {
            val timeDifference = scheduledEventTime - currentMillis
            when {
                timeDifference > 5000L -> ProgressInfo(0f, false)
                timeDifference >= 0L -> {
                    val progress = (5000L - timeDifference) / 5000f
                    ProgressInfo(progress.coerceIn(0f, 1f), true)
                }
                timeDifference >= -5000L -> {
                    val progress = (5000L + timeDifference) / 5000f
                    ProgressInfo(progress.coerceIn(0f, 1f), true)
                }
                else -> ProgressInfo(0f, false)
            }
        } else {
            ProgressInfo(0f, false)
        }
    }
    
    // Use Material3 color scheme (always dynamic)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryAccentColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // Line 1: Time - using centralized UI component
            UIComponents.TimeText(
                text = currentTime,
                color = primaryColor,
                fontScale = userPreferences.floatingClockStyle.fontScale
            )
            
            // Line 2: Based on display mode selection - using centralized UI component
            UIComponents.Line2Content(
                displayMode = userPreferences.floatingClockStyle.getLine2DisplayMode(),
                currentDate = currentDate,
                targetTime = targetTime,
                dateColor = secondaryAccentColor,
                targetColor = secondaryAccentColor,
                fontScale = userPreferences.floatingClockStyle.fontScale
            )
            
            // Line 3: Progress Indicator (if target time is set)
            targetTime?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressInfo.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (progressInfo.isActive) secondaryAccentColor else MaterialTheme.colorScheme.surfaceVariant,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

// Data class for progress indicator information
data class ProgressInfo(
    val progress: Float,
    val isActive: Boolean
)

private fun durationToDigits(durationMillis: Long): String {
    var remaining = durationMillis.coerceIn(0, MAX_DURATION_MILLIS)
    val hours = (remaining / TimeUnit.HOURS.toMillis(1)).toInt().coerceAtMost(99)
    remaining -= TimeUnit.HOURS.toMillis(hours.toLong())
    val minutes = (remaining / TimeUnit.MINUTES.toMillis(1)).toInt().coerceAtMost(59)
    remaining -= TimeUnit.MINUTES.toMillis(minutes.toLong())
    val seconds = (remaining / TimeUnit.SECONDS.toMillis(1)).toInt().coerceAtMost(59)
    remaining -= TimeUnit.SECONDS.toMillis(seconds.toLong())
    val millis = remaining.toInt().coerceAtMost(999)
    return String.format(java.util.Locale.US, "%02d%02d%02d%03d", hours, minutes, seconds, millis)
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun QuickAddEventCard(
    onAddEvent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Quick Add Event",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = "Create events with automatic scheduling and lifecycle management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onAddEvent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Event")
            }
        }
    }
}

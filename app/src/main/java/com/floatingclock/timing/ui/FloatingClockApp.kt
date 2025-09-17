package com.floatingclock.timing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

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

    Scaffold(
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
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Clock -> ClockTab(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                viewModel = viewModel,
                timeState = timeState,
                overlayState = overlayState,
                hasOverlayPermission = hasOverlayPermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onEnterPip = onEnterPip
            )
            MainTab.Sync -> SyncTab(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                viewModel = viewModel,
                timeState = timeState,
                userPreferences = userPreferences
            )
            MainTab.Style -> CustomizationTab(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                viewModel = viewModel,
                overlayState = overlayState,
                style = userPreferences.floatingClockStyle
            )
        }
    }
}

enum class MainTab(val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Clock(R.string.tab_clock, Icons.Default.Schedule),
    Sync(R.string.tab_settings, Icons.Default.Cloud),
    Style(R.string.tab_customization, Icons.Default.ColorLens)
}

@Composable
private fun ClockTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    timeState: TimeSyncState,
    overlayState: FloatingOverlayUiState,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onEnterPip: () -> Unit
) {
    val overlayActive = overlayState.isVisible || viewModel.isOverlayActive()
    val currentTimeMillis by androidx.compose.runtime.produceState(
        initialValue = timeState.baseNetworkTimeMillis,
        timeState.baseNetworkTimeMillis,
        timeState.baseElapsedRealtimeMillis
    ) {
        while (true) {
            value = timeState.baseNetworkTimeMillis + (android.os.SystemClock.elapsedRealtime() - timeState.baseElapsedRealtimeMillis)
            delay(200L)
        }
    }
    val currentInstant = Instant.ofEpochMilli(currentTimeMillis)
    val eventFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss.SSS") }
    val invalidFormatText = stringResource(id = R.string.invalid_time_format)
    var eventInput by rememberSaveable { mutableStateOf("") }
    var eventError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.tab_clock),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.synced_time_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss.SSS").format(currentInstant.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = stringResource(id = R.string.offset_rtt, timeState.offsetMillis, timeState.roundTripMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!hasOverlayPermission) {
            PermissionCard(onRequest = onRequestOverlayPermission)
        }

        EventSchedulerCard(
            eventInput = eventInput,
            onEventInputChange = { value ->
                if (value.length <= 12) {
                    eventInput = value
                }
            },
            onSchedule = {
                val scheduledInstant = parseEventTime(eventInput, currentInstant)
                if (scheduledInstant != null) {
                    viewModel.scheduleEvent(scheduledInstant)
                    eventError = null
                } else {
                    eventError = invalidFormatText
                }
            },
            onQuickAdd = { seconds ->
                val target = currentInstant.plusSeconds(seconds.toLong())
                viewModel.scheduleEvent(target)
                eventInput = eventFormatter.format(target.atZone(ZoneId.systemDefault()).toLocalTime())
                eventError = null
            },
            onClear = { viewModel.clearEvent() },
            eventError = eventError,
            scheduledEvent = overlayState.eventTimeMillis?.let { Instant.ofEpochMilli(it) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        viewModel.showOverlay()
                    } else {
                        onRequestOverlayPermission()
                    }
                },
                enabled = !overlayActive
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.start_overlay))
            }
            Button(
                onClick = { viewModel.hideOverlay() },
                enabled = overlayActive
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.stop_overlay))
            }
        }

        Button(onClick = onEnterPip, modifier = Modifier.align(Alignment.Start)) {
            Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.pip_mode))
        }

        Text(
            text = stringResource(id = R.string.floating_preview),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FloatingOverlaySurface(
            state = overlayState.copy(isVisible = true),
            onClose = {},
            onDrag = { _, _ -> }
        )
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
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
            Button(onClick = onRequest) {
                Text(text = stringResource(id = R.string.open_settings))
            }
        }
    }
}

@Composable
private fun EventSchedulerCard(
    eventInput: String,
    onEventInputChange: (String) -> Unit,
    onSchedule: () -> Unit,
    onQuickAdd: (Int) -> Unit,
    onClear: () -> Unit,
    eventError: String?,
    scheduledEvent: Instant?
) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.schedule_event),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = eventInput,
                onValueChange = onEventInputChange,
                label = { Text(text = stringResource(id = R.string.event_time_hint)) },
                supportingText = eventError?.let { { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                isError = eventError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 30, 60).forEach { seconds ->
                    AssistChip(onClick = { onQuickAdd(seconds) }, label = { Text(text = "+${seconds}s") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSchedule) { Text(text = stringResource(id = R.string.schedule_event)) }
                TextButton(onClick = onClear) { Text(text = stringResource(id = R.string.clear_event)) }
            }
            scheduledEvent?.let { instant ->
                val formatted = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss.SSS").format(instant.atZone(ZoneId.systemDefault()))
                Text(
                    text = stringResource(id = R.string.scheduled_time, formatted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val eventInputFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss")
    .appendLiteral('.')
    .appendValue(ChronoField.MILLI_OF_SECOND, 3)
    .toFormatter()

private fun parseEventTime(input: String, baseInstant: Instant): Instant? {
    return runCatching {
        val time = LocalTime.parse(input, eventInputFormatter)
        val zone = ZoneId.systemDefault()
        val currentDateTime = LocalDateTime.ofInstant(baseInstant, zone)
        var eventDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), time)
        if (!eventDateTime.isAfter(currentDateTime)) {
            eventDateTime = eventDateTime.plusDays(1)
        }
        eventDateTime.atZone(zone).toInstant()
    }.getOrNull()
}

@Composable
private fun SyncTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    timeState: TimeSyncState,
    userPreferences: UserPreferences
) {
    val availableServers = timeState.availableServers
    val syncFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss") }

    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.select_ntp_server), style = MaterialTheme.typography.titleMedium)
                availableServers.forEach { server ->
                    val selected = userPreferences.selectedServer == server
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                        Switch(checked = selected, onCheckedChange = { if (it) viewModel.selectServer(server) })
                    }
                    Divider()
                }
                var customServer by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = customServer,
                    onValueChange = { customServer = it },
                    label = { Text(text = stringResource(id = R.string.custom_server_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (userPreferences.customServers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(id = R.string.custom_servers), style = MaterialTheme.typography.titleSmall)
                        userPreferences.customServers.forEach { server ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = server, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.removeCustomServer(server) }) {
                                    Text(text = stringResource(id = R.string.remove))
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (customServer.isNotBlank()) {
                            viewModel.addCustomServer(customServer)
                            customServer = ""
                        }
                    }) {
                        Text(text = stringResource(id = R.string.add_custom_server))
                    }
                    TextButton(onClick = {
                        viewModel.removeCustomServer(customServer)
                        customServer = ""
                    }, enabled = userPreferences.customServers.contains(customServer)) {
                        Text(text = stringResource(id = R.string.remove))
                    }
                }
            }
        }

        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.auto_sync_enabled), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = userPreferences.autoSyncEnabled, onCheckedChange = viewModel::setAutoSync)
                }
                Text(text = stringResource(id = R.string.auto_sync_interval), style = MaterialTheme.typography.bodyMedium)
                var sliderValue by remember(userPreferences.syncIntervalMinutes) { mutableStateOf(userPreferences.syncIntervalMinutes.toFloat()) }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { viewModel.setSyncIntervalMinutes(sliderValue.roundToInt()) },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = "${userPreferences.syncIntervalMinutes} min", style = MaterialTheme.typography.labelLarge)
                Button(onClick = { viewModel.syncNow() }, enabled = !timeState.isSyncing) {
                    Text(text = if (timeState.isSyncing) stringResource(id = R.string.syncing) else stringResource(id = R.string.sync_now))
                }
                Text(
                    text = timeState.lastSyncInstant?.let { stringResource(id = R.string.last_synced, syncFormatter.format(it.atZone(ZoneId.systemDefault()))) }
                        ?: stringResource(id = R.string.waiting_for_sync),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                timeState.nextSyncAtMillis?.let { next ->
                    val diffMinutes = ((next - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                    Text(text = stringResource(id = R.string.next_sync, diffMinutes.toInt()), style = MaterialTheme.typography.bodySmall)
                }
                timeState.errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizationTab(
    modifier: Modifier,
    viewModel: MainViewModel,
    overlayState: FloatingOverlayUiState,
    style: FloatingClockStyle
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(id = R.string.clock_style_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(id = R.string.font_scale), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = style.fontScale,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(fontScale = value.coerceIn(0.6f, 1.6f)) } },
                    valueRange = 0.6f..1.6f
                )
                Text(text = String.format("%.2f", style.fontScale), style = MaterialTheme.typography.labelLarge)

                Text(text = stringResource(id = R.string.corner_radius), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = style.cornerRadiusDp,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(cornerRadiusDp = value.coerceIn(8f, 48f)) } },
                    valueRange = 8f..48f
                )
                Text(text = "${style.cornerRadiusDp.roundToInt()} dp", style = MaterialTheme.typography.labelLarge)

                Text(text = stringResource(id = R.string.opacity), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = style.backgroundOpacity,
                    onValueChange = { value -> viewModel.updateStyle { it.copy(backgroundOpacity = value.coerceIn(0.4f, 1f)) } },
                    valueRange = 0.4f..1f
                )
                Text(text = "${(style.backgroundOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.use_dynamic_color), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = style.useDynamicColor, onCheckedChange = { enabled ->
                        viewModel.updateStyle { it.copy(useDynamicColor = enabled, accentColor = if (enabled) null else it.accentColor) }
                    })
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.show_seconds), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = style.showSeconds, onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showSeconds = enabled) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.show_millis), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = style.showMillis, onCheckedChange = { enabled -> viewModel.updateStyle { it.copy(showMillis = enabled) } })
                }

                if (!style.useDynamicColor) {
                    Text(text = stringResource(id = R.string.accent_color), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val palette = listOf(0xFF3DDC84, 0xFF3A7BD5, 0xFFFF7043, 0xFF7E57C2, 0xFFFFC107)
                        palette.forEach { colorLong ->
                            val selected = style.accentColor == colorLong
                            Card(
                                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(colorLong)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(40.dp),
                                shape = MaterialTheme.shapes.small,
                                onClick = { viewModel.updateStyle { it.copy(accentColor = colorLong) } }
                            ) {
                                if (selected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(text = stringResource(id = R.string.live_preview), style = MaterialTheme.typography.titleMedium)
        FloatingOverlaySurface(
            state = overlayState.copy(style = style, isVisible = true),
            onClose = {},
            onDrag = { _, _ -> }
        )
    }
}

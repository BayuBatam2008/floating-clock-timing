package com.floatingclock.timing.ui.events

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.floatingclock.timing.data.model.Event
import com.floatingclock.timing.FloatingClockApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    eventViewModel: EventViewModel = viewModel(
        factory = EventViewModel.Factory(
            (LocalContext.current.applicationContext as FloatingClockApplication).eventRepository
        )
    )
) {
    val events by eventViewModel.events.collectAsState(initial = emptyList())
    val showEventDialog by eventViewModel.showEventDialog.collectAsState()
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedEvents by remember { mutableStateOf(setOf<String>()) }
    
    Scaffold(
        floatingActionButton = {
            EventFabMenu(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedEvents.size,
                onAddEvent = { eventViewModel.showCreateEventDialog() },
                onSelectMode = { 
                    isSelectionMode = true 
                    selectedEvents = emptySet()
                },
                onDeleteSelected = { 
                    selectedEvents.forEach { eventId ->
                        eventViewModel.deleteEvent(eventId)
                    }
                    selectedEvents = emptySet()
                    isSelectionMode = false
                },
                onCancel = { 
                    isSelectionMode = false 
                    selectedEvents = emptySet()
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (events.isEmpty()) {
            EmptyEventsState(
                onCreateEvent = { eventViewModel.showCreateEventDialog() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedEvents.contains(event.id),
                        onToggleEnabled = { eventViewModel.toggleEventEnabled(event.id) },
                        onEdit = { eventViewModel.showEditEventDialog(event) },
                        onDelete = { eventViewModel.deleteEvent(event.id) },
                        onSelect = { 
                            selectedEvents = if (selectedEvents.contains(event.id)) {
                                selectedEvents - event.id
                            } else {
                                selectedEvents + event.id
                            }
                        },
                        onLongPress = {
                            isSelectionMode = true
                            selectedEvents = setOf(event.id)
                        }
                    )
                }
                
                // Add some bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
    
    // Event creation/edit modal
    if (showEventDialog) {
        EventEditModal(
            eventViewModel = eventViewModel,
            onDismiss = { eventViewModel.hideEventDialog() }
        )
    }
}

@Composable
private fun EmptyEventsState(
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No events yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first event to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateEvent,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Event")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EventCard(
    event: Event,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    if (isSelectionMode) {
                        onSelect()
                    } else {
                        onEdit()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) onLongPress()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                event.isEnabled -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Time display in HH:mm:ss.SSS format
                Text(
                    text = event.getFormattedTime(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = if (event.isEnabled) FontWeight.Bold else FontWeight.Light
                    ),
                    color = if (event.isEnabled) 
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Event name
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (event.isEnabled) 
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Time until event (if upcoming)
                if (event.isEnabled && event.isUpcoming()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.getTimeUntilEvent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (event.isEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Description (if present)
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (event.isEnabled)
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Enable/disable switch
            Switch(
                checked = event.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun EventFabMenu(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onAddEvent: () -> Unit,
    onSelectMode: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCancel: () -> Unit
) {
    var fabExpanded by remember { mutableStateOf(false) }
    
    if (isSelectionMode) {
        // Selection mode FABs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel selection button
            SmallFloatingActionButton(
                onClick = onCancel,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel selection"
                )
            }
            
            // Delete selected button
            if (selectedCount > 0) {
                SmallFloatingActionButton(
                    onClick = onDeleteSelected,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected ($selectedCount)"
                    )
                }
            }
        }
    } else {
        // Normal mode FAB Menu
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Extended FAB options (show when expanded)
            if (fabExpanded) {
                // Add Event FAB
                SmallFloatingActionButton(
                    onClick = {
                        onAddEvent()
                        fabExpanded = false
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add event"
                    )
                }
                
                // Select Mode FAB
                SmallFloatingActionButton(
                    onClick = {
                        onSelectMode()
                        fabExpanded = false
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select events"
                    )
                }
            }
            
            // Main FAB with pencil icon
            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Menu"
                )
            }
        }
    }
}
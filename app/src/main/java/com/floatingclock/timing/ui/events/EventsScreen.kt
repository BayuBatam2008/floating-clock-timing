package com.floatingclock.timing.ui.events

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    ),
    isSelectionMode: Boolean = false,
    selectedEvents: Set<String> = emptySet(),
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSelectedEventsChange: (Set<String>) -> Unit = {}
) {
    val events by eventViewModel.events.collectAsState(initial = emptyList())
    val showEventDialog by eventViewModel.showEventDialog.collectAsState()
    // Error handling is now managed globally in FloatingClockApp
    
    // Use external selection state
    var internalSelectionMode by remember { mutableStateOf(isSelectionMode) }
    var internalSelectedEvents by remember { mutableStateOf(selectedEvents) }
    
    // Sync external state changes
    LaunchedEffect(isSelectionMode) {
        internalSelectionMode = isSelectionMode
    }
    
    LaunchedEffect(selectedEvents) {
        internalSelectedEvents = selectedEvents
    }
    
    // Auto-exit selection mode when no items are selected
    LaunchedEffect(internalSelectedEvents) {
        if (internalSelectionMode && internalSelectedEvents.isEmpty()) {
            internalSelectionMode = false
            onSelectionModeChange(false)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (events.isEmpty()) {
            EmptyEventsState(
                onCreateEvent = { eventViewModel.showCreateEventDialog() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        isSelectionMode = internalSelectionMode,
                        isSelected = internalSelectedEvents.contains(event.id),
                        onToggleEnabled = { eventViewModel.toggleEventEnabled(event.id) },
                        onEdit = { eventViewModel.showEditEventDialog(event) },
                        onDelete = { eventViewModel.deleteEvent(event.id) },
                        onSelect = { 
                            val newSelectedEvents = if (internalSelectedEvents.contains(event.id)) {
                                internalSelectedEvents - event.id
                            } else {
                                internalSelectedEvents + event.id
                            }
                            internalSelectedEvents = newSelectedEvents
                            onSelectedEventsChange(newSelectedEvents)
                        },
                        onLongPress = {
                            internalSelectionMode = true
                            val newSelectedEvents = setOf(event.id)
                            internalSelectedEvents = newSelectedEvents
                            onSelectionModeChange(true)
                            onSelectedEventsChange(newSelectedEvents)
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
        // Empty state illustration
        Icon(
            painter = androidx.compose.ui.res.painterResource(
                id = com.floatingclock.timing.R.drawable.empty_events_illustration
            ),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection mode checkbox on the left
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInHorizontally(animationSpec = tween(300)) + fadeIn(),
                exit = slideOutHorizontally(animationSpec = tween(300)) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            
            // Event details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Time display in HH:mm:ss.SSS format
                Text(
                    text = event.getFormattedTime(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 28.sp,
                        fontWeight = if (event.isEnabled) FontWeight.Bold else FontWeight.Light
                    ),
                    color = if (event.isEnabled) 
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Event date
                Text(
                    text = event.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (event.isEnabled) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Event name
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (event.isEnabled) 
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
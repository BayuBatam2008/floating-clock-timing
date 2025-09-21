package com.floatingclock.timing.ui.events

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditModal(
    eventViewModel: EventViewModel,
    onDismiss: () -> Unit
) {
    // Collect individual states instead of uiState
    val eventName by eventViewModel.eventName.collectAsState()
    val selectedDate by eventViewModel.selectedDate.collectAsState()
    val soundEnabled by eventViewModel.soundEnabled.collectAsState()
    val vibrationEnabled by eventViewModel.vibrationEnabled.collectAsState()
    val timeInput by eventViewModel.currentTimeInput.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Event",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Event Name Input
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventViewModel.updateEventName(it) },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            Card(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedDate.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Input Section with EXACT COPY from FloatingClockApp
            TimeInputScheduleSection(
                selectedDate = selectedDate,
                currentTimeInput = timeInput,
                eventViewModel = eventViewModel
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sound and Vibration Settings
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Alert Settings",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { eventViewModel.updateSoundEnabled(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibration",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { eventViewModel.updateVibrationEnabled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button with proper spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        eventViewModel.saveEvent()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Save Event")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24L * 60L * 60L * 1000L
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24L * 60L * 60L * 1000L))
                            eventViewModel.updateSelectedDate(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimeInputScheduleSection(
    selectedDate: LocalDate,
    currentTimeInput: String,
    eventViewModel: EventViewModel,
    modifier: Modifier = Modifier
) {
    // State for absolute time input (like alarm clock) - parse from currentTimeInput
    var selectedSegment by rememberSaveable { mutableStateOf(0) } // 0=hours, 1=minutes, 2=seconds, 3=millis
    
    // Parse current time input
    val paddedInput = currentTimeInput.padStart(9, '0')
    var hours by rememberSaveable(paddedInput) { mutableStateOf(paddedInput.substring(0, 2).toIntOrNull() ?: 0) }
    var minutes by rememberSaveable(paddedInput) { mutableStateOf(paddedInput.substring(2, 4).toIntOrNull() ?: 0) }
    var seconds by rememberSaveable(paddedInput) { mutableStateOf(paddedInput.substring(4, 6).toIntOrNull() ?: 0) }
    var millis by rememberSaveable(paddedInput) { mutableStateOf(paddedInput.substring(6, 9).toIntOrNull() ?: 0) }
    
    // Update eventViewModel when values change
    LaunchedEffect(hours, minutes, seconds, millis) {
        val timeString = "%02d%02d%02d%03d".format(hours, minutes, seconds, millis)
        eventViewModel.updateTimeInput(timeString)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Target Time", style = MaterialTheme.typography.titleMedium)

            // Interactive Time Display with highlighting - EXACT COPY
            EventInteractiveTimeDisplay(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                millis = millis,
                selectedSegment = selectedSegment,
                onSegmentClick = { segment -> selectedSegment = segment }
            )

            // Quick time buttons (set to common times)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickTimes = remember { 
                    listOf(
                        Triple(6, 0, "6:00"),
                        Triple(7, 30, "7:30"),
                        Triple(12, 0, "12:00"),
                        Triple(18, 0, "18:00")
                    )
                }
                quickTimes.forEach { (h, m, label) ->
                    AssistChip(
                        onClick = {
                            hours = h
                            minutes = m
                            seconds = 0
                            millis = 0
                        },
                        label = { Text(text = label) }
                    )
                }
            }

            // Interactive Keypad for editing selected segment - EXACT COPY
            EventInteractiveKeypad(
                selectedSegment = selectedSegment,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                millis = millis,
                onHoursChange = { hours = it.coerceIn(0, 23) },
                onMinutesChange = { minutes = it.coerceIn(0, 59) },
                onSecondsChange = { seconds = it.coerceIn(0, 59) },
                onMillisChange = { millis = it.coerceIn(0, 999) }
            )
        }
    }
}

// EXACT COPY of InteractiveTimeDisplay from FloatingClockApp
@Composable
private fun EventInteractiveTimeDisplay(
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
        EventInteractiveTimeSegment(
            value = "%02d".format(hours),
            label = "h",
            isSelected = selectedSegment == 0,
            onClick = { onSegmentClick(0) }
        )
        
        // Minutes segment (1)
        EventInteractiveTimeSegment(
            value = "%02d".format(minutes),
            label = "m",
            isSelected = selectedSegment == 1,
            onClick = { onSegmentClick(1) }
        )
        
        // Seconds segment (2)
        EventInteractiveTimeSegment(
            value = "%02d".format(seconds),
            label = "s",
            isSelected = selectedSegment == 2,
            onClick = { onSegmentClick(2) }
        )
        
        // Milliseconds segment (3)
        EventInteractiveTimeSegment(
            value = "%03d".format(millis),
            label = "ms",
            isSelected = selectedSegment == 3,
            onClick = { onSegmentClick(3) }
        )
    }
}

// EXACT COPY of InteractiveTimeSegment from FloatingClockApp
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EventInteractiveTimeSegment(
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

// EXACT COPY of InteractiveKeypad from FloatingClockApp  
@Composable
private fun EventInteractiveKeypad(
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
        // Display current editing segment info
        Text(
            text = when (selectedSegment) {
                0 -> "Editing Hours (0-23)"
                1 -> "Editing Minutes (0-59)"
                2 -> "Editing Seconds (0-59)"
                3 -> "Editing Milliseconds (0-999)"
                else -> "Select a time segment"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        keypadRows.forEachIndexed { rowIndex, row ->
            key(rowIndex) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { keyIndex, key ->
                        key("$rowIndex-$keyIndex") {
                            EventInteractiveKeypadButton(
                                modifier = Modifier.weight(1f),
                                label = key,
                                onClick = {
                                    when (key) {
                                        "⌫" -> {
                                            if (editingValue.isNotEmpty()) {
                                                val newValue = editingValue.dropLast(1)
                                                updateSelectedSegment(if (newValue.isEmpty()) "0" else newValue)
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

// EXACT COPY of InteractiveKeypadButton from FloatingClockApp
@Composable
private fun EventInteractiveKeypadButton(
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
package com.floatingclock.timing.ui.events

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditModal(
    eventViewModel: EventViewModel,
    onDismiss: () -> Unit
) {
    // Collect individual states instead of uiState
    val eventName by eventViewModel.eventName.collectAsState()
    val selectedDate by eventViewModel.selectedDate.collectAsState()
    val timeInput by eventViewModel.currentTimeInput.collectAsState()
    val editingEvent by eventViewModel.editingEvent.collectAsState()
    val errorMessage by eventViewModel.errorMessage.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    
    // Snackbar setup
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar when error message is set
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            eventViewModel.clearErrorMessage()
        }
    }
    
    // Use Material 3 ModalBottomSheet with built-in animations
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false // Allow dragging
    )
    
    Box {
        ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            // Header with save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingEvent != null) "Edit Event" else "New Event",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                FloatingActionButton(
                    onClick = { 
                        eventViewModel.saveEvent { success ->
                            if (success) {
                                onDismiss()
                            }
                            // If failed, do nothing - snackbar will show and modal stays open
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save Event"
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
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
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
        }
    }
        
        // SnackbarHost positioned at the bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val today = LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24L * 60L * 60L * 1000L,
            yearRange = today.year..today.year + 1, // Limit to current year and next year
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = LocalDate.ofEpochDay(utcTimeMillis / (24L * 60L * 60L * 1000L))
                    return !date.isBefore(today) // Only allow today and future dates
                }
            }
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
    
    // Parse current time input - remove rememberSaveable to avoid conflicts
    val paddedInput = currentTimeInput.padStart(9, '0')
    val hours = paddedInput.substring(0, 2).toIntOrNull() ?: 0
    val minutes = paddedInput.substring(2, 4).toIntOrNull() ?: 0
    val seconds = paddedInput.substring(4, 6).toIntOrNull() ?: 0
    val millis = paddedInput.substring(6, 9).toIntOrNull() ?: 0

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
                onSegmentClick = { segment -> 
                    selectedSegment = segment 
                }
            )

            // Interactive Keypad for editing selected segment - Updated to use ViewModel directly
            EventInteractiveKeypad(
                selectedSegment = selectedSegment,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                millis = millis,
                onHoursChange = { newHours -> 
                    val timeString = "%02d%02d%02d%03d".format(newHours.coerceIn(0, 23), minutes, seconds, millis)
                    eventViewModel.updateTimeInput(timeString)
                },
                onMinutesChange = { newMinutes -> 
                    val timeString = "%02d%02d%02d%03d".format(hours, newMinutes.coerceIn(0, 59), seconds, millis)
                    eventViewModel.updateTimeInput(timeString)
                },
                onSecondsChange = { newSeconds -> 
                    val timeString = "%02d%02d%02d%03d".format(hours, minutes, newSeconds.coerceIn(0, 59), millis)
                    eventViewModel.updateTimeInput(timeString)
                },
                onMillisChange = { newMillis -> 
                    val timeString = "%02d%02d%02d%03d".format(hours, minutes, seconds, newMillis.coerceIn(0, 999))
                    eventViewModel.updateTimeInput(timeString)
                },
                onResetAll = {
                    eventViewModel.updateTimeInput("000000000")
                }
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
@OptIn(ExperimentalMaterial3Api::class)
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
    onMillisChange: (Int) -> Unit,
    onResetAll: () -> Unit = {}
) {
    // Function to handle digit input for the selected segment
    fun handleDigitInput(digit: String) {
        val currentValue = when (selectedSegment) {
            0 -> hours
            1 -> minutes
            2 -> seconds
            3 -> millis
            else -> 0
        }
        
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
        
        // Calculate new value
        val currentStr = currentValue.toString()
        val newValueStr = if (currentValue == 0) {
            digit
        } else {
            val candidate = (currentStr + digit).take(maxLength)
            val candidateInt = candidate.toIntOrNull() ?: 0
            if (candidateInt <= maxValue) {
                candidate
            } else {
                currentStr // Keep old value if exceeds max
            }
        }
        
        val newValue = newValueStr.toIntOrNull() ?: 0
        
        // Call appropriate callback
        when (selectedSegment) {
            0 -> onHoursChange(newValue.coerceIn(0, 23))
            1 -> onMinutesChange(newValue.coerceIn(0, 59))
            2 -> onSecondsChange(newValue.coerceIn(0, 59))
            3 -> onMillisChange(newValue.coerceIn(0, 999))
        }
    }
    
    fun handleBackspace() {
        val currentValue = when (selectedSegment) {
            0 -> hours
            1 -> minutes
            2 -> seconds
            3 -> millis
            else -> 0
        }
        
        val currentStr = currentValue.toString()
        val newValueStr = if (currentStr.length > 1) {
            currentStr.dropLast(1)
        } else {
            "0"
        }
        
        val newValue = newValueStr.toIntOrNull() ?: 0
        
        when (selectedSegment) {
            0 -> onHoursChange(newValue.coerceIn(0, 23))
            1 -> onMinutesChange(newValue.coerceIn(0, 59))
            2 -> onSecondsChange(newValue.coerceIn(0, 59))
            3 -> onMillisChange(newValue.coerceIn(0, 999))
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
                            EventInteractiveKeypadButton(
                                modifier = Modifier.weight(1f),
                                label = key,
                                onClick = {
                                    when (key) {
                                        "Reset" -> onResetAll()
                                        "⌫" -> handleBackspace()
                                        else -> handleDigitInput(key)
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
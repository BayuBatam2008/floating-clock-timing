package com.floatingclock.timing.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.floatingclock.timing.data.EventRepository
import com.floatingclock.timing.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EventViewModel(private val eventRepository: EventRepository) : ViewModel() {
    
    val events = eventRepository.events
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showEventDialog = MutableStateFlow(false)
    val showEventDialog: StateFlow<Boolean> = _showEventDialog.asStateFlow()
    
    private val _editingEvent = MutableStateFlow<Event?>(null)
    val editingEvent: StateFlow<Event?> = _editingEvent.asStateFlow()
    
    // Current time input state for new/editing event
    private val _currentTimeInput = MutableStateFlow(getDefaultTimeInput()) // Default to current time + 5 minutes
    val currentTimeInput: StateFlow<String> = _currentTimeInput.asStateFlow()
    
    private fun getDefaultTimeInput(): String {
        val now = LocalTime.now()
        // Round to next minute, then add 5 minutes
        val roundedToNextMinute = now.withSecond(0).withNano(0).plusMinutes(1)
        val defaultTime = roundedToNextMinute.plusMinutes(5)
        return String.format("%02d%02d%02d%03d", 
            defaultTime.hour, defaultTime.minute, defaultTime.second, defaultTime.nano / 1000000)
    }
    
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    private val _eventName = MutableStateFlow("Event")
    val eventName: StateFlow<String> = _eventName.asStateFlow()
    
    private val _eventDescription = MutableStateFlow("")
    val eventDescription: StateFlow<String> = _eventDescription.asStateFlow()
    
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun showCreateEventDialog() {
        resetEventForm()
        _editingEvent.value = null
        _showEventDialog.value = true
    }
    
    fun showEditEventDialog(event: Event) {
        _editingEvent.value = event
        _eventName.value = event.name
        _eventDescription.value = event.description
        _soundEnabled.value = event.soundEnabled
        _vibrationEnabled.value = event.vibrationEnabled
        _selectedDate.value = LocalDate.parse(event.date)
        
        // Convert time format from HH:mm:ss.SSS to input format
        try {
            val time = LocalTime.parse(event.targetTime, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            val formattedTime = String.format("%02d%02d%02d%03d", 
                time.hour, time.minute, time.second, time.nano / 1000000)
            _currentTimeInput.value = formattedTime
        } catch (e: Exception) {
            _currentTimeInput.value = "000000000"
        }
        
        _showEventDialog.value = true
    }
    
    fun hideEventDialog() {
        _showEventDialog.value = false
        _editingEvent.value = null
    }
    
    private fun resetEventForm() {
        _eventName.value = "Event"
        _eventDescription.value = ""
        _soundEnabled.value = true
        _vibrationEnabled.value = true
        _selectedDate.value = LocalDate.now()
        
        // Set default time to next rounded minute + 5 minutes
        val now = LocalTime.now()
        val roundedToNextMinute = now.withSecond(0).withNano(0).plusMinutes(1)
        val defaultTime = roundedToNextMinute.plusMinutes(5)
        val formattedTime = String.format("%02d%02d%02d%03d", 
            defaultTime.hour, defaultTime.minute, defaultTime.second, defaultTime.nano / 1000000)
        _currentTimeInput.value = formattedTime
    }
    
    fun updateEventName(name: String) {
        _eventName.value = name
    }
    
    fun updateEventDescription(description: String) {
        _eventDescription.value = description
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
    }
    
    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    fun updateTimeInput(timeInput: String) {
        _currentTimeInput.value = timeInput
    }
    
    fun appendTimeDigit(digit: String) {
        val current = _currentTimeInput.value
        if (current.length < 9) {
            _currentTimeInput.value = current + digit
        }
    }
    
    fun removeLastTimeDigit() {
        val current = _currentTimeInput.value
        if (current.isNotEmpty()) {
            _currentTimeInput.value = current.dropLast(1)
        }
    }
    
    fun clearTimeInput() {
        _currentTimeInput.value = ""
    }
    
    fun getFormattedTimeFromInput(): String {
        val input = _currentTimeInput.value.padStart(9, '0')
        val hours = input.substring(0, 2)
        val minutes = input.substring(2, 4)
        val seconds = input.substring(4, 6)
        val millis = input.substring(6, 9)
        return "$hours:$minutes:$seconds.$millis"
    }
    
    fun saveEvent(onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val timeString = getFormattedTimeFromInput()
                val date = _selectedDate.value.toString()
                
                // Check if event is scheduled for today but with past time
                val today = LocalDate.now()
                val currentTime = LocalTime.now()
                if (_selectedDate.value.isEqual(today)) {
                    val eventTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                    if (eventTime.isBefore(currentTime)) {
                        _errorMessage.value = "Tidak dapat membuat event dengan waktu yang sudah lewat untuk hari ini!"
                        onResult?.invoke(false)
                        return@launch
                    }
                }
                
                val event = if (_editingEvent.value != null) {
                    _editingEvent.value!!.copy(
                        name = _eventName.value,
                        targetTime = timeString,
                        date = date,
                        description = _eventDescription.value,
                        soundEnabled = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value
                    )
                } else {
                    Event(
                        name = _eventName.value,
                        targetTime = timeString,
                        date = date,
                        description = _eventDescription.value,
                        soundEnabled = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value
                    )
                }
                
                if (_editingEvent.value != null) {
                    // Check for conflicts when updating
                    if (eventRepository.hasConflictingEvent(event.targetTime, event.date, event.id)) {
                        _errorMessage.value = "Event dengan waktu dan tanggal yang sama sudah ada!"
                        onResult?.invoke(false)
                        return@launch
                    }
                    eventRepository.updateEvent(event)
                } else {
                    val success = eventRepository.addEvent(event)
                    if (!success) {
                        // Handle duplicate case - show error message instead of closing
                        _errorMessage.value = "Event dengan waktu dan tanggal yang sama sudah ada!"
                        onResult?.invoke(false)
                        return@launch
                    }
                }
                
                // Success case
                hideEventDialog()
                onResult?.invoke(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult?.invoke(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }
    
    fun toggleEventEnabled(eventId: String) {
        viewModelScope.launch {
            eventRepository.toggleEventEnabled(eventId)
        }
    }
    
    fun getUpcomingEvents(): List<Event> {
        return eventRepository.getUpcomingEvents()
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    class Factory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
                return EventViewModel(eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
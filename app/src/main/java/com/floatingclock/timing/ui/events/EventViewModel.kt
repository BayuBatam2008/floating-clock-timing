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
    private val _currentTimeInput = MutableStateFlow("090000000") // Default 09:00:00.000
    val currentTimeInput: StateFlow<String> = _currentTimeInput.asStateFlow()
    
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
            _currentTimeInput.value = "090000000"
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
        _currentTimeInput.value = "090000000"
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
    
    fun saveEvent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val timeString = getFormattedTimeFromInput()
                
                val event = if (_editingEvent.value != null) {
                    _editingEvent.value!!.copy(
                        name = _eventName.value,
                        targetTime = timeString,
                        date = _selectedDate.value.toString(),
                        description = _eventDescription.value,
                        soundEnabled = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value
                    )
                } else {
                    Event(
                        name = _eventName.value,
                        targetTime = timeString,
                        date = _selectedDate.value.toString(),
                        description = _eventDescription.value,
                        soundEnabled = _soundEnabled.value,
                        vibrationEnabled = _vibrationEnabled.value
                    )
                }
                
                if (_editingEvent.value != null) {
                    eventRepository.updateEvent(event)
                } else {
                    eventRepository.addEvent(event)
                }
                
                hideEventDialog()
            } catch (e: Exception) {
                e.printStackTrace()
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
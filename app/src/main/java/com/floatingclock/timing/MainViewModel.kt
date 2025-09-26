package com.floatingclock.timing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.floatingclock.timing.data.AppDependencies
import com.floatingclock.timing.data.PreferencesRepository
import com.floatingclock.timing.data.TimeSyncManager
import com.floatingclock.timing.data.model.FloatingClockStyle
import com.floatingclock.timing.data.model.UserPreferences
import com.floatingclock.timing.overlay.FloatingClockController
import com.floatingclock.timing.overlay.FloatingOverlayUiState
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository: PreferencesRepository = AppDependencies.preferencesRepository
    private val timeSyncManager: TimeSyncManager = AppDependencies.timeSyncManager
    private val floatingClockController: FloatingClockController = AppDependencies.floatingClockController

    val timeState: StateFlow<com.floatingclock.timing.data.TimeSyncState> = timeSyncManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, timeSyncManager.state.value)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, run {
            val defaultStyle = FloatingClockStyle()
            UserPreferences(
                selectedServer = com.floatingclock.timing.data.DEFAULT_SERVERS.first(),
                customServers = emptyList(),
                autoSyncEnabled = true,
                syncIntervalMinutes = 15,
                floatingClockStyle = defaultStyle
            )
        })

    val overlayState: StateFlow<FloatingOverlayUiState> = floatingClockController.overlayState
        .stateIn(viewModelScope, SharingStarted.Eagerly, floatingClockController.overlayState.value)

    fun hideOverlay() {
        floatingClockController.hideOverlay(getApplication())
    }

    fun clearEvent() {
        floatingClockController.clearEvent()
    }

    fun syncNow() {
        viewModelScope.launch {
            timeSyncManager.syncNow()
        }
    }

    fun selectServer(server: String) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedServer(server)
            timeSyncManager.syncNow(server)
        }
    }

    fun addCustomServer(server: String) {
        viewModelScope.launch {
            preferencesRepository.addCustomServer(server)
        }
    }

    fun removeCustomServer(server: String) {
        viewModelScope.launch {
            preferencesRepository.removeCustomServer(server)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoSyncEnabled(enabled)
        }
    }

    fun setSyncIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateSyncIntervalMinutes(minutes)
        }
    }

    fun updateStyle(transform: (FloatingClockStyle) -> FloatingClockStyle) {
        viewModelScope.launch {
            val current = userPreferences.value.floatingClockStyle
            preferencesRepository.updateStyle(transform(current))
        }
    }

    fun isOverlayActive(): Boolean = floatingClockController.isOverlayActive()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[AndroidViewModelFactory.APPLICATION_KEY] as? Application
                    ?: throw IllegalStateException("Application is not available in extras")
                return MainViewModel(app) as T
            }
        }
    }
}

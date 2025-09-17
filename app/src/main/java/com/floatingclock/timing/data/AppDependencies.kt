package com.floatingclock.timing.data

import android.app.Application
import android.content.Context
import com.floatingclock.timing.overlay.FloatingClockController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object AppDependencies {
    private lateinit var appContext: Context
    private var initialized = false

    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var timeSyncManager: TimeSyncManager
        private set
    lateinit var floatingClockController: FloatingClockController
        private set
    private lateinit var ntpClient: NtpClient
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun initialize(application: Application) {
        if (initialized) return
        appContext = application
        ntpClient = NtpClient()
        preferencesRepository = PreferencesRepository(appContext)
        timeSyncManager = TimeSyncManager(preferencesRepository, ntpClient, ioDispatcher)
        floatingClockController = FloatingClockController(appContext, timeSyncManager, preferencesRepository)
        initialized = true
    }
}

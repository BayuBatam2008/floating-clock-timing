package com.floatingclock.timing

import android.app.Application
import com.floatingclock.timing.data.AppDependencies
import com.floatingclock.timing.data.EventRepository

class FloatingClockApplication : Application() {
    
    lateinit var eventRepository: EventRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        AppDependencies.initialize(this)
        eventRepository = EventRepository(this)
    }
}

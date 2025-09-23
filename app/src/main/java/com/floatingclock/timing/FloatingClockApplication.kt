package com.floatingclock.timing

import android.app.Application
import com.floatingclock.timing.data.AppDependencies
import com.floatingclock.timing.data.EventRepository

class FloatingClockApplication : Application() {
    
    val eventRepository: EventRepository
        get() = AppDependencies.eventRepository
    
    override fun onCreate() {
        super.onCreate()
        AppDependencies.initialize(this)
    }
}

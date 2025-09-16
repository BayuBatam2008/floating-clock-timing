package com.floatingclock.timing

import android.app.Application
import com.floatingclock.timing.data.AppDependencies

class FloatingClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDependencies.initialize(this)
    }
}

package com.floatingclock.timing.data.model

data class UserPreferences(
    val selectedServer: String,
    val customServers: List<String>,
    val autoSyncEnabled: Boolean,
    val syncIntervalMinutes: Int,
    val floatingClockStyle: FloatingClockStyle
)

package com.floatingclock.timing.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EventNotificationManager.ACTION_DISMISS) {
            val notificationManager = EventNotificationManager(context)
            notificationManager.dismissEventReminder()
        }
    }
}
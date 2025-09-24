package com.floatingclock.timing.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.floatingclock.timing.MainActivity
import com.floatingclock.timing.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "com.floatingclock.timing.DISMISS_REMINDER"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Event Reminders"
            val descriptionText = "Notifications for upcoming events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                enableVibration(true)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showEventReminder(eventName: String, targetTime: LocalDateTime) {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = targetTime.format(timeFormatter)
        
        android.util.Log.d("EventNotificationManager", "Showing notification for: $eventName at $formattedTime")
        
        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            android.util.Log.w("EventNotificationManager", "Notifications are disabled!")
            return
        }
        
        // Intent to open the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to dismiss the notification
        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🕒 Event Reminder")
                .setContentText("$eventName will trigger at $formattedTime")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Event '$eventName' will trigger soon at $formattedTime. Get ready!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(false) // Persistent notification
                .setOngoing(false) // Allow dismissal by swipe
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_notification, "Dismiss", dismissPendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            android.util.Log.i("EventNotificationManager", "Notification sent successfully for: $eventName")
        } catch (e: Exception) {
            android.util.Log.e("EventNotificationManager", "Error showing notification", e)
        }
    }
    
    fun dismissEventReminder() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
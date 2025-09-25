package com.floatingclock.timing.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.floatingclock.timing.MainActivity
import com.floatingclock.timing.R

/**
 * Efficient notification manager yang menggunakan AlarmManager
 * untuk scheduling tepat tanpa perlu polling/checking setiap 30 detik.
 * JAUH LEBIH HEMAT RESOURCE!
 */
class EfficientNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "floating_clock_events"
        const val CHANNEL_NAME = "Event Notifications"
        const val NOTIFICATION_ID = 1001
        const val ALARM_REQUEST_CODE = 2002
        
        const val EXTRA_EVENT_NAME = "event_name"
        const val EXTRA_EVENT_TIME = "event_time"
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Schedule notification 5 MENIT SEBELUM event - HANYA SATU ALARM!
     * Resource-efficient approach untuk remind user 5 menit sebelum event.
     */
    fun scheduleEventNotification(eventName: String, eventTimeMillis: Long) {
        val now = System.currentTimeMillis()
        val fiveMinutesInMillis = 5 * 60 * 1000L // 5 minutes in milliseconds
        val notificationTime = eventTimeMillis - fiveMinutesInMillis // 5 minutes BEFORE event
        val timeUntilNotification = notificationTime - now
        
        val eventTimeFormatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(eventTimeMillis))
        val notificationTimeFormatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(notificationTime))
        
        android.util.Log.d("EfficientNotif", "⏰ Scheduling 5-MINUTE REMINDER notification:")
        android.util.Log.d("EfficientNotif", "   📝 Event: $eventName")
        android.util.Log.d("EfficientNotif", "   🎯 Event Time: $eventTimeFormatted")
        android.util.Log.d("EfficientNotif", "   🔔 Notification Time: $notificationTimeFormatted")
        android.util.Log.d("EfficientNotif", "   ⏳ Notification in: ${timeUntilNotification / 1000}s (${timeUntilNotification / 60000} minutes)")
        
        // Cancel existing alarm first
        cancelScheduledNotification()
        
        // Don't schedule if notification time is in the past
        if (timeUntilNotification <= 0) {
            android.util.Log.w("EfficientNotif", "⚠️ 5-minute reminder time has passed - event too close or in past")
            return
        }
        
        // Create intent untuk alarm receiver
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_NAME, eventName)
            putExtra(EXTRA_EVENT_TIME, eventTimeMillis)
            putExtra("notification_time", notificationTime)
            putExtra("scheduled_at", now)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule exact alarm - 5 MENIT SEBELUM EVENT!
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                )
                android.util.Log.i("EfficientNotif", "✅ 5-minute reminder alarm scheduled successfully")
                android.util.Log.i("EfficientNotif", "   🔔 Will remind in ${timeUntilNotification / 60000} minutes")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                )
                android.util.Log.i("EfficientNotif", "✅ 5-minute reminder alarm scheduled (older Android)")
            }
        } catch (e: SecurityException) {
            android.util.Log.w("EfficientNotif", "❌ Exact alarm permission denied, using regular alarm: ${e.message}")
            // Fallback to regular alarm if exact alarm permission denied
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
            android.util.Log.i("EfficientNotif", "✅ Fallback 5-minute reminder scheduled")
        } catch (e: Exception) {
            android.util.Log.e("EfficientNotif", "❌ Failed to schedule 5-minute reminder: ${e.message}")
        }
    }
    
    /**
     * Cancel scheduled notification
     */
    fun cancelScheduledNotification() {
        val intent = Intent(context, EventAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    /**
     * Show 5-minute reminder notification - HANYA INI YANG AKAN MUNCUL!
     */
    fun showFiveMinuteReminder(eventName: String, eventTimeMillis: Long) {
        android.util.Log.d("EfficientNotif", "⏰ Showing 5-minute reminder for: $eventName")
        
        // Check notification permission first
        if (!notificationManager.areNotificationsEnabled()) {
            android.util.Log.w("EfficientNotif", "❌ Notifications are disabled!")
            return
        }
        
        val eventTimeFormatted = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(eventTimeMillis))
        
        val title = "⏰ 5-Minute Reminder"
        val message = "$eventName starts in 5 minutes"
        val fullMessage = "$message (at $eventTimeFormatted)"
        
        // Create intent to open main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("event_name", eventName)
            putExtra("event_time", eventTimeMillis)
            putExtra("is_reminder", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(fullMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
            
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            android.util.Log.i("EfficientNotif", "✅ 5-minute reminder notification sent successfully")
            android.util.Log.i("EfficientNotif", "   📱 Title: $title")
            android.util.Log.i("EfficientNotif", "   💬 Message: $fullMessage")
        } catch (e: SecurityException) {
            android.util.Log.e("EfficientNotif", "❌ Notification permission denied: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("EfficientNotif", "❌ Failed to show 5-minute reminder: ${e.message}")
        }
    }
    
    /**
     * Show immediate notification (deprecated - not used for 5-minute reminder system)
     */
    @Deprecated("Use showFiveMinuteReminder instead")
    fun showEventNotification(eventName: String, eventTimeMillis: Long) {
        android.util.Log.w("EfficientNotif", "⚠️ showEventNotification called - this is deprecated for 5-minute reminder system")
        // Do nothing - we only want 5-minute reminders
    }
    

    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "5-minute reminder notifications for floating clock events"
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

/**
 * BroadcastReceiver untuk handle alarm - trigger 5 MENIT SEBELUM event!
 * Ini yang akan dipanggil 5 menit sebelum event time oleh AlarmManager.
 */
class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        val eventName = intent.getStringExtra(EfficientNotificationManager.EXTRA_EVENT_NAME) ?: "Event"
        val eventTime = intent.getLongExtra(EfficientNotificationManager.EXTRA_EVENT_TIME, 0L)
        val notificationTime = intent.getLongExtra("notification_time", 0L)
        val scheduledAt = intent.getLongExtra("scheduled_at", 0L)
        
        val actualDelay = now - notificationTime // Delay from expected notification time
        val minutesUntilEvent = (eventTime - now) / (60 * 1000L) // Minutes until actual event
        
        android.util.Log.i("EventAlarmReceiver", "⏰ 5-MINUTE REMINDER ALARM TRIGGERED!")
        android.util.Log.i("EventAlarmReceiver", "   📝 Event: $eventName")
        android.util.Log.i("EventAlarmReceiver", "   🎯 Event Time: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(eventTime))}")
        android.util.Log.i("EventAlarmReceiver", "   🔔 Notification Time: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(notificationTime))}")
        android.util.Log.i("EventAlarmReceiver", "   ⏳ Minutes until event: $minutesUntilEvent")
        android.util.Log.i("EventAlarmReceiver", "   ⚡ Notification delay: ${actualDelay}ms")
        
        // Show 5-minute reminder notification
        val notificationManager = EfficientNotificationManager(context)
        notificationManager.showFiveMinuteReminder(eventName, eventTime)
        
        android.util.Log.i("EventAlarmReceiver", "✅ 5-minute reminder notification sent successfully")
    }
}
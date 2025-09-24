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
     * Schedule notification untuk event tertentu - HANYA SATU ALARM, BUKAN POLLING!
     */
    fun scheduleEventNotification(eventName: String, eventTimeMillis: Long) {
        // Cancel existing alarm first
        cancelScheduledNotification()
        
        // Create intent untuk alarm receiver
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_NAME, eventName)
            putExtra(EXTRA_EVENT_TIME, eventTimeMillis)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule exact alarm - HANYA SEKALI DI WAKTU EVENT!
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    eventTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    eventTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to regular alarm if exact alarm permission denied
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                eventTimeMillis,
                pendingIntent
            )
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
     * Show immediate notification
     */
    fun showEventNotification(eventName: String, eventTimeMillis: Long) {
        val now = System.currentTimeMillis()
        val isEventTime = kotlin.math.abs(eventTimeMillis - now) <= 5000L // 5 second tolerance
        
        val title = if (isEventTime) {
            "🎯 Event Time!"
        } else {
            "⏰ Upcoming Event"
        }
        
        val message = "$eventName is ${if (isEventTime) "now" else "scheduled"}"
        
        // Create intent to open main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle notification permission denied
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for floating clock events"
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

/**
 * BroadcastReceiver untuk handle alarm - hanya jalan saat alarm trigger!
 */
class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getStringExtra(EfficientNotificationManager.EXTRA_EVENT_NAME) ?: "Event"
        val eventTime = intent.getLongExtra(EfficientNotificationManager.EXTRA_EVENT_TIME, 0L)
        
        // Show notification
        val notificationManager = EfficientNotificationManager(context)
        notificationManager.showEventNotification(eventName, eventTime)
    }
}
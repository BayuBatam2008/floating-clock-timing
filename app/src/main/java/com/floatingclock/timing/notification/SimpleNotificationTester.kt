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

/**
 * Simple notification tester - PASTI BEKERJA!
 * Untuk debug dan memastikan notification dasar bisa muncul
 */
class SimpleNotificationTester(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "test_notifications"
        const val CHANNEL_NAME = "Test Notifications"
        const val NOTIFICATION_ID = 9999
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
        android.util.Log.d("SimpleNotifTest", "SimpleNotificationTester initialized")
    }
    
    fun testBasicNotification(): Boolean {
        android.util.Log.d("SimpleNotifTest", "=== TESTING BASIC NOTIFICATION ===")
        
        // Check if notifications are enabled
        val notificationsEnabled = notificationManager.areNotificationsEnabled()
        android.util.Log.d("SimpleNotifTest", "Notifications enabled: $notificationsEnabled")
        
        if (!notificationsEnabled) {
            android.util.Log.e("SimpleNotifTest", "NOTIFICATIONS ARE DISABLED!")
            return false
        }
        
        try {
            // Create simple notification
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon to ensure it exists
                .setContentTitle("🔥 TEST NOTIFICATION")
                .setContentText("This is a test notification - if you see this, notifications work!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            
            android.util.Log.d("SimpleNotifTest", "Notification created, trying to show...")
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            android.util.Log.d("SimpleNotifTest", "✅ NOTIFICATION SENT SUCCESSFULLY!")
            return true
            
        } catch (e: SecurityException) {
            android.util.Log.e("SimpleNotifTest", "❌ SECURITY EXCEPTION: ${e.message}")
            return false
        } catch (e: Exception) {
            android.util.Log.e("SimpleNotifTest", "❌ EXCEPTION: ${e.message}")
            return false
        }
    }
    
    fun checkAllPermissions(): String {
        val sb = StringBuilder()
        sb.append("=== PERMISSION CHECK ===\n")
        
        // Notification permission
        val notifEnabled = notificationManager.areNotificationsEnabled()
        sb.append("Notifications Enabled: $notifEnabled\n")
        
        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager?.getNotificationChannel(CHANNEL_ID)
            sb.append("Channel exists: ${channel != null}\n")
            sb.append("Channel importance: ${channel?.importance}\n")
        }
        
        val result = sb.toString()
        android.util.Log.d("SimpleNotifTest", result)
        return result
    }
    
    private fun createNotificationChannel() {
        android.util.Log.d("SimpleNotifTest", "Creating notification channel...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Test notifications for debugging"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            
            android.util.Log.d("SimpleNotifTest", "✅ Notification channel created")
        }
    }
}
package com.alarmapp.alarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alarmapp.alarmapp.service.ForegroundService

class AlarmReceiver : BroadcastReceiver() {
    val CHANNEL_ID = "alarm_channel"

    override fun onReceive(context: Context, intent: Intent) {
        // Create an Intent for the AlarmNotificationActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, ForegroundService::class.java))
        } else {
            context.startService(Intent(context, ForegroundService::class.java))
        }
    }
}

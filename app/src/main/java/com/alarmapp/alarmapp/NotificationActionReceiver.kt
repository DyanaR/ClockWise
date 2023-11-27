package com.alarmapp.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.alarmapp.alarmapp.service.ForegroundService

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        when (intent.action) {
            "ACTION_SNOOZE" -> {
                Toast.makeText(context, "snooze for 10 mint", Toast.LENGTH_SHORT).show()
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis()+ 600000,//snooze for 10 mint
                    pendingIntent
                )
                val stopIntent = Intent(context, ForegroundService::class.java)
                context.stopService(stopIntent)
            }

            "ACTION_CANCEL" -> {
                // Handle cancel action
                val stopIntent = Intent(context, ForegroundService::class.java)
                context.stopService(stopIntent)
            }
        }
    }
}
package com.alarmapp.alarmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alarmapp.alarmapp.service.ForegroundService

class RepeatingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Create an Intent for the AlarmNotificationActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, ForegroundService::class.java))
        } else {
            context.startService(Intent(context, ForegroundService::class.java))
        }
    }
}
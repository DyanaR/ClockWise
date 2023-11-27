package com.alarmapp.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alarmapp.alarmapp.databinding.ActivitySnoozeCancelAlarmBinding
import com.alarmapp.alarmapp.service.ForegroundService

class SnoozeCancelAlarmActivity : AppCompatActivity() {
    lateinit var binding: ActivitySnoozeCancelAlarmBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySnoozeCancelAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cancelAlarm.setOnClickListener {
            val stopIntent = Intent(this, ForegroundService::class.java)
            applicationContext.stopService(stopIntent)

            finish()
        }
        binding.snoozeAlarm.setOnClickListener {
            val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 600000,//snooze for 10 mint
                pendingIntent
            )

            val stopIntent = Intent(this, ForegroundService::class.java)
            applicationContext.stopService(stopIntent)
            finish()
        }
    }
}
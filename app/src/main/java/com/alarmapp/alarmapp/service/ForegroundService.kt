package com.alarmapp.alarmapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.alarmapp.alarmapp.NotificationActionReceiver
import com.alarmapp.alarmapp.R
import com.alarmapp.alarmapp.SnoozeCancelAlarmActivity

class ForegroundService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    lateinit var notification : Notification
    lateinit var mNotifyManager : NotificationManager


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAlarm()
        mNotifyManager = getSystemService(NotificationManager::class.java)
        notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, 100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        }else{
            startForeground(1, notification)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer.isLooping = true

    }

    private fun startAlarm() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val openNotificationIntent =
            Intent(applicationContext, SnoozeCancelAlarmActivity::class.java)
        openNotificationIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openNotificationIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java)
        snoozeIntent.action = "ACTION_SNOOZE"
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, NotificationActionReceiver::class.java)
        cancelIntent.action = "ACTION_CANCEL"
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle("Alarm Channel")
            .setContentText("Alarm is ringing")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setChannelId("alarm_channel")
            .setAutoCancel(false)
            .addAction(R.drawable.ic_alarm_add, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_alarm_add, "Cancel", cancelPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Set to HIGH or MAX
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // Vibrate pattern
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()
    }

    private fun createNotificationChannel() {
        // Create a NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: NotificationChannel = NotificationChannel(
                "alarm_channel",
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setDescription("Alarm is ringing")
            channel.enableLights(true)
            channel.enableVibration(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                channel.isImportantConversation
            }
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mNotifyManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}

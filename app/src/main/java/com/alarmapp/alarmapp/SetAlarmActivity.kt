package com.alarmapp.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alarmapp.alarmapp.databinding.ActivitySetAlarmBinding
import com.alarmapp.alarmapp.model.AlarmListModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar


class SetAlarmActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetAlarmBinding
    lateinit var repeatingAlarmIntent: Intent
    lateinit var alarmIntent: Intent
    lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repeatingAlarmIntent = Intent(this, RepeatingAlarmReceiver::class.java)
        alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

        binding.repeatAlarm.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.daysOfWeekLayout.visibility = View.VISIBLE
            } else {
                binding.daysOfWeekLayout.visibility = View.GONE
            }
        }
        binding.btnSetAlarm.setOnClickListener {
            setAlarm()
        }

        binding.imgClose.setOnClickListener {
            finish()
        }
        if (intent.extras != null && intent.extras!!.getInt("position", -1) != -1) {

            val bundle: Bundle = intent.extras!!
            var model: AlarmListModel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                model = bundle.getSerializable("data", AlarmListModel::class.java)!!
            } else {
                model = bundle.getSerializable("data") as AlarmListModel
            }
            binding.timePicker.hour = model.alarmHour
            binding.timePicker.minute = model.alarmMinutes
            binding.edtxtAlarmTitle.setText(model.alarmTitle)
            binding.repeatAlarm.isChecked = model.isRepeat
            if (model.isSelected){
                val repeatDays: MutableList<Int> = model.alarmDays
                repeatDays.forEach{
                    when(it){
                        1-> binding.sunCheck.isChecked = true
                        2-> binding.monCheck.isChecked = true
                        3-> binding.tueCheck.isChecked = true
                        4-> binding.wedCheck.isChecked = true
                        5-> binding.thuCheck.isChecked = true
                        6-> binding.friCheck.isChecked = true
                        7-> binding.satCheck.isChecked = true
                    }
                }

            }

        }
    }

    private fun setAlarm() {
        if (intent.extras != null && intent.extras!!.getInt("position", -1) != -1) {

            val bundle: Bundle = intent.extras!!
            var model: AlarmListModel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                model = bundle.getSerializable("data", AlarmListModel::class.java)!!
            } else {
                model = bundle.getSerializable("data") as AlarmListModel
            }

            val alarmManager : AlarmManager =
                binding.root.context.getSystemService(AlarmManager::class.java)
            val alarmIntent = Intent(binding.root.context, AlarmReceiver::class.java)

            model.timeInMilliSecond.forEach {

                val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    binding.root.context,
                    it.toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)

            }

            val sharedPref: SharedPreferences =
                binding.root.context.getSharedPreferences(
                    "AlarmSharedPref",
                    AppCompatActivity.MODE_PRIVATE
                )
            var list = mutableListOf<AlarmListModel>()
            var jsonString = sharedPref.getString("alarms", "[]")
            jsonString.let {
                val existingAlarmList = Gson().fromJson<Collection<AlarmListModel>>(
                    it,
                    object : TypeToken<List<AlarmListModel>>() {}.type
                )
                list.addAll(existingAlarmList)
            }
            val position = bundle.getInt("position", 0)
            if (list.size > position) {
                list.removeAt(position)
            }
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()

        }
        var hour = binding.timePicker.hour
        var minute = binding.timePicker.minute
        var repeatDays: MutableList<Int> = mutableListOf<Int>()

        if (binding.repeatAlarm.isChecked) {
            if (binding.sunCheck.isChecked) {
                repeatDays.add(Calendar.SUNDAY)
            }
            if (binding.monCheck.isChecked) {
                repeatDays.add(Calendar.MONDAY)
            }
            if (binding.tueCheck.isChecked) {
                repeatDays.add(Calendar.TUESDAY)
            }
            if (binding.wedCheck.isChecked) {
                repeatDays.add(Calendar.WEDNESDAY)
            }
            if (binding.thuCheck.isChecked) {
                repeatDays.add(Calendar.THURSDAY)
            }
            if (binding.friCheck.isChecked) {
                repeatDays.add(Calendar.FRIDAY)
            }
            if (binding.satCheck.isChecked) {
                repeatDays.add(Calendar.SATURDAY)
            }
            if (repeatDays.size > 0) {
                setRepeatingAlarm(hour, minute, repeatDays)
            } else {
                Toast.makeText(
                    binding.root.context,
                    " Please Select atleast one day",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMint = calendar.get(Calendar.MINUTE)
            if (hour > currentHour) {
                //today
                repeatDays.add(calendar.get(Calendar.DAY_OF_WEEK))
            } else if (hour < currentHour) {
                //tomorrow
                repeatDays.add(calendar.get(Calendar.DAY_OF_WEEK) + 1)
            } else if (minute > currentMint) {
                //today
                repeatDays.add(calendar.get(Calendar.DAY_OF_WEEK))
            } else {
                //tomorrow
                repeatDays.add(calendar.get(Calendar.DAY_OF_WEEK) + 1)
            }
            setNonRepeatingAlarm(hour, minute, repeatDays)
        }
    }

    private fun setNonRepeatingAlarm(hour: Int, minute: Int, repeatDays: MutableList<Int>) {
        var success = 0
        var listOfTimeInMiliSecond : MutableList<Long> = mutableListOf()
        for (desiredDay in repeatDays) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val nextAlarmTime = getNextAlarmTime(calendar, desiredDay)
            listOfTimeInMiliSecond.add(nextAlarmTime)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        binding.root.context,
                        android.Manifest.permission.SCHEDULE_EXACT_ALARM
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        try {
                            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                                this,
                                nextAlarmTime.toInt(),
                                alarmIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nextAlarmTime,
                                pendingIntent
                            )
                            success = 1

                        } catch (e: Exception) {
                            Toast.makeText(applicationContext, "exception $e", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } else {
                        ScheduleExactAlarmPermission()
                        Toast.makeText(
                            binding.root.context,
                            "Please Allow Permission Schedule Exact Alarm in Setting",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    ScheduleExactAlarmPermission()
                    Toast.makeText(
                        binding.root.context,
                        "opening setting Allow Permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                } else {
                    val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        this,
                        nextAlarmTime.toInt(),
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
                success = 2
            }
        }
        if (success == 1) {
            Toast.makeText(this, "Alarm Scheduled Successfully", Toast.LENGTH_SHORT)
                .show()
        } else if (success == 2) {
            Toast.makeText(this, "Alarm Scheduled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Something went wrong alarm can't be scheduled",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        var alarmTitle = ""
        if (binding.edtxtAlarmTitle.text != null)
            alarmTitle = binding.edtxtAlarmTitle.text.toString()

        var model: AlarmListModel =
            AlarmListModel(
                1,
                hour,
                minute,
                listOfTimeInMiliSecond,
                repeatDays,
                false,
                true,
                alarmTitle
            )
        val sharedPref: SharedPreferences =
            binding.root.context.getSharedPreferences("AlarmSharedPref", MODE_PRIVATE)
        if (sharedPref.getString("alarms", "")?.isEmpty() == true) {
            var list = mutableListOf<AlarmListModel>()
            list.add(model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        } else {
            var list = mutableListOf<AlarmListModel>()
            var jsonString = sharedPref.getString("alarms", "")
            jsonString.let {
                val existingAlarmList = Gson().fromJson<Collection<AlarmListModel>>(
                    it,
                    object : TypeToken<List<AlarmListModel>>() {}.type
                )
                list.addAll(existingAlarmList)
            }
            list.add(model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        }
        finish()
    }

    private fun setRepeatingAlarm(hour: Int, minute: Int, repeatDays: MutableList<Int>) {
        var listOfTimeInMiliSecond : MutableList<Long> = mutableListOf()
        var success = 0

        for (day in repeatDays) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val nextAlarmTime = getNextAlarmTime(calendar, day)
            listOfTimeInMiliSecond.add(nextAlarmTime)
            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                this,
                nextAlarmTime.toInt(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                nextAlarmTime,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )
        }
        Toast.makeText(this, "Repeated Alarm Scheduled Successfully", Toast.LENGTH_SHORT)
            .show()
        var alarmTitle = ""
        if (binding.edtxtAlarmTitle.text != null)
            alarmTitle = binding.edtxtAlarmTitle.text.toString()
        var model: AlarmListModel = AlarmListModel(
            1,
            hour,
            minute,
            listOfTimeInMiliSecond,
            repeatDays,
            true,
            true,
            alarmTitle
        )
        val sharedPref: SharedPreferences =
            binding.root.context.getSharedPreferences("AlarmSharedPref", MODE_PRIVATE)
        if (sharedPref.getString("alarms", "")?.isEmpty() == true) {
            var list = mutableListOf<AlarmListModel>()
            list.add(model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        } else {
            var list = mutableListOf<AlarmListModel>()
            var jsonString = sharedPref.getString("alarms", "[]")
            jsonString.let {
                val existingAlarmList = Gson().fromJson<Collection<AlarmListModel>>(
                    it,
                    object : TypeToken<List<AlarmListModel>>() {}.type
                )
                list.addAll(existingAlarmList)
            }
            list.add(model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        }
        finish()
    }

    private fun getNextAlarmTime(calendar: Calendar, desiredDay: Int): Long {
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToAdd =
            if (currentDay < desiredDay) desiredDay - currentDay else if (currentDay > desiredDay) 7 - (currentDay - desiredDay) else {
                if (calendar.timeInMillis > System.currentTimeMillis()) 0 else 7
            }
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }

    private fun ScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requestPermissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(requestPermissionIntent)
        }
    }
}
package com.alarmapp.alarmapp.adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.alarmapp.alarmapp.AlarmReceiver
import com.alarmapp.alarmapp.databinding.ItemAlarmBinding
import com.alarmapp.alarmapp.model.AlarmListModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class AlarmListAdapter(
    var context: Context,
    var list: MutableList<AlarmListModel>,
    var adapterInterface: AlarmListAdapterInterface
) :
    RecyclerView.Adapter<AlarmListAdapter.ViewHolder>() {
    inner class ViewHolder(var bind: ItemAlarmBinding) : RecyclerView.ViewHolder(bind.root) {

        fun setData(model: AlarmListModel, position: Int) {
            if (!model.alarmTitle.isNullOrEmpty()) {
                bind.alarmTitle.text = model.alarmTitle
            }
            var time = ""
            if (model.alarmHour >= 12) {
                bind.txtAmPm.text = "pm"
                time = (model.alarmHour % 12).toString()
                if (model.alarmHour == 12) {
                    time = "12"
                }
            } else {
                bind.txtAmPm.text = "am"
                time = model.alarmHour.toString()
                if (model.alarmHour == 0) {
                    time = "12"
                }
            }
            if (time.length == 1) {
                time = "0$time"
            }
            if (model.alarmMinutes < 10) {
                bind.txtTime.text = "$time:0${model.alarmMinutes}"

            } else {
                bind.txtTime.text = "$time:${model.alarmMinutes}"
            }
            var days = ""
            model.alarmDays.forEach {
                when (it) {
                    1 -> days = "sun,"
                    2 -> days = "$days mon,"
                    3 -> days = "$days tue,"
                    4 -> days = "$days wed,"
                    5 -> days = "$days thu,"
                    6 -> days = "$days fri,"
                    7 -> days = "$days sat,"
                }
            }
            days = days.dropLast(1)
            bind.txtDay.text = days
            bind.checked.isChecked = model.isSelected
            bind.root.alpha = if (bind.checked.isChecked) 1F else 0.5F
            bind.deleteAlarm.setOnClickListener {
                val builder = AlertDialog.Builder(bind.root.context)
                builder.setTitle("Delete")
                builder.setMessage("Are you Sure To Delete?")

                builder.setPositiveButton("Yes") { _, _ ->
                    adapterInterface.onItemDelete(position)
                }

                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            }
            bind.root.setOnClickListener {
                adapterInterface.onItemClick(model, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(list.get(position), position)
        with(holder.bind) {
            checked.setOnCheckedChangeListener(null) // Clear previous listeners to avoid conflicts
            checked.setOnCheckedChangeListener { _, isChecked ->
                // Handle checkbox state change
                if (isChecked) {
                    val alarmManager: AlarmManager =
                        holder.bind.root.context.getSystemService(AlarmManager::class.java)
                    val alarmIntent = Intent(holder.bind.root.context, AlarmReceiver::class.java)


                    if (list[position].isRepeat) {
                        list[position].timeInMilliSecond.forEach {
                            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                                holder.bind.root.context,
                                it.toInt(),
                                alarmIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                it,
                                pendingIntent
                            )
                            list[position].isSelected = true
                        }
                    } else {
                        list[position].alarmDays.clear()
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = System.currentTimeMillis()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMint = calendar.get(Calendar.MINUTE)
                        if (list[position].alarmHour > currentHour) {
                            //today
                            list[position].alarmDays.add(calendar.get(Calendar.DAY_OF_WEEK))
                        } else if (list[position].alarmHour < currentHour) {
                            //tomorrow
                            list[position].alarmDays.add(calendar.get(Calendar.DAY_OF_WEEK) + 1)
                        } else if (list[position].alarmMinutes > currentMint) {
                            //today
                            list[position].alarmDays.add(calendar.get(Calendar.DAY_OF_WEEK))
                        } else {
                            //tomorrow
                            list[position].alarmDays.add(calendar.get(Calendar.DAY_OF_WEEK) + 1)
                        }
                        setNonRepeatingAlarm(
                            list[position].alarmHour,
                            list[position].alarmMinutes,
                            list[position].alarmDays,
                            list[position].alarmTitle,
                            position
                        )
                        notifyDataSetChanged()
                    }
                    root.alpha = 1F
                } else {
                    val alarmManager =
                        getSystemService(holder.bind.root.context, AlarmManager::class.java)
                    val alarmIntent = Intent(holder.bind.root.context, AlarmReceiver::class.java)

                    list[position].timeInMilliSecond.forEach {

                        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                            holder.bind.root.context,
                            it.toInt(),
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager!!.cancel(pendingIntent)
                    }
                    list[position].isSelected = false
                    root.alpha = 0.5F
                }
                val sharedPref: SharedPreferences =
                    holder.bind.root.context.getSharedPreferences(
                        "AlarmSharedPref",
                        AppCompatActivity.MODE_PRIVATE
                    )
                val updatedAlarmsJson = Gson().toJson(list)
                sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
            }
        }
    }

    private fun setNonRepeatingAlarm(
        hour: Int,
        minute: Int,
        repeatDays: MutableList<Int>,
        alarmTitle: String,
        position: Int
    ) {
        val alarmManager: AlarmManager =
            context.getSystemService(AlarmManager::class.java)
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        var success = 0
        var listOfTimeInMiliSecond: MutableList<Long> = mutableListOf()
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
                        context,
                        android.Manifest.permission.SCHEDULE_EXACT_ALARM
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        try {
                            val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                                context,
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
                            Toast.makeText(context, "exception $e", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } else {
                        ScheduleExactAlarmPermission()
                        Toast.makeText(
                            context,
                            "Please Allow Permission Schedule Exact Alarm in Setting",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    ScheduleExactAlarmPermission()
                    Toast.makeText(
                        context,
                        "opening setting Allow Permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    nextAlarmTime.toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
            }
        }

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
            context.getSharedPreferences("AlarmSharedPref", Context.MODE_PRIVATE)
        if (sharedPref.getString("alarms", "")?.isEmpty() == true) {
            list.add(position,model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        } else {
            list.removeAt(position)
            list.add(position,model)
            val updatedAlarmsJson = Gson().toJson(list)
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
        }
    }

    private fun ScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requestPermissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(context, requestPermissionIntent, null)
        }
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


    override fun getItemCount(): Int {
        return list.size
    }

    interface AlarmListAdapterInterface {
        fun onItemDelete(position: Int)
        fun onItemClick(model: AlarmListModel, position: Int)
    }
}

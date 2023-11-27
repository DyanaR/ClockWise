package com.alarmapp.alarmapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alarmapp.alarmapp.adapter.AlarmListAdapter
import com.alarmapp.alarmapp.databinding.ActivityMainBinding
import com.alarmapp.alarmapp.model.AlarmListModel
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity(), AlarmListAdapter.AlarmListAdapterInterface {
    private lateinit var adapter: AlarmListAdapter
    private var list: MutableList<AlarmListModel> = mutableListOf<AlarmListModel>()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ExcludeFromBatteryOptimisation()
        fnRequestNotificationPermission()



        binding.addAlarm.setOnClickListener {
            startActivity(Intent(this@MainActivity, SetAlarmActivity::class.java))
        }
    }



    private fun setAdapter() {
        setModel()
        adapter = AlarmListAdapter(binding.root.context, list, this)
        binding.rvAlarmList.adapter = adapter
    }

    private fun setModel() {
        val sharedPref: SharedPreferences = binding.root.context.getSharedPreferences("AlarmSharedPref", MODE_PRIVATE)
        if (sharedPref.getString("alarms","")?.isEmpty() == true){
            //list is empty
        }else{
            val jsonString =sharedPref.getString("alarms","[]")
            jsonString.let {
                val existingAlarmList = Gson().fromJson<Collection<AlarmListModel>>(it, object : TypeToken<List<AlarmListModel>>() {}.type)
                list.addAll(existingAlarmList)
            }
        }
    }

    private fun ExcludeFromBatteryOptimisation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
    fun fnRequestNotificationPermission() {
        // Sets up permissions request launcher.
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
                Toast.makeText(binding.root.context, "permission is granted -- requestPermissionLauncher", Toast.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root,
                    "Please grant Notification permission from App Settings",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> {

            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Snackbar.make(
                    binding.root,
                    "The user denied the notifications ):",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Settings") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri: Uri =
                            Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        list.clear()
        setAdapter()
    }

    override fun onItemDelete(position: Int) {
        if (list.size > position) {
            if (list[position].isSelected) {
                val alarmManager =
                    ContextCompat.getSystemService(binding.root.context, AlarmManager::class.java)
                val alarmIntent =
                    Intent(binding.root.context, AlarmReceiver::class.java)

                list[position].timeInMilliSecond.forEach {

                    val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                        binding.root.context,
                        it.toInt(),
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager!!.cancel(pendingIntent)
                    list[position].isSelected = false
                }
            }
            list.removeAt(position)
            val updatedAlarmsJson = Gson().toJson(list)
            val sharedPref: SharedPreferences =
                binding.root.context.getSharedPreferences(
                    "AlarmSharedPref",
                    AppCompatActivity.MODE_PRIVATE
                )
            sharedPref.edit().putString("alarms", updatedAlarmsJson).apply()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onItemClick(model: AlarmListModel, position: Int) {
        var bundle = Bundle()
        bundle.putSerializable("data", model)
        bundle.putInt("position", position)
        val intent = Intent(this@MainActivity, SetAlarmActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)


    }
}
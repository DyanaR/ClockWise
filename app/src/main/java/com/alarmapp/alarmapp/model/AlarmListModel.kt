package com.alarmapp.alarmapp.model

import java.io.Serializable

data class AlarmListModel(
    val alarmId: Int,
    val alarmHour: Int,
    val alarmMinutes: Int,
    val timeInMilliSecond : MutableList<Long>,
    val alarmDays: MutableList<Int>,
    val isRepeat : Boolean,
    var isSelected: Boolean,
    var alarmTitle : String
): Serializable
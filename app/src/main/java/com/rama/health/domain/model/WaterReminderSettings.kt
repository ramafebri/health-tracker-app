package com.rama.health.domain.model

data class WaterReminderSettings(
    val enabled: Boolean = false,
    val scheduleMode: WaterScheduleMode = WaterScheduleMode.INTERVAL,
    val intervalMinutes: Int = 60,
    val activeStartMinutes: Int = 480,
    val activeEndMinutes: Int = 1320,
    val fixedTimes: List<MedicationTime> = emptyList(),
    val dailyGoalMl: Int? = null,
    val todayIntakeMl: Int = 0,
)

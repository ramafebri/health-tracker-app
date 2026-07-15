package com.rama.health.ui.water

enum class WaterReminderValidationError {
    INTERVAL_INVALID,
    ACTIVE_HOURS_INVALID,
    FIXED_TIMES_EMPTY,
    DAILY_GOAL_INVALID,
}

data class WaterReminderUiState(
    val enabled: Boolean = false,
    val scheduleMode: com.rama.health.domain.model.WaterScheduleMode =
        com.rama.health.domain.model.WaterScheduleMode.INTERVAL,
    val intervalMinutes: Int = 60,
    val activeStartMinutes: Int = 480,
    val activeEndMinutes: Int = 1320,
    val fixedTimes: List<com.rama.health.domain.model.MedicationTime> = emptyList(),
    val dailyGoalInput: String = "",
    val todayIntakeMl: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationError: WaterReminderValidationError? = null,
)

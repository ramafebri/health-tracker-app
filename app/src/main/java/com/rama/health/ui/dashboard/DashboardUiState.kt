package com.rama.health.ui.dashboard

data class DashboardUiState(
    val todaySteps: Int = 0,
    val dailyGoal: Int = 10_000,
    val hasActivityRecognitionPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isTrackingActive: Boolean = false,
)

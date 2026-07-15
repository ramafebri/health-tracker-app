package com.rama.health.ui.reminders

data class RemindersHubUiState(
    val waterEnabled: Boolean = false,
    val waterSummary: String = "",
    val medicationCount: Int = 0,
    val hasNotificationPermission: Boolean = true,
    val hasExactAlarmPermission: Boolean = true,
    val isLoading: Boolean = true,
)

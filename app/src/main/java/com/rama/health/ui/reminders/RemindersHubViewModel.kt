package com.rama.health.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.usecase.ObserveMedicationRemindersUseCase
import com.rama.health.domain.usecase.ObserveWaterReminderSettingsUseCase
import com.rama.health.domain.usecase.RescheduleAllRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersHubViewModel @Inject constructor(
    observeWaterReminderSettings: ObserveWaterReminderSettingsUseCase,
    observeMedicationReminders: ObserveMedicationRemindersUseCase,
    private val rescheduleAllReminders: RescheduleAllRemindersUseCase,
) : ViewModel() {

    private val permissionState = MutableStateFlow(PermissionSnapshot())

    val uiState: StateFlow<RemindersHubUiState> = combine(
        observeWaterReminderSettings(),
        observeMedicationReminders(),
        permissionState,
    ) { waterSettings, medications, permissions ->
        RemindersHubUiState(
            waterEnabled = waterSettings.enabled,
            waterSummary = buildWaterSummary(waterSettings),
            medicationCount = medications.size,
            hasNotificationPermission = permissions.hasNotificationPermission,
            hasExactAlarmPermission = permissions.hasExactAlarmPermission,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersHubUiState(isLoading = true),
    )

    fun onPermissionsChecked(
        hasNotificationPermission: Boolean,
        hasExactAlarmPermission: Boolean,
    ) {
        val previous = permissionState.value
        permissionState.update {
            it.copy(
                hasNotificationPermission = hasNotificationPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
            )
        }
        if (
            (!previous.hasNotificationPermission && hasNotificationPermission) ||
            (!previous.hasExactAlarmPermission && hasExactAlarmPermission)
        ) {
            viewModelScope.launch { rescheduleAllReminders() }
        }
    }

    private fun buildWaterSummary(
        settings: com.rama.health.domain.model.WaterReminderSettings,
    ): String {
        if (!settings.enabled) return ""
        return when (settings.scheduleMode) {
            WaterScheduleMode.INTERVAL -> "${settings.intervalMinutes}m"
            WaterScheduleMode.FIXED_TIMES -> "${settings.fixedTimes.size} times"
        }
    }

    private data class PermissionSnapshot(
        val hasNotificationPermission: Boolean = true,
        val hasExactAlarmPermission: Boolean = true,
    )
}

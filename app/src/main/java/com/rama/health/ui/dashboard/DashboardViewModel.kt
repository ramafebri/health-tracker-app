package com.rama.health.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.usecase.ObserveDailyGoalUseCase
import com.rama.health.domain.usecase.ObserveTodayStepsUseCase
import com.rama.health.domain.usecase.SetDailyGoalUseCase
import com.rama.health.domain.usecase.SetTrackingEnabledUseCase
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
class DashboardViewModel @Inject constructor(
    observeTodaySteps: ObserveTodayStepsUseCase,
    observeDailyGoal: ObserveDailyGoalUseCase,
    private val setDailyGoal: SetDailyGoalUseCase,
    private val setTrackingEnabled: SetTrackingEnabledUseCase,
) : ViewModel() {

    private val permissionState = MutableStateFlow(
        PermissionState(hasActivityRecognitionPermission = false, hasNotificationPermission = true),
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        observeTodaySteps(),
        observeDailyGoal(),
        permissionState,
    ) { todaySteps: Int, dailyGoal: Int, permissions: PermissionState ->
        DashboardUiState(
            todaySteps = todaySteps,
            dailyGoal = dailyGoal,
            hasActivityRecognitionPermission = permissions.hasActivityRecognitionPermission,
            hasNotificationPermission = permissions.hasNotificationPermission,
            isTrackingActive = permissions.hasActivityRecognitionPermission,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun onGoalChanged(goal: Int) {
        viewModelScope.launch { setDailyGoal(goal) }
    }

    fun onPermissionsChecked(activityRecognitionGranted: Boolean, notificationsGranted: Boolean) {
        permissionState.update {
            it.copy(
                hasActivityRecognitionPermission = activityRecognitionGranted,
                hasNotificationPermission = notificationsGranted,
            )
        }
    }

    /**
     * Must be called whenever the caller actually starts the foreground tracking service, so
     * that [com.rama.health.service.BootCompletedReceiver] knows to restart it after a reboot.
     */
    fun onTrackingStarted() {
        viewModelScope.launch { setTrackingEnabled(true) }
    }

    private data class PermissionState(
        val hasActivityRecognitionPermission: Boolean,
        val hasNotificationPermission: Boolean,
    )
}

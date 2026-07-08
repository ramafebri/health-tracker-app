package com.rama.health.ui.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.usecase.CancelWorkoutUseCase
import com.rama.health.domain.usecase.ObserveActiveWorkoutUseCase
import com.rama.health.domain.usecase.PauseWorkoutUseCase
import com.rama.health.domain.usecase.ResumeWorkoutUseCase
import com.rama.health.domain.usecase.StartWorkoutUseCase
import com.rama.health.domain.usecase.StopWorkoutUseCase
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
class ActiveWorkoutViewModel @Inject constructor(
    observeActiveWorkout: ObserveActiveWorkoutUseCase,
    private val startWorkout: StartWorkoutUseCase,
    private val pauseWorkout: PauseWorkoutUseCase,
    private val resumeWorkout: ResumeWorkoutUseCase,
    private val stopWorkout: StopWorkoutUseCase,
    private val cancelWorkout: CancelWorkoutUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(ActiveWorkoutUiState())

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        observeActiveWorkout(),
        localState,
    ) { active, local ->
        local.copy(activeWorkout = active)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveWorkoutUiState(),
    )

    fun onTypeSelected(type: WorkoutType) {
        localState.update { it.copy(selectedType = type) }
    }

    fun onPermissionsChecked(locationGranted: Boolean, notificationsGranted: Boolean) {
        localState.update {
            it.copy(
                hasLocationPermission = locationGranted,
                hasNotificationPermission = notificationsGranted,
            )
        }
    }

    fun onStartWorkout(onStarted: () -> Unit) {
        val type = localState.value.selectedType ?: return
        viewModelScope.launch {
            localState.update { it.copy(isStarting = true) }
            startWorkout(type)
            localState.update { it.copy(isStarting = false) }
            onStarted()
        }
    }

    fun onPauseWorkout(onAction: () -> Unit) {
        viewModelScope.launch {
            pauseWorkout()
            onAction()
        }
    }

    fun onResumeWorkout(onAction: () -> Unit) {
        viewModelScope.launch {
            resumeWorkout()
            onAction()
        }
    }

    fun onStopWorkout(onStopped: (String?) -> Unit) {
        viewModelScope.launch {
            val workoutId = stopWorkout()
            localState.update { it.copy(showStopConfirmation = false) }
            onStopped(workoutId)
        }
    }

    fun onCancelWorkout(onCancelled: () -> Unit) {
        viewModelScope.launch {
            cancelWorkout()
            onCancelled()
        }
    }

    fun showStopConfirmation() {
        localState.update { it.copy(showStopConfirmation = true) }
    }

    fun dismissStopConfirmation() {
        localState.update { it.copy(showStopConfirmation = false) }
    }

    val isTracking: Boolean
        get() {
            val status = uiState.value.activeWorkout.status
            return status == WorkoutStatus.ACTIVE || status == WorkoutStatus.PAUSED
        }
}

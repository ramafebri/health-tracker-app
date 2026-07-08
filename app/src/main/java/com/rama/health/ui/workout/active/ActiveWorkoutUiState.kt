package com.rama.health.ui.workout.active

import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.WorkoutType

data class ActiveWorkoutUiState(
    val activeWorkout: ActiveWorkoutState = ActiveWorkoutState(),
    val selectedType: WorkoutType? = null,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isStarting: Boolean = false,
    val showStopConfirmation: Boolean = false,
)

package com.rama.health.ui.workout.list

import com.rama.health.domain.model.WorkoutRecord

data class WorkoutListUiState(
    val workouts: List<WorkoutRecord> = emptyList(),
    val hasActiveWorkout: Boolean = false,
    val isLoading: Boolean = true,
)

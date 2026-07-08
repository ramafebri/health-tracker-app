package com.rama.health.ui.workout.detail

import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord

data class WorkoutDetailUiState(
    val workout: WorkoutRecord? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val isLoading: Boolean = true,
)

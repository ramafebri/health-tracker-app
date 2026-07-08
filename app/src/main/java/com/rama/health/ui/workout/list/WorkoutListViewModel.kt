package com.rama.health.ui.workout.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.usecase.ObserveActiveWorkoutUseCase
import com.rama.health.domain.usecase.ObserveWorkoutHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    observeWorkoutHistory: ObserveWorkoutHistoryUseCase,
    observeActiveWorkout: ObserveActiveWorkoutUseCase,
) : ViewModel() {

    val uiState: StateFlow<WorkoutListUiState> = combine(
        observeWorkoutHistory(),
        observeActiveWorkout(),
    ) { workouts, active ->
        WorkoutListUiState(
            workouts = workouts,
            hasActiveWorkout = active.status == WorkoutStatus.ACTIVE || active.status == WorkoutStatus.PAUSED,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutListUiState(isLoading = true),
    )
}

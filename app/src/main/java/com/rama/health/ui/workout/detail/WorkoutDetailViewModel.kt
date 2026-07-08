package com.rama.health.ui.workout.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.usecase.ObserveWorkoutDetailUseCase
import com.rama.health.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeWorkoutDetail: ObserveWorkoutDetailUseCase,
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle[NavRoutes.WORKOUT_ID_ARG])

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        observeWorkoutDetail(workoutId),
        observeWorkoutDetail.route(workoutId),
    ) { workout, routePoints ->
        WorkoutDetailUiState(
            workout = workout,
            routePoints = routePoints,
            isLoading = workout == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutDetailUiState(isLoading = true),
    )
}

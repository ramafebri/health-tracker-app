package com.rama.health.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.usecase.ObserveActiveWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    observeActiveWorkout: ObserveActiveWorkoutUseCase,
) : ViewModel() {

    val activeWorkout: StateFlow<ActiveWorkoutState> = observeActiveWorkout()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveWorkoutState(),
        )
}

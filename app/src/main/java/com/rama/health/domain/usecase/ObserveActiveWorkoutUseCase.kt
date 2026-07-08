package com.rama.health.domain.usecase

import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActiveWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(): Flow<ActiveWorkoutState> = repository.observeActiveWorkout()
}

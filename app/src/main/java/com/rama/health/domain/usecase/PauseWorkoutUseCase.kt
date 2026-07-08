package com.rama.health.domain.usecase

import com.rama.health.domain.repository.WorkoutRepository
import javax.inject.Inject

class PauseWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke() = repository.pauseWorkout()
}

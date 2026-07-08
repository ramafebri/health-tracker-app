package com.rama.health.domain.usecase

import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.repository.WorkoutRepository
import javax.inject.Inject

class StartWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(type: WorkoutType): String = repository.startWorkout(type)
}

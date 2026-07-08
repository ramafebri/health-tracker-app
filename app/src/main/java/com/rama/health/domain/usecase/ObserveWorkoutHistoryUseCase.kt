package com.rama.health.domain.usecase

import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorkoutHistoryUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(): Flow<List<WorkoutRecord>> = repository.observeWorkoutHistory()
}

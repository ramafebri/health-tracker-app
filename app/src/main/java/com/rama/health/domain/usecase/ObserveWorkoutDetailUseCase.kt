package com.rama.health.domain.usecase

import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWorkoutDetailUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(workoutId: String): Flow<WorkoutRecord?> = repository.observeWorkoutDetail(workoutId)

    fun route(workoutId: String): Flow<List<RoutePoint>> = repository.observeWorkoutRoute(workoutId)
}

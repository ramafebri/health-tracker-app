package com.rama.health.domain.repository

import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutType
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeActiveWorkout(): Flow<ActiveWorkoutState>
    fun observeWorkoutHistory(): Flow<List<WorkoutRecord>>
    fun observeWorkoutDetail(workoutId: String): Flow<WorkoutRecord?>
    fun observeWorkoutRoute(workoutId: String): Flow<List<com.rama.health.domain.model.RoutePoint>>

    suspend fun startWorkout(type: WorkoutType): String
    suspend fun pauseWorkout()
    suspend fun resumeWorkout()
    suspend fun stopWorkout(): String?
    suspend fun cancelWorkout()
    suspend fun onLocationUpdate(latitude: Double, longitude: Double, altitude: Double?, timestampMillis: Long)
    suspend fun restoreActiveWorkoutIfNeeded()
    suspend fun refreshActiveWorkoutElapsed()
}

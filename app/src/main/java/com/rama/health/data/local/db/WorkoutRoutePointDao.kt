package com.rama.health.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRoutePointDao {
    @Insert
    suspend fun insert(point: WorkoutRoutePointEntity)

    @Query("SELECT * FROM workout_route_points WHERE workoutId = :workoutId ORDER BY sequence ASC")
    fun observeByWorkoutId(workoutId: String): Flow<List<WorkoutRoutePointEntity>>

    @Query("SELECT * FROM workout_route_points WHERE workoutId = :workoutId ORDER BY sequence ASC")
    suspend fun getByWorkoutId(workoutId: String): List<WorkoutRoutePointEntity>

    @Query("SELECT MAX(sequence) FROM workout_route_points WHERE workoutId = :workoutId")
    suspend fun getMaxSequence(workoutId: String): Int?

    @Query("DELETE FROM workout_route_points WHERE workoutId = :workoutId")
    suspend fun deleteByWorkoutId(workoutId: String)
}

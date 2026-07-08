package com.rama.health.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Upsert
    suspend fun upsert(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE status IN ('ACTIVE', 'PAUSED') LIMIT 1")
    suspend fun getActiveWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY startTimeMillis DESC")
    fun observeCompleted(): Flow<List<WorkoutEntity>>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)
}

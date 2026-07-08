package com.rama.health.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Upsert
    suspend fun upsert(entity: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyStepsEntity>>
}

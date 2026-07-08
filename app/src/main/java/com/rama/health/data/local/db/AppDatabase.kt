package com.rama.health.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyStepsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao
}

package com.rama.health.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepsEntity::class,
        WorkoutEntity::class,
        WorkoutRoutePointEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutRoutePointDao(): WorkoutRoutePointDao
}

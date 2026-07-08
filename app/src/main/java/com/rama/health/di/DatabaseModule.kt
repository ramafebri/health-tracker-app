package com.rama.health.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rama.health.data.local.db.AppDatabase
import com.rama.health.data.local.db.DailyStepsDao
import com.rama.health.data.local.db.WorkoutDao
import com.rama.health.data.local.db.WorkoutRoutePointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import androidx.room.Room
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workouts (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                startTimeMillis INTEGER NOT NULL,
                endTimeMillis INTEGER,
                durationSeconds INTEGER NOT NULL,
                distanceMeters REAL NOT NULL,
                avgPaceSecPerKm REAL,
                avgSpeedKmh REAL,
                elevationGainMeters REAL NOT NULL,
                minAltitude REAL,
                maxAltitude REAL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_route_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                altitude REAL,
                timestampMillis INTEGER NOT NULL,
                sequence INTEGER NOT NULL,
                FOREIGN KEY(workoutId) REFERENCES workouts(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_route_points_workoutId ON workout_route_points(workoutId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "health_tracker.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideDailyStepsDao(database: AppDatabase): DailyStepsDao = database.dailyStepsDao()

    @Provides
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideWorkoutRoutePointDao(database: AppDatabase): WorkoutRoutePointDao =
        database.workoutRoutePointDao()
}

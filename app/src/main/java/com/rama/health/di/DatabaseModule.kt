package com.rama.health.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rama.health.data.local.db.AppDatabase
import com.rama.health.data.local.db.DailyStepsDao
import com.rama.health.data.local.db.MedicationReminderDao
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

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication_reminders (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                dosage TEXT,
                enabled INTEGER NOT NULL,
                repeatDaysMask INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS medication_reminder_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                medicationId TEXT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                FOREIGN KEY(medicationId) REFERENCES medication_reminders(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_medication_reminder_times_medicationId " +
                "ON medication_reminder_times(medicationId)",
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "health_tracker.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideDailyStepsDao(database: AppDatabase): DailyStepsDao = database.dailyStepsDao()

    @Provides
    fun provideWorkoutDao(database: AppDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideWorkoutRoutePointDao(database: AppDatabase): WorkoutRoutePointDao =
        database.workoutRoutePointDao()

    @Provides
    fun provideMedicationReminderDao(database: AppDatabase): MedicationReminderDao =
        database.medicationReminderDao()
}

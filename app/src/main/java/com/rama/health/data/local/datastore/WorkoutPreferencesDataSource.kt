package com.rama.health.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_prefs")

data class PersistedActiveWorkout(
    val workoutId: String,
    val type: String,
    val status: String,
    val startTimeMillis: Long,
    val pausedDurationMillis: Long,
    val pauseStartMillis: Long?,
)

@Singleton
class WorkoutPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val WORKOUT_ID = stringPreferencesKey("active_workout_id")
        val TYPE = stringPreferencesKey("active_workout_type")
        val STATUS = stringPreferencesKey("active_workout_status")
        val START_TIME = longPreferencesKey("active_workout_start_time")
        val PAUSED_DURATION = longPreferencesKey("active_workout_paused_duration")
        val PAUSE_START = longPreferencesKey("active_workout_pause_start")
    }

    val activeWorkout: Flow<PersistedActiveWorkout?> = context.workoutDataStore.data.map { prefs ->
        val id = prefs[Keys.WORKOUT_ID] ?: return@map null
        val type = prefs[Keys.TYPE] ?: return@map null
        val status = prefs[Keys.STATUS] ?: return@map null
        val startTime = prefs[Keys.START_TIME] ?: return@map null
        PersistedActiveWorkout(
            workoutId = id,
            type = type,
            status = status,
            startTimeMillis = startTime,
            pausedDurationMillis = prefs[Keys.PAUSED_DURATION] ?: 0L,
            pauseStartMillis = prefs[Keys.PAUSE_START],
        )
    }

    suspend fun saveActiveWorkout(session: PersistedActiveWorkout) {
        context.workoutDataStore.edit {
            it[Keys.WORKOUT_ID] = session.workoutId
            it[Keys.TYPE] = session.type
            it[Keys.STATUS] = session.status
            it[Keys.START_TIME] = session.startTimeMillis
            it[Keys.PAUSED_DURATION] = session.pausedDurationMillis
            if (session.pauseStartMillis != null) {
                it[Keys.PAUSE_START] = session.pauseStartMillis
            } else {
                it.remove(Keys.PAUSE_START)
            }
        }
    }

    suspend fun clearActiveWorkout() {
        context.workoutDataStore.edit { it.clear() }
    }
}

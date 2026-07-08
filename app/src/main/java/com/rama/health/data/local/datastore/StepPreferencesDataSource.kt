package com.rama.health.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rama.health.domain.util.LocalDateFormats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "step_prefs")

/**
 * Persisted sensor baseline: [value]/[date] pin the cumulative hardware reading to a calendar
 * day, and [offsetSteps] is how many steps were already credited to [date] at the moment [value]
 * was captured (non-zero after a same-day reboot-recovery re-baseline). See
 * [com.rama.health.domain.util.StepBaselineCalculator] for how this is reconciled.
 */
data class PersistedBaseline(val value: Long, val date: LocalDate, val offsetSteps: Int)

@Singleton
class StepPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val BASELINE_VALUE = longPreferencesKey("baseline_value")
        val BASELINE_DATE = stringPreferencesKey("baseline_date")
        val BASELINE_OFFSET_STEPS = intPreferencesKey("baseline_offset_steps")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal }
    }

    val baseline: Flow<PersistedBaseline?> = context.dataStore.data.map { prefs ->
        val value = prefs[Keys.BASELINE_VALUE]
        val dateStr = prefs[Keys.BASELINE_DATE]
        val offsetSteps = prefs[Keys.BASELINE_OFFSET_STEPS] ?: 0
        if (value != null && dateStr != null) {
            PersistedBaseline(value, LocalDateFormats.parseStorageString(dateStr), offsetSteps)
        } else {
            null
        }
    }

    suspend fun setBaseline(value: Long, date: LocalDate, offsetSteps: Int) {
        context.dataStore.edit {
            it[Keys.BASELINE_VALUE] = value
            it[Keys.BASELINE_DATE] = LocalDateFormats.toStorageString(date)
            it[Keys.BASELINE_OFFSET_STEPS] = offsetSteps
        }
    }

    val trackingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRACKING_ENABLED] ?: false }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TRACKING_ENABLED] = enabled }
    }

    companion object {
        const val DEFAULT_DAILY_GOAL = 10_000
    }
}

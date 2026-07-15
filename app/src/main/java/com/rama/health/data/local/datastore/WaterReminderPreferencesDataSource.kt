package com.rama.health.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.util.LocalDateFormats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.waterReminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "water_reminder_prefs",
)

@Singleton
class WaterReminderPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val SCHEDULE_MODE = stringPreferencesKey("schedule_mode")
        val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        val ACTIVE_START_MINUTES = intPreferencesKey("active_start_minutes")
        val ACTIVE_END_MINUTES = intPreferencesKey("active_end_minutes")
        val FIXED_TIMES = stringPreferencesKey("fixed_times")
        val DAILY_GOAL_ML = intPreferencesKey("daily_goal_ml")
        val TODAY_INTAKE_ML = intPreferencesKey("today_intake_ml")
        val TODAY_INTAKE_DATE = stringPreferencesKey("today_intake_date")
    }

    val settings: Flow<WaterReminderSettings> = context.waterReminderDataStore.data.map { prefs ->
        val today = LocalDate.now()
        val todayStr = LocalDateFormats.toStorageString(today)
        val intakeDateStr = prefs[Keys.TODAY_INTAKE_DATE]
        val todayIntakeMl = if (intakeDateStr == todayStr) {
            prefs[Keys.TODAY_INTAKE_ML] ?: 0
        } else {
            0
        }

        WaterReminderSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            scheduleMode = prefs[Keys.SCHEDULE_MODE]?.let(WaterScheduleMode::valueOf)
                ?: WaterScheduleMode.INTERVAL,
            intervalMinutes = prefs[Keys.INTERVAL_MINUTES] ?: DEFAULT_INTERVAL_MINUTES,
            activeStartMinutes = prefs[Keys.ACTIVE_START_MINUTES] ?: DEFAULT_ACTIVE_START_MINUTES,
            activeEndMinutes = prefs[Keys.ACTIVE_END_MINUTES] ?: DEFAULT_ACTIVE_END_MINUTES,
            fixedTimes = parseFixedTimes(prefs[Keys.FIXED_TIMES]),
            dailyGoalMl = prefs[Keys.DAILY_GOAL_ML],
            todayIntakeMl = todayIntakeMl,
        )
    }

    suspend fun updateSettings(settings: WaterReminderSettings) {
        context.waterReminderDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = settings.enabled
            prefs[Keys.SCHEDULE_MODE] = settings.scheduleMode.name
            prefs[Keys.INTERVAL_MINUTES] = settings.intervalMinutes
            prefs[Keys.ACTIVE_START_MINUTES] = settings.activeStartMinutes
            prefs[Keys.ACTIVE_END_MINUTES] = settings.activeEndMinutes
            prefs[Keys.FIXED_TIMES] = formatFixedTimes(settings.fixedTimes)
            if (settings.dailyGoalMl != null) {
                prefs[Keys.DAILY_GOAL_ML] = settings.dailyGoalMl
            } else {
                prefs.remove(Keys.DAILY_GOAL_ML)
            }
        }
    }

    suspend fun logIntake(amountMl: Int) {
        context.waterReminderDataStore.edit { prefs ->
            val todayStr = LocalDateFormats.toStorageString(LocalDate.now())
            val currentDate = prefs[Keys.TODAY_INTAKE_DATE]
            val current = if (currentDate == todayStr) prefs[Keys.TODAY_INTAKE_ML] ?: 0 else 0
            prefs[Keys.TODAY_INTAKE_DATE] = todayStr
            prefs[Keys.TODAY_INTAKE_ML] = current + amountMl
        }
    }

    suspend fun clearIntake() {
        context.waterReminderDataStore.edit { prefs ->
            prefs[Keys.TODAY_INTAKE_ML] = 0
            prefs[Keys.TODAY_INTAKE_DATE] = LocalDateFormats.toStorageString(LocalDate.now())
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 60
        const val DEFAULT_ACTIVE_START_MINUTES = 480
        const val DEFAULT_ACTIVE_END_MINUTES = 1320

        private fun parseFixedTimes(raw: String?): List<MedicationTime> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(',')
                .mapNotNull { token ->
                    val parts = token.trim().split(':')
                    if (parts.size != 2) return@mapNotNull null
                    val hour = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val minute = parts[1].toIntOrNull() ?: return@mapNotNull null
                    MedicationTime(hour = hour, minute = minute)
                }
        }

        private fun formatFixedTimes(times: List<MedicationTime>): String =
            times.joinToString(",") { "%02d:%02d".format(it.hour, it.minute) }
    }
}

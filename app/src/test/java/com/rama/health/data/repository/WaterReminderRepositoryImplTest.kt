package com.rama.health.data.repository

import app.cash.turbine.test
import com.rama.health.data.local.datastore.WaterReminderPreferencesDataSource
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WaterReminderRepositoryImpl], with [WaterReminderPreferencesDataSource]
 * fully mocked via MockK (no real DataStore instance is constructed).
 */
class WaterReminderRepositoryImplTest {

    private val prefs = mockk<WaterReminderPreferencesDataSource>()
    private fun repository() = WaterReminderRepositoryImpl(prefs)

    @Test
    fun updateSettings_roundTripsThroughObserveSettings() = runTest {
        val settings = WaterReminderSettings(
            enabled = true,
            scheduleMode = WaterScheduleMode.FIXED_TIMES,
            intervalMinutes = 45,
            activeStartMinutes = 540,
            activeEndMinutes = 1260,
            fixedTimes = listOf(MedicationTime(9, 0), MedicationTime(15, 30)),
            dailyGoalMl = 2_000,
        )
        val settingsFlow = MutableStateFlow(settings.copy(todayIntakeMl = 0))
        coEvery { prefs.updateSettings(settings) } coAnswers { settingsFlow.value = settings.copy(todayIntakeMl = 0) }
        every { prefs.settings } returns settingsFlow

        repository().updateSettings(settings)

        repository().observeSettings().test {
            assertEquals(settings.copy(todayIntakeMl = 0), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { prefs.updateSettings(settings) }
    }

    @Test
    fun observeSettings_intakeDayRollover_resetsTodayIntakeToZero() = runTest {
        every { prefs.settings } returns MutableStateFlow(
            WaterReminderSettings(enabled = true, todayIntakeMl = 0),
        )

        assertEquals(0, repository().observeSettings().first().todayIntakeMl)
    }

    @Test
    fun logIntake_delegatesToPreferences() = runTest {
        val settingsFlow = MutableStateFlow(WaterReminderSettings(todayIntakeMl = 400))
        coEvery { prefs.logIntake(150) } coAnswers {
            settingsFlow.value = settingsFlow.value.copy(todayIntakeMl = 400)
        }
        every { prefs.settings } returns settingsFlow

        repository().logIntake(150)

        coVerify { prefs.logIntake(150) }
        assertEquals(400, repository().observeSettings().first().todayIntakeMl)
    }
}

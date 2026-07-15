package com.rama.health.ui.reminders

import app.cash.turbine.test
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.usecase.ObserveMedicationRemindersUseCase
import com.rama.health.domain.usecase.ObserveWaterReminderSettingsUseCase
import com.rama.health.domain.usecase.RescheduleAllRemindersUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RemindersHubViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeWaterSettings = mockk<ObserveWaterReminderSettingsUseCase>()
    private val observeMedicationReminders = mockk<ObserveMedicationRemindersUseCase>()
    private val rescheduleAllReminders = mockk<RescheduleAllRemindersUseCase>()

    private fun viewModel() = RemindersHubViewModel(
        observeWaterSettings,
        observeMedicationReminders,
        rescheduleAllReminders,
    )

    @Test
    fun uiState_combinesWaterSettingsAndMedicationCount() = runTest {
        every { observeWaterSettings() } returns MutableStateFlow(
            WaterReminderSettings(enabled = true, intervalMinutes = 90),
        )
        every { observeMedicationReminders() } returns MutableStateFlow(
            listOf(sampleMedication("med-1"), sampleMedication("med-2")),
        )

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertTrue(state.waterEnabled)
            assertEquals("90m", state.waterSummary)
            assertEquals(2, state.medicationCount)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun uiState_fixedTimesMode_buildsTimesSummary() = runTest {
        every { observeWaterSettings() } returns MutableStateFlow(
            WaterReminderSettings(
                enabled = true,
                scheduleMode = WaterScheduleMode.FIXED_TIMES,
                fixedTimes = listOf(
                    MedicationTime(9, 0),
                    MedicationTime(12, 0),
                    MedicationTime(15, 0),
                ),
            ),
        )
        every { observeMedicationReminders() } returns MutableStateFlow(emptyList())

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertEquals("3 times", state.waterSummary)
        }
    }

    @Test
    fun onPermissionsChecked_updatesPermissionFields() = runTest {
        every { observeWaterSettings() } returns MutableStateFlow(WaterReminderSettings())
        every { observeMedicationReminders() } returns MutableStateFlow(emptyList())

        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()

            vm.onPermissionsChecked(
                hasNotificationPermission = false,
                hasExactAlarmPermission = false,
            )

            val updated = awaitItem()
            assertFalse(updated.hasNotificationPermission)
            assertFalse(updated.hasExactAlarmPermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionsChecked_newlyGrantedPermission_reschedulesReminders() = runTest {
        every { observeWaterSettings() } returns MutableStateFlow(WaterReminderSettings())
        every { observeMedicationReminders() } returns MutableStateFlow(emptyList())
        coEvery { rescheduleAllReminders() } just Runs

        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()

            vm.onPermissionsChecked(
                hasNotificationPermission = false,
                hasExactAlarmPermission = true,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            vm.onPermissionsChecked(
                hasNotificationPermission = true,
                hasExactAlarmPermission = true,
            )
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { rescheduleAllReminders() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleMedication(id: String) = MedicationReminder(
        id = id,
        name = "Medication",
        repeatDays = RepeatDays.allDays(),
        times = listOf(MedicationTime(8, 0)),
        createdAtMillis = 1_000L,
    )
}

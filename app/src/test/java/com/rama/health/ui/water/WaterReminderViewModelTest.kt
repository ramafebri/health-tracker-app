package com.rama.health.ui.water

import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.usecase.LogWaterIntakeUseCase
import com.rama.health.domain.usecase.ObserveWaterReminderSettingsUseCase
import com.rama.health.domain.usecase.UpdateWaterReminderSettingsUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class WaterReminderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeWaterReminderSettings = mockk<ObserveWaterReminderSettingsUseCase>()
    private val updateWaterReminderSettings = mockk<UpdateWaterReminderSettingsUseCase>()
    private val logWaterIntake = mockk<LogWaterIntakeUseCase>()

    private fun viewModel(initial: WaterReminderSettings = WaterReminderSettings()): WaterReminderViewModel {
        every { observeWaterReminderSettings() } returns MutableStateFlow(initial)
        coEvery { updateWaterReminderSettings(any()) } just Runs
        coEvery { logWaterIntake(any()) } just Runs
        return WaterReminderViewModel(
            observeWaterReminderSettings,
            updateWaterReminderSettings,
            logWaterIntake,
        )
    }

    @Test
    fun onIntervalChanged_rapidUpdates_persistsOnlyLatestAfterDebounce() = runTest {
        val viewModel = viewModel(
            initial = WaterReminderSettings(
                enabled = true,
                scheduleMode = WaterScheduleMode.INTERVAL,
            ),
        )
        runCurrent()

        viewModel.onIntervalChanged(60)
        viewModel.onIntervalChanged(90)
        viewModel.onIntervalChanged(120)

        advanceTimeBy(299)
        coVerify(exactly = 0) { updateWaterReminderSettings(any()) }

        advanceTimeBy(1)
        runCurrent()

        val settingsSlot = slot<WaterReminderSettings>()
        coVerify(exactly = 1) { updateWaterReminderSettings(capture(settingsSlot)) }
        assertEquals(120, settingsSlot.captured.intervalMinutes)
    }

    @Test
    fun onEnabledChanged_whenFixedTimesEmpty_showsErrorAndDoesNotPersistOptimisticState() = runTest {
        val viewModel = viewModel(
            initial = WaterReminderSettings(
                enabled = false,
                scheduleMode = WaterScheduleMode.FIXED_TIMES,
                fixedTimes = emptyList(),
            ),
        )
        runCurrent()

        viewModel.onEnabledChanged(true)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.enabled)
        assertEquals(WaterReminderValidationError.FIXED_TIMES_EMPTY, state.validationError)
        coVerify(exactly = 0) { updateWaterReminderSettings(any()) }
    }
}

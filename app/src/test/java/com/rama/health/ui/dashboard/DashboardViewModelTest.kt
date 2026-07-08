package com.rama.health.ui.dashboard

import app.cash.turbine.test
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.usecase.ObserveDailyGoalUseCase
import com.rama.health.domain.usecase.ObserveTodayStepsUseCase
import com.rama.health.domain.usecase.SetDailyGoalUseCase
import com.rama.health.domain.usecase.SetTrackingEnabledUseCase
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

/**
 * Unit tests for [DashboardViewModel], with [ObserveTodayStepsUseCase], [ObserveDailyGoalUseCase]
 * and [SetDailyGoalUseCase] fully mocked via MockK (no repository is constructed).
 */
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeTodaySteps = mockk<ObserveTodayStepsUseCase>()
    private val observeDailyGoal = mockk<ObserveDailyGoalUseCase>()
    private val setDailyGoal = mockk<SetDailyGoalUseCase>()
    private val setTrackingEnabled = mockk<SetTrackingEnabledUseCase>()

    private fun viewModel() =
        DashboardViewModel(observeTodaySteps, observeDailyGoal, setDailyGoal, setTrackingEnabled)

    @Test
    fun uiState_combinesStepsAndGoalFromUseCases() = runTest {
        every { observeTodaySteps() } returns MutableStateFlow(1500)
        every { observeDailyGoal() } returns MutableStateFlow(8_000)

        viewModel().uiState.test {
            // stateIn's initialValue (defaults) is emitted first, before the combine of the
            // mocked flows has had a chance to run on the test dispatcher.
            skipItems(1)

            val state = awaitItem()
            assertEquals(1500, state.todaySteps)
            assertEquals(8_000, state.dailyGoal)
            assertFalse(state.hasActivityRecognitionPermission)
            assertTrue(state.hasNotificationPermission)
            assertFalse(state.isTrackingActive)
        }
    }

    @Test
    fun onGoalChanged_invokesSetDailyGoalUseCaseWithNewGoal() = runTest {
        every { observeTodaySteps() } returns MutableStateFlow(1500)
        every { observeDailyGoal() } returns MutableStateFlow(8_000)
        coEvery { setDailyGoal(any()) } just Runs

        val vm = viewModel()
        vm.uiState.test {
            skipItems(1) // initial default state
            awaitItem() // combined state from the mocked flows
            vm.onGoalChanged(12_000)
            // onGoalChanged launches on viewModelScope (backed by Dispatchers.Main, i.e. the
            // rule's TestDispatcher), which needs its own scheduler advanced to actually run.
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
            coVerify { setDailyGoal(12_000) }
        }
    }

    @Test
    fun onPermissionsChecked_updatesPermissionAndTrackingFields() = runTest {
        every { observeTodaySteps() } returns MutableStateFlow(1500)
        every { observeDailyGoal() } returns MutableStateFlow(8_000)

        val vm = viewModel()
        vm.uiState.test {
            skipItems(1) // initial default state
            awaitItem() // combined state from the mocked flows

            vm.onPermissionsChecked(activityRecognitionGranted = true, notificationsGranted = false)

            val updated = awaitItem()
            assertTrue(updated.hasActivityRecognitionPermission)
            assertFalse(updated.hasNotificationPermission)
            // isTrackingActive mirrors activity recognition permission per the implementation.
            assertTrue(updated.isTrackingActive)

            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(vm.uiState.value.hasActivityRecognitionPermission)
        assertFalse(vm.uiState.value.hasNotificationPermission)
        assertTrue(vm.uiState.value.isTrackingActive)
    }

    @Test
    fun onTrackingStarted_invokesSetTrackingEnabledUseCaseWithTrue() = runTest {
        every { observeTodaySteps() } returns MutableStateFlow(1500)
        every { observeDailyGoal() } returns MutableStateFlow(8_000)
        coEvery { setTrackingEnabled(any()) } just Runs

        val vm = viewModel()
        vm.onTrackingStarted()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { setTrackingEnabled(true) }
    }
}

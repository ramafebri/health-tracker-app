package com.rama.health.ui.workout.active

import app.cash.turbine.test
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.usecase.CancelWorkoutUseCase
import com.rama.health.domain.usecase.ObserveActiveWorkoutUseCase
import com.rama.health.domain.usecase.PauseWorkoutUseCase
import com.rama.health.domain.usecase.ResumeWorkoutUseCase
import com.rama.health.domain.usecase.StartWorkoutUseCase
import com.rama.health.domain.usecase.StopWorkoutUseCase
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
 * Unit tests for [ActiveWorkoutViewModel], with workout use cases fully mocked via MockK.
 */
class ActiveWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeActiveWorkout = mockk<ObserveActiveWorkoutUseCase>()
    private val startWorkout = mockk<StartWorkoutUseCase>()
    private val pauseWorkout = mockk<PauseWorkoutUseCase>()
    private val resumeWorkout = mockk<ResumeWorkoutUseCase>()
    private val stopWorkout = mockk<StopWorkoutUseCase>()
    private val cancelWorkout = mockk<CancelWorkoutUseCase>()

    private fun viewModel() = ActiveWorkoutViewModel(
        observeActiveWorkout,
        startWorkout,
        pauseWorkout,
        resumeWorkout,
        stopWorkout,
        cancelWorkout,
    )

    @Test
    fun uiState_combinesActiveWorkoutWithLocalState() = runTest {
        val active = ActiveWorkoutState(
            workoutId = "active-id",
            type = WorkoutType.RUN,
            status = WorkoutStatus.ACTIVE,
            startTime = 1_000L,
            elapsedSeconds = 120L,
        )
        every { observeActiveWorkout() } returns MutableStateFlow(active)

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertEquals(active, state.activeWorkout)
            assertFalse(state.hasLocationPermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTypeSelected_updatesSelectedType() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())

        val vm = viewModel()
        vm.uiState.test {
            awaitItem()

            vm.onTypeSelected(WorkoutType.CYCLE)

            assertEquals(WorkoutType.CYCLE, awaitItem().selectedType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionsChecked_updatesPermissionFields() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())

        val vm = viewModel()
        vm.uiState.test {
            awaitItem()

            vm.onPermissionsChecked(locationGranted = true, notificationsGranted = false)

            val updated = awaitItem()
            assertTrue(updated.hasLocationPermission)
            assertFalse(updated.hasNotificationPermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onStartWorkout_invokesUseCaseWhenTypeSelected() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())
        coEvery { startWorkout(any()) } returns "new-workout-id"

        val vm = viewModel()
        vm.onTypeSelected(WorkoutType.WALK)

        var started = false
        vm.onStartWorkout { started = true }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { startWorkout(WorkoutType.WALK) }
        assertTrue(started)
        assertFalse(vm.uiState.value.isStarting)
    }

    @Test
    fun onStartWorkout_doesNothingWhenTypeNotSelected() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())

        val vm = viewModel()
        vm.onStartWorkout { }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { startWorkout(any()) }
    }

    @Test
    fun showStopConfirmation_togglesDialogFlag() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())

        val vm = viewModel()
        vm.uiState.test {
            awaitItem()

            vm.showStopConfirmation()
            assertTrue(awaitItem().showStopConfirmation)

            vm.dismissStopConfirmation()
            assertFalse(awaitItem().showStopConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onStopWorkout_invokesUseCaseAndClearsConfirmation() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())
        coEvery { stopWorkout() } returns "stopped-id"

        val vm = viewModel()
        vm.showStopConfirmation()

        var stoppedId: String? = null
        vm.onStopWorkout { stoppedId = it }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify { stopWorkout() }
        assertEquals("stopped-id", stoppedId)
        assertFalse(vm.uiState.value.showStopConfirmation)
    }

    @Test
    fun isTracking_trueWhenActiveOrPaused() = runTest {
        every { observeActiveWorkout() } returns MutableStateFlow(
            ActiveWorkoutState(status = WorkoutStatus.ACTIVE),
        )

        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            assertTrue(vm.isTracking)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

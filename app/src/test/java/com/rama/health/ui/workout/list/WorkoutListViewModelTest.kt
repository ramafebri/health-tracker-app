package com.rama.health.ui.workout.list

import app.cash.turbine.test
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.usecase.ObserveActiveWorkoutUseCase
import com.rama.health.domain.usecase.ObserveWorkoutHistoryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [WorkoutListViewModel], with workout observation use cases fully mocked via MockK.
 */
class WorkoutListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeWorkoutHistory = mockk<ObserveWorkoutHistoryUseCase>()
    private val observeActiveWorkout = mockk<ObserveActiveWorkoutUseCase>()

    private fun viewModel() = WorkoutListViewModel(observeWorkoutHistory, observeActiveWorkout)

    private val sampleWorkout = WorkoutRecord(
        id = "workout-1",
        type = WorkoutType.RUN,
        startTime = 1_000L,
        endTime = 3_601_000L,
        durationSeconds = 3_600L,
        distanceMeters = 5_000.0,
        avgPaceSecPerKm = 720,
        avgSpeedKmh = null,
        elevationGainMeters = 10.0,
        minAltitude = null,
        maxAltitude = null,
    )

    @Test
    fun uiState_combinesHistoryAndActiveWorkout() = runTest {
        every { observeWorkoutHistory() } returns MutableStateFlow(listOf(sampleWorkout))
        every { observeActiveWorkout() } returns MutableStateFlow(ActiveWorkoutState())

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertEquals(listOf(sampleWorkout), state.workouts)
            assertFalse(state.hasActiveWorkout)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun uiState_hasActiveWorkout_trueWhenActiveOrPaused() = runTest {
        every { observeWorkoutHistory() } returns MutableStateFlow(emptyList())
        every { observeActiveWorkout() } returns MutableStateFlow(
            ActiveWorkoutState(
                workoutId = "active-id",
                type = WorkoutType.WALK,
                status = WorkoutStatus.PAUSED,
                startTime = 1_000L,
            ),
        )

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertTrue(state.hasActiveWorkout)
            assertFalse(state.isLoading)
        }
    }
}

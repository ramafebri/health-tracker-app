package com.rama.health.ui.workout.detail

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.usecase.ObserveWorkoutDetailUseCase
import com.rama.health.ui.navigation.NavRoutes
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
 * Unit tests for [WorkoutDetailViewModel], with [ObserveWorkoutDetailUseCase] fully mocked via MockK.
 */
class WorkoutDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val workoutId = "workout-detail-1"
    private val observeWorkoutDetail = mockk<ObserveWorkoutDetailUseCase>()

    private val sampleWorkout = WorkoutRecord(
        id = workoutId,
        type = WorkoutType.RUN,
        startTime = 1_000L,
        endTime = 3_601_000L,
        durationSeconds = 3_600L,
        distanceMeters = 5_000.0,
        avgPaceSecPerKm = 720,
        avgSpeedKmh = null,
        elevationGainMeters = 15.0,
        minAltitude = 90.0,
        maxAltitude = 120.0,
    )

    private val sampleRoute = listOf(
        RoutePoint(lat = 52.52, lng = 13.405, altitude = 100.0, timestamp = 1_000L),
        RoutePoint(lat = 52.521, lng = 13.406, altitude = 105.0, timestamp = 2_000L),
    )

    private fun viewModel() = WorkoutDetailViewModel(
        SavedStateHandle(mapOf(NavRoutes.WORKOUT_ID_ARG to workoutId)),
        observeWorkoutDetail,
    )

    @Test
    fun uiState_combinesWorkoutAndRoutePoints() = runTest {
        every { observeWorkoutDetail(workoutId) } returns MutableStateFlow(sampleWorkout)
        every { observeWorkoutDetail.route(workoutId) } returns MutableStateFlow(sampleRoute)

        viewModel().uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertEquals(sampleWorkout, state.workout)
            assertEquals(sampleRoute, state.routePoints)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun uiState_isLoadingWhenWorkoutMissing() = runTest {
        every { observeWorkoutDetail(workoutId) } returns MutableStateFlow(null)
        every { observeWorkoutDetail.route(workoutId) } returns MutableStateFlow(emptyList())

        viewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            assertEquals(null, state.workout)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

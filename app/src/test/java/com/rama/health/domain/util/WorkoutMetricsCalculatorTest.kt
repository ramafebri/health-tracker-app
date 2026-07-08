package com.rama.health.domain.util

import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [WorkoutMetricsCalculator], covering Haversine distance, pace, speed,
 * elevation gain, and edge cases (duplicate timestamps, missing altitude, paused duration).
 */
class WorkoutMetricsCalculatorTest {

    @Test
    fun computeDistanceMeters_singlePoint_returnsZero() {
        val points = listOf(RoutePoint(lat = 52.52, lng = 13.405, altitude = null, timestamp = 1_000L))

        assertEquals(0.0, WorkoutMetricsCalculator.computeDistanceMeters(points), 0.001)
    }

    @Test
    fun computeDistanceMeters_identicalCoordinates_returnsZero() {
        val points = listOf(
            RoutePoint(lat = 52.52, lng = 13.405, altitude = null, timestamp = 1_000L),
            RoutePoint(lat = 52.52, lng = 13.405, altitude = null, timestamp = 2_000L),
        )

        assertEquals(0.0, WorkoutMetricsCalculator.computeDistanceMeters(points), 0.001)
    }

    @Test
    fun computeDistanceMeters_duplicateTimestamp_skipsSegment() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 1_000L),
            RoutePoint(lat = 0.0, lng = 0.009, altitude = null, timestamp = 1_000L),
            RoutePoint(lat = 0.0, lng = 0.018, altitude = null, timestamp = 2_000L),
        )

        // The A->B segment is skipped (equal timestamps); only B->C contributes.
        val expectedFromSkippedDuplicate = WorkoutMetricsCalculator.computeDistanceMeters(
            listOf(
                RoutePoint(lat = 0.0, lng = 0.009, altitude = null, timestamp = 1_000L),
                RoutePoint(lat = 0.0, lng = 0.018, altitude = null, timestamp = 2_000L),
            ),
        )

        assertEquals(expectedFromSkippedDuplicate, WorkoutMetricsCalculator.computeDistanceMeters(points), 1.0)
    }

    @Test
    fun computeDistanceMeters_equatorLongitudeDelta_isApproxOneKilometer() {
        // ~0.009 degrees longitude at the equator is roughly one kilometre.
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 1_000L),
            RoutePoint(lat = 0.0, lng = 0.009, altitude = null, timestamp = 2_000L),
        )

        val distance = WorkoutMetricsCalculator.computeDistanceMeters(points)

        assertEquals(1_000.0, distance, 50.0)
    }

    @Test
    fun computeMetrics_runWorkout_computesAveragePace() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 0L),
            RoutePoint(lat = 0.0, lng = 0.009, altitude = null, timestamp = 1_000L),
        )
        val startTime = 0L
        val endTime = 600_000L // 10 minutes active

        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.RUN,
            routePoints = points,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
        )

        assertEquals(600L, result.durationSeconds)
        assertNull(result.avgSpeedKmh)
        assertNotNull(result.avgPaceSecPerKm)
        // ~1 km in 600 s -> ~600 sec/km (Haversine distance is slightly over 1 km).
        assertTrue(result.avgPaceSecPerKm!! in 595..605)
    }

    @Test
    fun computeMetrics_cycleWorkout_computesAverageSpeed() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 0L),
            RoutePoint(lat = 0.0, lng = 0.045, altitude = null, timestamp = 1_000L),
        )
        val startTime = 0L
        val endTime = 1_800_000L // 30 minutes

        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.CYCLE,
            routePoints = points,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
        )

        assertNull(result.avgPaceSecPerKm)
        assertNull(result.currentPaceSecPerKm)
        assertEquals(10.0, result.avgSpeedKmh!!, 1.0)
    }

    @Test
    fun computeMetrics_pausedDuration_subtractsFromElapsedTime() {
        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.WALK,
            routePoints = emptyList(),
            startTimeMillis = 0L,
            endTimeMillis = 120_000L,
            pausedDurationSeconds = 30L,
        )

        assertEquals(90L, result.durationSeconds)
    }

    @Test
    fun computeMetrics_pausedDurationLongerThanWallClock_clampsToZero() {
        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.WALK,
            routePoints = emptyList(),
            startTimeMillis = 0L,
            endTimeMillis = 60_000L,
            pausedDurationSeconds = 120L,
        )

        assertEquals(0L, result.durationSeconds)
        assertNull(result.avgPaceSecPerKm)
    }

    @Test
    fun computeMetrics_elevationGain_sumsOnlyPositiveDeltas() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = 100.0, timestamp = 1_000L),
            RoutePoint(lat = 0.0, lng = 0.001, altitude = 110.0, timestamp = 2_000L),
            RoutePoint(lat = 0.0, lng = 0.002, altitude = 105.0, timestamp = 3_000L),
            RoutePoint(lat = 0.0, lng = 0.003, altitude = 120.0, timestamp = 4_000L),
        )

        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.RUN,
            routePoints = points,
            startTimeMillis = 1_000L,
            endTimeMillis = 4_000L,
        )

        assertEquals(25.0, result.elevationGainMeters, 0.001)
        assertEquals(100.0, result.minAltitude)
        assertEquals(120.0, result.maxAltitude)
    }

    @Test
    fun computeMetrics_missingAltitude_excludesFromGainAndMinMax() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 1_000L),
            RoutePoint(lat = 0.0, lng = 0.001, altitude = 200.0, timestamp = 2_000L),
        )

        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.RUN,
            routePoints = points,
            startTimeMillis = 1_000L,
            endTimeMillis = 2_000L,
        )

        assertEquals(0.0, result.elevationGainMeters, 0.001)
        assertEquals(200.0, result.minAltitude)
        assertEquals(200.0, result.maxAltitude)
    }

    @Test
    fun computeMetrics_zeroDistance_returnsNullPaceAndSpeed() {
        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.RUN,
            routePoints = listOf(
                RoutePoint(lat = 52.52, lng = 13.405, altitude = null, timestamp = 1_000L),
            ),
            startTimeMillis = 0L,
            endTimeMillis = 60_000L,
        )

        assertNull(result.currentPaceSecPerKm)
        assertNull(result.avgPaceSecPerKm)
    }

    @Test
    fun computeMetrics_currentPace_usesMostRecentValidSegment() {
        val points = listOf(
            RoutePoint(lat = 0.0, lng = 0.0, altitude = null, timestamp = 0L),
            RoutePoint(lat = 0.0, lng = 0.009, altitude = null, timestamp = 600_000L),
            RoutePoint(lat = 0.0, lng = 0.018, altitude = null, timestamp = 900_000L),
        )

        val result = WorkoutMetricsCalculator.computeMetrics(
            workoutType = WorkoutType.RUN,
            routePoints = points,
            startTimeMillis = 0L,
            endTimeMillis = 900_000L,
        )

        // Last segment: ~1 km in 300 s -> ~300 sec/km (integer truncation).
        assertNotNull(result.currentPaceSecPerKm)
        assertTrue(result.currentPaceSecPerKm!! in 295..305)
    }
}

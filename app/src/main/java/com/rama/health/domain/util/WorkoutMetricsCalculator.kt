package com.rama.health.domain.util

import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class WorkoutMetricsResult(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val currentPaceSecPerKm: Int?,
    val avgPaceSecPerKm: Int?,
    val avgSpeedKmh: Double?,
    val elevationGainMeters: Double,
    val minAltitude: Double?,
    val maxAltitude: Double?,
)

object WorkoutMetricsCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val MILLIS_PER_SECOND = 1_000L

    /**
     * Derives workout metrics from a sequence of GPS [routePoints], the workout's wall-clock
     * bounds, and any accumulated pause time.
     *
     * Distance is the sum of Haversine great-circle distances between consecutive points whose
     * timestamps strictly increase. Segments where the current point's timestamp is less than or
     * equal to the previous point's timestamp contribute zero distance — this absorbs duplicate
     * timestamps and out-of-order fixes without double-counting or producing invalid pace
     * denominators.
     *
     * Elapsed [durationSeconds] is `(endTimeMillis - startTimeMillis) / 1000 -
     * pausedDurationSeconds`, clamped to zero. Paused wall-clock time must be supplied by the
     * caller (the calculator has no visibility into pause/resume events); only active tracking
     * intervals affect pace and speed denominators through this subtraction.
     *
     * Pace and speed are type-specific:
     * - **Run / Walk** — [WorkoutMetricsResult.currentPaceSecPerKm] reflects the most recent
     *   valid segment (strictly increasing timestamps, positive distance and time delta);
     *   [WorkoutMetricsResult.avgPaceSecPerKm] is total active duration divided by total
     *   distance in kilometres. Both are `null` for [WorkoutType.CYCLE] or when insufficient
     *   movement exists.
     * - **Cycle** — [WorkoutMetricsResult.avgSpeedKmh] is total distance in kilometres divided
     *   by active duration in hours. It is `null` for [WorkoutType.RUN] and [WorkoutType.WALK],
     *   or when duration or distance is zero.
     *
     * Elevation [WorkoutMetricsResult.elevationGainMeters] sums only **positive** altitude
     * deltas between consecutive points where **both** altitudes are non-null; flat or descending
     * segments contribute zero gain. [WorkoutMetricsResult.minAltitude] and
     * [WorkoutMetricsResult.maxAltitude] are taken across all points with a non-null altitude;
     * when no altitude data exists, gain is zero and min/max are `null`.
     *
     * Edge cases:
     * - **First point only** — distance and elevation gain are zero; current pace is `null`.
     * - **Duplicate timestamps** — segment skipped (zero distance, not used for current pace).
     * - **Missing altitude** — segment excluded from gain; point excluded from min/max unless
     *   another point supplies altitude.
     * - **Zero distance or zero active duration** — average pace and average speed are `null`.
     */
    fun computeMetrics(
        workoutType: WorkoutType,
        routePoints: List<RoutePoint>,
        startTimeMillis: Long,
        endTimeMillis: Long,
        pausedDurationSeconds: Long = 0L,
    ): WorkoutMetricsResult {
        val distanceMeters = computeDistanceMeters(routePoints)
        val durationSeconds = computeDurationSeconds(
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            pausedDurationSeconds = pausedDurationSeconds,
        )
        val elevationStats = computeElevationStats(routePoints)
        val currentPaceSecPerKm = when (workoutType) {
            WorkoutType.RUN, WorkoutType.WALK -> computeCurrentPaceSecPerKm(routePoints)
            WorkoutType.CYCLE -> null
        }
        val avgPaceSecPerKm = when (workoutType) {
            WorkoutType.RUN, WorkoutType.WALK -> computeAveragePaceSecPerKm(
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
            )
            WorkoutType.CYCLE -> null
        }
        val avgSpeedKmh = when (workoutType) {
            WorkoutType.CYCLE -> computeAverageSpeedKmh(
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
            )
            WorkoutType.RUN, WorkoutType.WALK -> null
        }

        return WorkoutMetricsResult(
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            currentPaceSecPerKm = currentPaceSecPerKm,
            avgPaceSecPerKm = avgPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh,
            elevationGainMeters = elevationStats.elevationGainMeters,
            minAltitude = elevationStats.minAltitude,
            maxAltitude = elevationStats.maxAltitude,
        )
    }

    /**
     * Sums Haversine distances for every consecutive pair with strictly increasing timestamps.
     */
    fun computeDistanceMeters(routePoints: List<RoutePoint>): Double {
        if (routePoints.size < 2) {
            return 0.0
        }

        var totalDistanceMeters = 0.0
        for (index in 1 until routePoints.size) {
            val previous = routePoints[index - 1]
            val current = routePoints[index]
            if (current.timestamp <= previous.timestamp) {
                continue
            }
            totalDistanceMeters += haversineDistanceMeters(
                lat1 = previous.lat,
                lng1 = previous.lng,
                lat2 = current.lat,
                lng2 = current.lng,
            )
        }
        return totalDistanceMeters
    }

    private fun computeDurationSeconds(
        startTimeMillis: Long,
        endTimeMillis: Long,
        pausedDurationSeconds: Long,
    ): Long {
        val wallClockSeconds = (endTimeMillis - startTimeMillis).coerceAtLeast(0L) / MILLIS_PER_SECOND
        return (wallClockSeconds - pausedDurationSeconds.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    private fun computeCurrentPaceSecPerKm(routePoints: List<RoutePoint>): Int? {
        if (routePoints.size < 2) {
            return null
        }

        for (index in routePoints.lastIndex downTo 1) {
            val previous = routePoints[index - 1]
            val current = routePoints[index]
            if (current.timestamp <= previous.timestamp) {
                continue
            }

            val segmentDistanceMeters = haversineDistanceMeters(
                lat1 = previous.lat,
                lng1 = previous.lng,
                lat2 = current.lat,
                lng2 = current.lng,
            )
            val segmentDurationSeconds = (current.timestamp - previous.timestamp) / MILLIS_PER_SECOND
            if (segmentDistanceMeters <= 0.0 || segmentDurationSeconds <= 0L) {
                continue
            }

            return paceSecPerKm(
                durationSeconds = segmentDurationSeconds,
                distanceMeters = segmentDistanceMeters,
            )
        }

        return null
    }

    private fun computeAveragePaceSecPerKm(
        durationSeconds: Long,
        distanceMeters: Double,
    ): Int? {
        if (durationSeconds <= 0L || distanceMeters <= 0.0) {
            return null
        }
        return paceSecPerKm(durationSeconds = durationSeconds, distanceMeters = distanceMeters)
    }

    private fun computeAverageSpeedKmh(
        durationSeconds: Long,
        distanceMeters: Double,
    ): Double? {
        if (durationSeconds <= 0L || distanceMeters <= 0.0) {
            return null
        }
        val distanceKilometers = distanceMeters / 1_000.0
        val durationHours = durationSeconds / 3_600.0
        return distanceKilometers / durationHours
    }

    private fun paceSecPerKm(durationSeconds: Long, distanceMeters: Double): Int {
        val distanceKilometers = distanceMeters / 1_000.0
        return (durationSeconds / distanceKilometers).toInt()
    }

    private fun computeElevationStats(routePoints: List<RoutePoint>): ElevationStats {
        if (routePoints.isEmpty()) {
            return ElevationStats(
                elevationGainMeters = 0.0,
                minAltitude = null,
                maxAltitude = null,
            )
        }

        var elevationGainMeters = 0.0
        var minAltitude: Double? = null
        var maxAltitude: Double? = null

        routePoints.forEach { point ->
            val altitude = point.altitude ?: return@forEach
            minAltitude = minAltitude?.let { currentMin -> minOf(currentMin, altitude) } ?: altitude
            maxAltitude = maxAltitude?.let { currentMax -> maxOf(currentMax, altitude) } ?: altitude
        }

        for (index in 1 until routePoints.size) {
            val previousAltitude = routePoints[index - 1].altitude ?: continue
            val currentAltitude = routePoints[index].altitude ?: continue
            val delta = currentAltitude - previousAltitude
            if (delta > 0.0) {
                elevationGainMeters += delta
            }
        }

        return ElevationStats(
            elevationGainMeters = elevationGainMeters,
            minAltitude = minAltitude,
            maxAltitude = maxAltitude,
        )
    }

    private fun haversineDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val lat1Radians = Math.toRadians(lat1)
        val lat2Radians = Math.toRadians(lat2)
        val deltaLatRadians = Math.toRadians(lat2 - lat1)
        val deltaLngRadians = Math.toRadians(lng2 - lng1)

        val haversine = sin(deltaLatRadians / 2) * sin(deltaLatRadians / 2) +
            cos(lat1Radians) * cos(lat2Radians) *
            sin(deltaLngRadians / 2) * sin(deltaLngRadians / 2)
        val angularDistance = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
        return EARTH_RADIUS_METERS * angularDistance
    }

    private data class ElevationStats(
        val elevationGainMeters: Double,
        val minAltitude: Double?,
        val maxAltitude: Double?,
    )
}

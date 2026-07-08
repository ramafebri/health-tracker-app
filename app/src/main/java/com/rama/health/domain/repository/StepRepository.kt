package com.rama.health.domain.repository

import com.rama.health.domain.model.DailyStepRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Source of truth for step-tracking data, backed by Room (history) and DataStore
 * (baseline/goal persistence). Deliberately free of Android-framework dependencies so it
 * can be mocked with MockK in pure JVM tests without Robolectric.
 */
interface StepRepository {
    fun observeTodaySteps(): Flow<Int>
    fun observeHistory(): Flow<List<DailyStepRecord>>
    fun observeDailyGoal(): Flow<Int>
    suspend fun setDailyGoal(goal: Int)

    /**
     * Called by the foreground sensor service on each hardware step-counter sensor callback.
     * Implementations should reconcile [cumulativeCount] against the persisted baseline using
     * [com.rama.health.domain.util.StepBaselineCalculator], then persist the resulting baseline
     * to DataStore and update today's step total in Room history. Concurrent calls MUST be
     * serialized internally, since sensor callbacks can arrive faster than a single
     * read-compute-write cycle completes.
     */
    suspend fun onSensorReading(cumulativeCount: Long, timestamp: LocalDate)

    /**
     * The cumulative hardware reading recorded in the last persisted baseline, or null if no
     * baseline has ever been captured. Used by the step-detector fallback path to seed its own
     * synthetic running counter on service (re)start, so that a service restart (not a device
     * reboot) doesn't look like a reboot to the calculator.
     */
    suspend fun getPersistedBaselineCumulative(): Long?

    /** Persists whether the user has opted in to background step tracking. */
    suspend fun setTrackingEnabled(enabled: Boolean)
}

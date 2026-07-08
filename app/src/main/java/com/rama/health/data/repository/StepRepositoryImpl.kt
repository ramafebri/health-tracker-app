package com.rama.health.data.repository

import com.rama.health.data.local.datastore.PersistedBaseline
import com.rama.health.data.local.datastore.StepPreferencesDataSource
import com.rama.health.data.local.db.DailyStepsDao
import com.rama.health.data.local.db.DailyStepsEntity
import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.domain.repository.StepRepository
import com.rama.health.domain.util.LocalDateFormats
import com.rama.health.domain.util.StepBaselineCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepRepositoryImpl @Inject constructor(
    private val dao: DailyStepsDao,
    private val prefs: StepPreferencesDataSource,
) : StepRepository {

    // Null until seeded from Room on first collection of observeTodaySteps(), so that a
    // process restart mid-day never briefly reports 0 before the DB read completes.
    private val todayStepsFlow = MutableStateFlow<Int?>(null)
    private val seedMutex = Mutex()

    // Serializes the read-compute-write cycle in onSensorReading(): sensor callbacks can arrive
    // faster than a single cycle completes (especially TYPE_STEP_DETECTOR firing once per step),
    // and without this, concurrent unsynchronized reads/writes of the DAO row and DataStore
    // baseline can race and silently drop or corrupt updates.
    private val sensorReadingMutex = Mutex()

    override fun observeTodaySteps(): Flow<Int> = flow {
        seedTodayStepsIfNeeded()
        emitAll(todayStepsFlow.filterNotNull())
    }

    override fun observeHistory(): Flow<List<DailyStepRecord>> =
        dao.observeAll().map { entities ->
            entities.map { DailyStepRecord(date = LocalDateFormats.parseStorageString(it.date), steps = it.steps) }
        }

    override fun observeDailyGoal(): Flow<Int> = prefs.dailyGoal
    override fun observeTrackingEnabled(): Flow<Boolean> = prefs.trackingEnabled

    override suspend fun setDailyGoal(goal: Int) = prefs.setDailyGoal(goal)

    override suspend fun onSensorReading(cumulativeCount: Long, timestamp: LocalDate): Int {
        return sensorReadingMutex.withLock {
            val existing = dao.getByDate(LocalDateFormats.toStorageString(timestamp))
            val existingSteps = existing?.steps ?: 0

            val persistedBaseline = prefs.baseline.first()
            val baseline = persistedBaseline
                ?: PersistedBaseline(value = cumulativeCount, date = timestamp, offsetSteps = existingSteps)

            val result = StepBaselineCalculator.computeTodaySteps(
                currentCumulative = cumulativeCount,
                currentDate = timestamp,
                baselineValue = baseline.value,
                baselineDate = baseline.date,
                baselineOffsetSteps = baseline.offsetSteps,
                existingTodaySteps = existingSteps,
            )

            // Guard against out-of-order/duplicate sensor callbacks: never overwrite today's
            // persisted total with a lower value than what's already stored for the same day.
            // Bail out entirely (don't touch the baseline either) rather than partially apply a
            // stale reading, so persisted state never drifts out of sync with what's displayed.
            if (existingSteps > result.todaySteps) return@withLock existingSteps

            dao.upsert(
                DailyStepsEntity(
                    date = LocalDateFormats.toStorageString(timestamp),
                    steps = result.todaySteps,
                ),
            )
            prefs.setBaseline(result.newBaselineValue, result.newBaselineDate, result.newBaselineOffsetSteps)
            todayStepsFlow.value = result.todaySteps
            result.todaySteps
        }
    }

    override suspend fun getPersistedBaselineCumulative(): Long? = prefs.baseline.first()?.value

    override suspend fun setTrackingEnabled(enabled: Boolean) = prefs.setTrackingEnabled(enabled)

    private suspend fun seedTodayStepsIfNeeded() {
        if (todayStepsFlow.value != null) return
        seedMutex.withLock {
            if (todayStepsFlow.value == null) {
                todayStepsFlow.value =
                    dao.getByDate(LocalDateFormats.toStorageString(LocalDate.now()))?.steps ?: 0
            }
        }
    }
}

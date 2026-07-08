package com.rama.health.data.repository

import app.cash.turbine.test
import com.rama.health.data.local.datastore.PersistedBaseline
import com.rama.health.data.local.datastore.StepPreferencesDataSource
import com.rama.health.data.local.db.DailyStepsDao
import com.rama.health.data.local.db.DailyStepsEntity
import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.domain.util.LocalDateFormats
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [StepRepositoryImpl], with [DailyStepsDao] and [StepPreferencesDataSource]
 * fully mocked via MockK (no real Room/DataStore instance is constructed).
 */
class StepRepositoryImplTest {

    private val dao = mockk<DailyStepsDao>()
    private val prefs = mockk<StepPreferencesDataSource>()

    private fun repository() = StepRepositoryImpl(dao, prefs)

    @Test
    fun onSensorReading_freshBaseline_initializesBaselineAndPersistsZeroSteps() = runTest {
        val today = LocalDate.of(2026, 7, 7)
        val cumulativeCount = 1000L

        every { prefs.baseline } returns flowOf(null)
        coEvery { dao.getByDate(today.toString()) } returns null
        coEvery { dao.upsert(any()) } just Runs
        coEvery { prefs.setBaseline(any(), any(), any()) } just Runs

        val todaySteps = repository().onSensorReading(cumulativeCount, today)

        // With no prior baseline, the current reading itself becomes the baseline, so today's
        // steps correctly start at zero (there is no earlier reading to diff against yet).
        coVerify { prefs.setBaseline(cumulativeCount, today, 0) }
        coVerify { dao.upsert(DailyStepsEntity(date = today.toString(), steps = 0)) }
        assertEquals(0, todaySteps)
    }

    @Test
    fun onSensorReading_secondCallSameDayHigherValue_incrementsAndReUpserts() = runTest {
        val today = LocalDate.of(2026, 7, 7)
        val baselineValue = 1000L
        // Result persisted by a first onSensorReading call that established the baseline above.
        val firstPersisted = DailyStepsEntity(date = today.toString(), steps = 0)

        every { prefs.baseline } returns flowOf(PersistedBaseline(baselineValue, today, offsetSteps = 0))
        coEvery { dao.getByDate(today.toString()) } returns firstPersisted
        coEvery { dao.upsert(any()) } just Runs
        coEvery { prefs.setBaseline(any(), any(), any()) } just Runs

        val todaySteps = repository().onSensorReading(cumulativeCount = 1500L, timestamp = today)

        coVerify { dao.upsert(DailyStepsEntity(date = today.toString(), steps = 500)) }
        coVerify { prefs.setBaseline(baselineValue, today, 0) }
        assertEquals(500, todaySteps)
    }

    @Test
    fun onSensorReading_lowerValueThanPersisted_doesNotOverwriteOrTouchBaseline() = runTest {
        val today = LocalDate.of(2026, 7, 7)
        val baselineValue = 1000L
        // Simulates an out-of-order/duplicate low sensor callback: the DB already holds a
        // higher step count for today than what this (stale) reading would compute.
        val existing = DailyStepsEntity(date = today.toString(), steps = 800)

        every { prefs.baseline } returns flowOf(PersistedBaseline(baselineValue, today, offsetSteps = 0))
        coEvery { dao.getByDate(today.toString()) } returns existing
        coEvery { dao.upsert(any()) } just Runs
        coEvery { prefs.setBaseline(any(), any(), any()) } just Runs

        // Computes todaySteps = 1200 - 1000 = 200, which is lower than the 800 already stored.
        val todaySteps = repository().onSensorReading(cumulativeCount = 1200L, timestamp = today)

        // Stale reading is dropped entirely: no DAO write, no baseline write.
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { prefs.setBaseline(any(), any(), any()) }
        assertEquals(800, todaySteps)
    }

    @Test
    fun onSensorReading_midDayReboot_preservesExistingStepsAndRebasesBaseline() = runTest {
        val today = LocalDate.of(2026, 7, 7)
        // Existing persisted total (800) must survive a same-day reboot where the sensor's
        // cumulative reading resets to a small post-reboot value (150).
        val existing = DailyStepsEntity(date = today.toString(), steps = 800)

        every { prefs.baseline } returns flowOf(PersistedBaseline(8000L, today, offsetSteps = 0))
        coEvery { dao.getByDate(today.toString()) } returns existing
        coEvery { dao.upsert(any()) } just Runs
        coEvery { prefs.setBaseline(any(), any(), any()) } just Runs

        val todaySteps = repository().onSensorReading(cumulativeCount = 150L, timestamp = today)

        coVerify { dao.upsert(DailyStepsEntity(date = today.toString(), steps = 800)) }
        coVerify { prefs.setBaseline(150L, today, 800) }
        assertEquals(800, todaySteps)
    }

    @Test
    fun onSensorReading_afterRebootOffsetEstablished_accumulatesOnTopOfIt() = runTest {
        val today = LocalDate.of(2026, 7, 7)
        // Follow-up reading after the reboot-recovery baseline above: the persisted total (800)
        // is now also reflected as the DAO's existing row.
        val existing = DailyStepsEntity(date = today.toString(), steps = 800)

        every { prefs.baseline } returns flowOf(PersistedBaseline(150L, today, offsetSteps = 800))
        coEvery { dao.getByDate(today.toString()) } returns existing
        coEvery { dao.upsert(any()) } just Runs
        coEvery { prefs.setBaseline(any(), any(), any()) } just Runs

        val todaySteps = repository().onSensorReading(cumulativeCount = 170L, timestamp = today)

        coVerify { dao.upsert(DailyStepsEntity(date = today.toString(), steps = 820)) }
        coVerify { prefs.setBaseline(150L, today, 800) }
        assertEquals(820, todaySteps)
    }

    @Test
    fun observeHistory_mapsEntitiesToDomainRecordsWithParsedDates() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(
                DailyStepsEntity(date = "2026-07-06", steps = 3000),
                DailyStepsEntity(date = "2026-07-07", steps = 5000),
            ),
        )

        repository().observeHistory().test {
            assertEquals(
                listOf(
                    DailyStepRecord(date = LocalDate.of(2026, 7, 6), steps = 3000),
                    DailyStepRecord(date = LocalDate.of(2026, 7, 7), steps = 5000),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun observeTodaySteps_seedsFromPersistedEntityOnFirstCollection() = runTest {
        val persistedSteps = 2500
        // observeTodaySteps() seeds via dao.getByDate(LocalDate.now().toString()) internally,
        // so the stub must key off the same expression rather than a hardcoded date string.
        val todayStorageDate = LocalDateFormats.toStorageString(LocalDate.now())
        coEvery { dao.getByDate(todayStorageDate) } returns
            DailyStepsEntity(date = todayStorageDate, steps = persistedSteps)

        repository().observeTodaySteps().test {
            // The underlying MutableStateFlow never completes, so we cancel instead of
            // awaiting completion.
            assertEquals(persistedSteps, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getPersistedBaselineCumulative_returnsBaselineValue_orNullIfUnset() = runTest {
        every { prefs.baseline } returns flowOf(PersistedBaseline(4200L, LocalDate.of(2026, 7, 7), 0))
        assertEquals(4200L, repository().getPersistedBaselineCumulative())

        every { prefs.baseline } returns flowOf(null)
        assertEquals(null, repository().getPersistedBaselineCumulative())
    }

    @Test
    fun setTrackingEnabled_delegatesToPreferences() = runTest {
        coEvery { prefs.setTrackingEnabled(any()) } just Runs
        repository().setTrackingEnabled(true)
        coVerify { prefs.setTrackingEnabled(true) }
    }

    @Test
    fun observeTrackingEnabled_delegatesToPreferencesFlow() = runTest {
        every { prefs.trackingEnabled } returns flowOf(true)
        assertEquals(true, repository().observeTrackingEnabled().first())
    }
}

package com.rama.health.data.repository

import app.cash.turbine.test
import com.rama.health.data.local.datastore.PersistedActiveWorkout
import com.rama.health.data.local.datastore.WorkoutPreferencesDataSource
import com.rama.health.data.local.db.WorkoutDao
import com.rama.health.data.local.db.WorkoutEntity
import com.rama.health.data.local.db.WorkoutRoutePointDao
import com.rama.health.data.local.db.WorkoutRoutePointEntity
import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [WorkoutRepositoryImpl], with DAOs and [WorkoutPreferencesDataSource]
 * fully mocked via MockK (no real Room/DataStore instance is constructed).
 */
class WorkoutRepositoryImplTest {

    private val workoutDao = mockk<WorkoutDao>()
    private val routePointDao = mockk<WorkoutRoutePointDao>()
    private val prefs = mockk<WorkoutPreferencesDataSource>()

    private fun repository() = WorkoutRepositoryImpl(workoutDao, routePointDao, prefs)

    private fun stubNoActiveWorkoutInDatabase() {
        coEvery { workoutDao.getActiveWorkout() } returns null
        coEvery { prefs.clearActiveWorkout() } just Runs
        every { prefs.activeWorkout } returns flowOf(null)
    }

    @Test
    fun observeWorkoutHistory_mapsEntitiesToWorkoutRecords() = runTest {
        every { workoutDao.observeCompleted() } returns flowOf(
            listOf(
                WorkoutEntity(
                    id = "workout-1",
                    type = "RUN",
                    status = "COMPLETED",
                    startTimeMillis = 1_000L,
                    endTimeMillis = 3_601_000L,
                    durationSeconds = 3_600L,
                    distanceMeters = 5_000.0,
                    avgPaceSecPerKm = 720.0,
                    avgSpeedKmh = null,
                    elevationGainMeters = 25.0,
                    minAltitude = 90.0,
                    maxAltitude = 115.0,
                ),
            ),
        )

        repository().observeWorkoutHistory().test {
            assertEquals(
                listOf(
                    WorkoutRecord(
                        id = "workout-1",
                        type = WorkoutType.RUN,
                        startTime = 1_000L,
                        endTime = 3_601_000L,
                        durationSeconds = 3_600L,
                        distanceMeters = 5_000.0,
                        avgPaceSecPerKm = 720,
                        avgSpeedKmh = null,
                        elevationGainMeters = 25.0,
                        minAltitude = 90.0,
                        maxAltitude = 115.0,
                    ),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun observeWorkoutRoute_mapsEntitiesToRoutePoints() = runTest {
        every { routePointDao.observeByWorkoutId("workout-1") } returns flowOf(
            listOf(
                WorkoutRoutePointEntity(
                    workoutId = "workout-1",
                    latitude = 52.52,
                    longitude = 13.405,
                    altitude = 100.0,
                    timestampMillis = 1_000L,
                    sequence = 0,
                ),
            ),
        )

        repository().observeWorkoutRoute("workout-1").test {
            assertEquals(
                listOf(RoutePoint(lat = 52.52, lng = 13.405, altitude = 100.0, timestamp = 1_000L)),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun startWorkout_persistsActiveEntityAndSession() = runTest {
        stubNoActiveWorkoutInDatabase()
        coEvery { workoutDao.upsert(any()) } just Runs
        coEvery { prefs.saveActiveWorkout(any()) } just Runs

        val repo = repository()
        val workoutId = repo.startWorkout(WorkoutType.RUN)

        assertNotNull(workoutId)
        coVerify {
            workoutDao.upsert(
                match {
                    it.id == workoutId &&
                        it.type == "RUN" &&
                        it.status == WorkoutStatus.ACTIVE.name
                },
            )
        }
        coVerify {
            prefs.saveActiveWorkout(
                match {
                    it.workoutId == workoutId &&
                        it.type == "RUN" &&
                        it.status == WorkoutStatus.ACTIVE.name
                },
            )
        }

        repo.observeActiveWorkout().test {
            val active = awaitItem()
            assertEquals(workoutId, active.workoutId)
            assertEquals(WorkoutType.RUN, active.type)
            assertEquals(WorkoutStatus.ACTIVE, active.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onLocationUpdate_whenIdle_doesNotInsertRoutePoint() = runTest {
        coEvery { routePointDao.insert(any()) } just Runs

        repository().onLocationUpdate(
            latitude = 52.52,
            longitude = 13.405,
            altitude = 100.0,
            timestampMillis = 1_000L,
        )

        coVerify(exactly = 0) { routePointDao.insert(any()) }
    }

    @Test
    fun onLocationUpdate_whenActive_insertsRoutePointAndUpdatesState() = runTest {
        stubNoActiveWorkoutInDatabase()
        coEvery { workoutDao.upsert(any()) } just Runs
        coEvery { prefs.saveActiveWorkout(any()) } just Runs
        coEvery { routePointDao.insert(any()) } just Runs

        val repo = repository()
        repo.startWorkout(WorkoutType.WALK)
        repo.onLocationUpdate(
            latitude = 52.52,
            longitude = 13.405,
            altitude = 100.0,
            timestampMillis = 1_000L,
        )

        coVerify { routePointDao.insert(match { it.latitude == 52.52 && it.longitude == 13.405 }) }

        repo.observeActiveWorkout().test {
            val active = awaitItem()
            assertEquals(1, active.routePoints.size)
            assertEquals(52.52, active.routePoints.first().lat, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancelWorkout_deletesWorkoutAndClearsSession() = runTest {
        stubNoActiveWorkoutInDatabase()
        coEvery { workoutDao.upsert(any()) } just Runs
        coEvery { prefs.saveActiveWorkout(any()) } just Runs
        coEvery { prefs.clearActiveWorkout() } just Runs
        coEvery { workoutDao.deleteById(any()) } just Runs
        coEvery { routePointDao.deleteByWorkoutId(any()) } just Runs

        val repo = repository()
        val workoutId = repo.startWorkout(WorkoutType.CYCLE)
        repo.cancelWorkout()

        coVerify { routePointDao.deleteByWorkoutId(workoutId) }
        coVerify { workoutDao.deleteById(workoutId) }
        coVerify { prefs.clearActiveWorkout() }

        repo.observeActiveWorkout().test {
            val active = awaitItem()
            assertEquals(WorkoutStatus.IDLE, active.status)
            assertNull(active.workoutId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun stopWorkout_marksCompletedAndClearsActiveSession() = runTest {
        stubNoActiveWorkoutInDatabase()
        coEvery { workoutDao.upsert(any()) } just Runs
        coEvery { prefs.saveActiveWorkout(any()) } just Runs
        coEvery { prefs.clearActiveWorkout() } just Runs

        val repo = repository()
        val workoutId = repo.startWorkout(WorkoutType.RUN)

        repo.stopWorkout()

        coVerify {
            workoutDao.upsert(
                match {
                    it.id == workoutId && it.status == WorkoutStatus.COMPLETED.name
                },
            )
        }
        coVerify { prefs.clearActiveWorkout() }

        repo.observeActiveWorkout().test {
            assertEquals(WorkoutStatus.IDLE, awaitItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restoreActiveWorkoutIfNeeded_restoresPersistedSession() = runTest {
        val persisted = PersistedActiveWorkout(
            workoutId = "restored-id",
            type = "WALK",
            status = WorkoutStatus.PAUSED.name,
            startTimeMillis = 5_000L,
            pausedDurationMillis = 30_000L,
            pauseStartMillis = 60_000L,
        )
        val entity = WorkoutEntity(
            id = "restored-id",
            type = "WALK",
            status = WorkoutStatus.PAUSED.name,
            startTimeMillis = 5_000L,
            endTimeMillis = null,
            durationSeconds = 0L,
            distanceMeters = 0.0,
            avgPaceSecPerKm = null,
            avgSpeedKmh = null,
            elevationGainMeters = 0.0,
            minAltitude = null,
            maxAltitude = null,
        )

        every { prefs.activeWorkout } returns flowOf(persisted)
        coEvery { workoutDao.getActiveWorkout() } returns entity
        coEvery { routePointDao.getByWorkoutId("restored-id") } returns emptyList()

        val repo = repository()
        repo.restoreActiveWorkoutIfNeeded()

        repo.observeActiveWorkout().test {
            val active = awaitItem()
            assertEquals("restored-id", active.workoutId)
            assertEquals(WorkoutType.WALK, active.type)
            assertEquals(WorkoutStatus.PAUSED, active.status)
            assertEquals(5_000L, active.startTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startWorkout_whenActiveSessionExists_returnsExistingId() = runTest {
        val existing = WorkoutEntity(
            id = "existing-id",
            type = "RUN",
            status = WorkoutStatus.ACTIVE.name,
            startTimeMillis = 5_000L,
            endTimeMillis = null,
            durationSeconds = 0L,
            distanceMeters = 0.0,
            avgPaceSecPerKm = null,
            avgSpeedKmh = null,
            elevationGainMeters = 0.0,
            minAltitude = null,
            maxAltitude = null,
        )

        coEvery { workoutDao.getActiveWorkout() } returns existing
        coEvery { prefs.activeWorkout } returns flowOf(null)
        coEvery { routePointDao.getByWorkoutId("existing-id") } returns emptyList()

        val repo = repository()
        val workoutId = repo.startWorkout(WorkoutType.WALK)

        assertEquals("existing-id", workoutId)
        coVerify(exactly = 0) { workoutDao.upsert(any()) }
    }

    @Test
    fun restoreActiveWorkoutIfNeeded_missingEntity_clearsPersistedSession() = runTest {
        coEvery { workoutDao.getActiveWorkout() } returns null
        coEvery { prefs.clearActiveWorkout() } just Runs

        val repo = repository()
        repo.restoreActiveWorkoutIfNeeded()

        coVerify { prefs.clearActiveWorkout() }
        repo.observeActiveWorkout().test {
            assertTrue(awaitItem().status == WorkoutStatus.IDLE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restoreActiveWorkoutIfNeeded_restoresFromDatabaseWhenPrefsMissing() = runTest {
        val entity = WorkoutEntity(
            id = "db-only-id",
            type = "RUN",
            status = WorkoutStatus.ACTIVE.name,
            startTimeMillis = 2_000L,
            endTimeMillis = null,
            durationSeconds = 0L,
            distanceMeters = 0.0,
            avgPaceSecPerKm = null,
            avgSpeedKmh = null,
            elevationGainMeters = 0.0,
            minAltitude = null,
            maxAltitude = null,
        )

        coEvery { workoutDao.getActiveWorkout() } returns entity
        every { prefs.activeWorkout } returns flowOf(null)
        coEvery { routePointDao.getByWorkoutId("db-only-id") } returns emptyList()

        val repo = repository()
        repo.restoreActiveWorkoutIfNeeded()

        repo.observeActiveWorkout().test {
            val active = awaitItem()
            assertEquals("db-only-id", active.workoutId)
            assertEquals(WorkoutStatus.ACTIVE, active.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

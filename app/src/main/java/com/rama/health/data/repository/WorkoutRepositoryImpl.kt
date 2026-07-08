package com.rama.health.data.repository

import com.rama.health.data.local.datastore.PersistedActiveWorkout
import com.rama.health.data.local.datastore.WorkoutPreferencesDataSource
import com.rama.health.data.local.db.WorkoutDao
import com.rama.health.data.local.db.WorkoutEntity
import com.rama.health.data.local.db.WorkoutRoutePointDao
import com.rama.health.data.local.db.WorkoutRoutePointEntity
import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import com.rama.health.domain.repository.WorkoutRepository
import com.rama.health.domain.util.WorkoutMetricsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val routePointDao: WorkoutRoutePointDao,
    private val prefs: WorkoutPreferencesDataSource,
) : WorkoutRepository {

    private val activeWorkoutFlow = MutableStateFlow(ActiveWorkoutState())
    private val locationMutex = Mutex()
    private var pausedDurationMillis = 0L
    private var pauseStartMillis: Long? = null
    private var routePoints = mutableListOf<RoutePoint>()

    override fun observeActiveWorkout(): Flow<ActiveWorkoutState> = activeWorkoutFlow

    override fun observeWorkoutHistory(): Flow<List<WorkoutRecord>> =
        workoutDao.observeCompleted().map { entities -> entities.map { it.toRecord() } }

    override fun observeWorkoutDetail(workoutId: String): Flow<WorkoutRecord?> =
        workoutDao.observeCompleted().map { workouts -> workouts.firstOrNull { it.id == workoutId }?.toRecord() }
            .distinctUntilChanged()

    override fun observeWorkoutRoute(workoutId: String): Flow<List<RoutePoint>> =
        routePointDao.observeByWorkoutId(workoutId).map { points ->
            points.map { RoutePoint(it.latitude, it.longitude, it.altitude, it.timestampMillis) }
        }

    override suspend fun startWorkout(type: WorkoutType): String {
        return locationMutex.withLock {
            restoreActiveWorkoutIfNeededLocked()

            val inMemory = activeWorkoutFlow.value
            if (inMemory.status == WorkoutStatus.ACTIVE || inMemory.status == WorkoutStatus.PAUSED) {
                return inMemory.workoutId!!
            }

            workoutDao.getActiveWorkout()?.let { existing ->
                loadActiveWorkoutFromEntity(existing)
                return existing.id
            }

            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            pausedDurationMillis = 0L
            pauseStartMillis = null
            routePoints = mutableListOf()

            workoutDao.upsert(
                WorkoutEntity(
                    id = id,
                    type = type.name,
                    status = WorkoutStatus.ACTIVE.name,
                    startTimeMillis = now,
                    endTimeMillis = null,
                    durationSeconds = 0L,
                    distanceMeters = 0.0,
                    avgPaceSecPerKm = null,
                    avgSpeedKmh = null,
                    elevationGainMeters = 0.0,
                    minAltitude = null,
                    maxAltitude = null,
                ),
            )
            prefs.saveActiveWorkout(
                PersistedActiveWorkout(
                    workoutId = id,
                    type = type.name,
                    status = WorkoutStatus.ACTIVE.name,
                    startTimeMillis = now,
                    pausedDurationMillis = 0L,
                    pauseStartMillis = null,
                ),
            )
            activeWorkoutFlow.value = ActiveWorkoutState(
                workoutId = id,
                type = type,
                status = WorkoutStatus.ACTIVE,
                startTime = now,
            )
            refreshActiveState(id, type, WorkoutStatus.ACTIVE)
            id
        }
    }

    override suspend fun pauseWorkout() {
        locationMutex.withLock {
            val current = activeWorkoutFlow.value
            if (current.status != WorkoutStatus.ACTIVE || current.workoutId == null || current.type == null) return
            pauseStartMillis = System.currentTimeMillis()
            updateWorkoutStatus(current.workoutId, WorkoutStatus.PAUSED)
            refreshActiveState(current.workoutId, current.type, WorkoutStatus.PAUSED)
            persistSession(current.workoutId, current.type, WorkoutStatus.PAUSED)
        }
    }

    override suspend fun resumeWorkout() {
        locationMutex.withLock {
            val current = activeWorkoutFlow.value
            if (current.status != WorkoutStatus.PAUSED || current.workoutId == null || current.type == null) return
            pauseStartMillis?.let { pauseStart ->
                pausedDurationMillis += System.currentTimeMillis() - pauseStart
            }
            pauseStartMillis = null
            updateWorkoutStatus(current.workoutId, WorkoutStatus.ACTIVE)
            refreshActiveState(current.workoutId, current.type, WorkoutStatus.ACTIVE)
            persistSession(current.workoutId, current.type, WorkoutStatus.ACTIVE)
        }
    }

    override suspend fun stopWorkout(): String? {
        return locationMutex.withLock {
            val current = activeWorkoutFlow.value
            val workoutId = current.workoutId ?: return@withLock null
            val type = current.type ?: return@withLock null
            finalizePausedDuration()
            val endTime = System.currentTimeMillis()
            val pausedSeconds = pausedDurationMillis / 1_000L
            val metrics = WorkoutMetricsCalculator.computeMetrics(
                workoutType = type,
                routePoints = routePoints.toList(),
                startTimeMillis = current.startTime,
                endTimeMillis = endTime,
                pausedDurationSeconds = pausedSeconds,
            )

            workoutDao.upsert(
                WorkoutEntity(
                    id = workoutId,
                    type = type.name,
                    status = WorkoutStatus.COMPLETED.name,
                    startTimeMillis = current.startTime,
                    endTimeMillis = endTime,
                    durationSeconds = metrics.durationSeconds,
                    distanceMeters = metrics.distanceMeters,
                    avgPaceSecPerKm = metrics.avgPaceSecPerKm?.toDouble(),
                    avgSpeedKmh = metrics.avgSpeedKmh,
                    elevationGainMeters = metrics.elevationGainMeters,
                    minAltitude = metrics.minAltitude,
                    maxAltitude = metrics.maxAltitude,
                ),
            )
            prefs.clearActiveWorkout()
            activeWorkoutFlow.value = ActiveWorkoutState()
            routePoints.clear()
            pausedDurationMillis = 0L
            pauseStartMillis = null
            workoutId
        }
    }

    override suspend fun cancelWorkout() {
        locationMutex.withLock {
            val workoutId = activeWorkoutFlow.value.workoutId ?: return@withLock
            routePointDao.deleteByWorkoutId(workoutId)
            workoutDao.deleteById(workoutId)
            prefs.clearActiveWorkout()
            activeWorkoutFlow.value = ActiveWorkoutState()
            routePoints.clear()
            pausedDurationMillis = 0L
            pauseStartMillis = null
        }
    }

    override suspend fun onLocationUpdate(
        latitude: Double,
        longitude: Double,
        altitude: Double?,
        timestampMillis: Long,
    ) {
        locationMutex.withLock {
            val current = activeWorkoutFlow.value
            if (current.status != WorkoutStatus.ACTIVE || current.workoutId == null || current.type == null) return

            val point = RoutePoint(latitude, longitude, altitude, timestampMillis)
            routePoints.add(point)
            routePointDao.insert(
                WorkoutRoutePointEntity(
                    workoutId = current.workoutId,
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    timestampMillis = timestampMillis,
                    sequence = routePoints.lastIndex,
                ),
            )
            refreshActiveState(current.workoutId, current.type, WorkoutStatus.ACTIVE)
        }
    }

    override suspend fun restoreActiveWorkoutIfNeeded() {
        locationMutex.withLock {
            restoreActiveWorkoutIfNeededLocked()
        }
    }

    private suspend fun restoreActiveWorkoutIfNeededLocked() {
        if (activeWorkoutFlow.value.status != WorkoutStatus.IDLE) return

        val entity = workoutDao.getActiveWorkout() ?: run {
            prefs.clearActiveWorkout()
            return
        }

        loadActiveWorkoutFromEntity(entity)
    }

    private suspend fun loadActiveWorkoutFromEntity(entity: WorkoutEntity) {
        val persisted = prefs.activeWorkout.first()
        val type = WorkoutType.valueOf(entity.type)
        val status = WorkoutStatus.valueOf(entity.status)
        pausedDurationMillis = persisted?.pausedDurationMillis ?: 0L
        pauseStartMillis = persisted?.pauseStartMillis
        routePoints = routePointDao.getByWorkoutId(entity.id)
            .map { RoutePoint(it.latitude, it.longitude, it.altitude, it.timestampMillis) }
            .toMutableList()
        activeWorkoutFlow.value = ActiveWorkoutState(
            workoutId = entity.id,
            type = type,
            status = status,
            startTime = entity.startTimeMillis,
        )
        refreshActiveState(entity.id, type, status)
    }

    override suspend fun refreshActiveWorkoutElapsed() {
        locationMutex.withLock {
            val current = activeWorkoutFlow.value
            if (current.workoutId == null || current.type == null) return
            if (current.status == WorkoutStatus.IDLE || current.status == WorkoutStatus.COMPLETED) return
            refreshActiveState(current.workoutId, current.type, current.status)
        }
    }

    private fun refreshActiveState(workoutId: String, type: WorkoutType, status: WorkoutStatus) {
        val startTime = activeWorkoutFlow.value.startTime.takeIf { it > 0L }
            ?: return
        val endTime = if (status == WorkoutStatus.PAUSED) {
            pauseStartMillis ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
        val pausedSeconds = pausedDurationMillis / 1_000L
        val metrics = WorkoutMetricsCalculator.computeMetrics(
            workoutType = type,
            routePoints = routePoints.toList(),
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            pausedDurationSeconds = pausedSeconds,
        )
        activeWorkoutFlow.value = ActiveWorkoutState(
            workoutId = workoutId,
            type = type,
            status = status,
            startTime = startTime,
            elapsedSeconds = metrics.durationSeconds,
            distanceMeters = metrics.distanceMeters,
            currentPaceSecPerKm = metrics.currentPaceSecPerKm,
            avgPaceSecPerKm = metrics.avgPaceSecPerKm,
            avgSpeedKmh = metrics.avgSpeedKmh,
            elevationGainMeters = metrics.elevationGainMeters,
            minAltitude = metrics.minAltitude,
            maxAltitude = metrics.maxAltitude,
            routePoints = routePoints.toList(),
        )
    }

    private suspend fun updateWorkoutStatus(workoutId: String, status: WorkoutStatus) {
        val entity = workoutDao.getById(workoutId) ?: return
        workoutDao.upsert(entity.copy(status = status.name))
    }

    private suspend fun persistSession(workoutId: String, type: WorkoutType, status: WorkoutStatus) {
        prefs.saveActiveWorkout(
            PersistedActiveWorkout(
                workoutId = workoutId,
                type = type.name,
                status = status.name,
                startTimeMillis = activeWorkoutFlow.value.startTime,
                pausedDurationMillis = pausedDurationMillis,
                pauseStartMillis = pauseStartMillis,
            ),
        )
    }

    private fun finalizePausedDuration() {
        pauseStartMillis?.let { pauseStart ->
            pausedDurationMillis += System.currentTimeMillis() - pauseStart
        }
        pauseStartMillis = null
    }

    private fun WorkoutEntity.toRecord(): WorkoutRecord = WorkoutRecord(
        id = id,
        type = WorkoutType.valueOf(type),
        startTime = startTimeMillis,
        endTime = endTimeMillis ?: startTimeMillis,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        avgPaceSecPerKm = avgPaceSecPerKm?.toInt(),
        avgSpeedKmh = avgSpeedKmh,
        elevationGainMeters = elevationGainMeters,
        minAltitude = minAltitude,
        maxAltitude = maxAltitude,
    )
}

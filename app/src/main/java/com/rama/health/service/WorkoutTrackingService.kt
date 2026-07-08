package com.rama.health.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.rama.health.data.location.LocationDataSource
import com.rama.health.domain.model.ActiveWorkoutState
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.repository.WorkoutRepository
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps GPS updates registered while a workout is active,
 * forwarding each fix to [WorkoutRepository] and refreshing the ongoing notification.
 *
 * [onCreate] calls [startForeground] synchronously before any suspend work, matching
 * the invariant documented on [StepCounterService].
 */
@AndroidEntryPoint
class WorkoutTrackingService : Service() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var locationDataSource: LocationDataSource
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var notificationHelper: WorkoutNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickerJob: Job? = null
    private var trackingStatus: WorkoutStatus? = null
    private var locationUpdatesActive = false

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannel(notificationManager)

        val initialNotification = notificationHelper.buildNotification(
            workoutType = null,
            elapsedSeconds = 0L,
            distanceMeters = 0.0,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                WorkoutNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(WorkoutNotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        serviceScope.launch {
            workoutRepository.restoreActiveWorkoutIfNeeded()
            observeActiveWorkout()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> serviceScope.launch { workoutRepository.pauseWorkout() }
            ACTION_RESUME -> serviceScope.launch { workoutRepository.resumeWorkout() }
            ACTION_STOP -> serviceScope.launch {
                workoutRepository.stopWorkout()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun observeActiveWorkout() {
        serviceScope.launch {
            workoutRepository.observeActiveWorkout().collect { state ->
                refreshNotification(state)
                if (state.status != trackingStatus) {
                    onTrackingStatusChanged(state.status)
                    trackingStatus = state.status
                }
            }
        }
    }

    private fun onTrackingStatusChanged(status: WorkoutStatus) {
        when (status) {
            WorkoutStatus.ACTIVE -> {
                startLocationUpdatesIfNeeded()
                startTicker()
            }
            WorkoutStatus.PAUSED -> {
                stopLocationUpdates()
                stopTicker()
            }
            WorkoutStatus.IDLE, WorkoutStatus.COMPLETED -> {
                stopLocationUpdates()
                stopTicker()
                stopSelf()
            }
        }
    }

    private fun startLocationUpdatesIfNeeded() {
        if (locationUpdatesActive) return
        if (!PermissionUtils.hasFineLocationPermission(this)) {
            Log.w(TAG, "ACCESS_FINE_LOCATION permission missing; skipping GPS registration")
            return
        }
        locationUpdatesActive = true
        locationDataSource.startUpdates(LOCATION_INTERVAL_MS) { lat, lng, alt, timestamp ->
            serviceScope.launch {
                workoutRepository.onLocationUpdate(lat, lng, alt, timestamp)
            }
        }
    }

    private fun stopLocationUpdates() {
        if (!locationUpdatesActive) return
        locationDataSource.stopUpdates()
        locationUpdatesActive = false
    }

    private fun refreshNotification(state: ActiveWorkoutState) {
        notificationManager.notify(
            WorkoutNotificationHelper.NOTIFICATION_ID,
            notificationHelper.buildNotification(
                workoutType = state.type,
                elapsedSeconds = state.elapsedSeconds,
                distanceMeters = state.distanceMeters,
            ),
        )
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                workoutRepository.refreshActiveWorkoutElapsed()
                delay(TICKER_INTERVAL_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocationUpdates()
        stopTicker()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WorkoutTrackingService"
        const val ACTION_PAUSE = "com.rama.health.action.WORKOUT_PAUSE"
        const val ACTION_RESUME = "com.rama.health.action.WORKOUT_RESUME"
        const val ACTION_STOP = "com.rama.health.action.WORKOUT_STOP"
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val TICKER_INTERVAL_MS = 1_000L
    }
}

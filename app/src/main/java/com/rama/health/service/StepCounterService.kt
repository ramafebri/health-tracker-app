package com.rama.health.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.rama.health.domain.repository.StepRepository
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Foreground service that keeps the step-counter/detector sensor registered while step
 * tracking is enabled, forwarding each sensor callback to [StepRepository] and refreshing
 * the ongoing notification with the latest today/goal totals.
 */
@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject lateinit var stepRepository: StepRepository
    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var notificationHelper: StepCounterNotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var usingStepDetectorFallback = false
    private var detectorCumulativeCount = 0L
    @Volatile private var cachedDailyGoal = DEFAULT_GOAL_PLACEHOLDER
    private var isGoalObservationStarted = false

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannel(notificationManager)

        // Must be called synchronously, before any suspend/async work, to satisfy Android's
        // 5-second foreground-promotion window after startForegroundService().
        val initialNotification = notificationHelper.buildNotification(todaySteps = 0, goal = DEFAULT_GOAL_PLACEHOLDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                StepCounterNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            startForeground(StepCounterNotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        ensureGoalObservationStarted()
        registerSensorListener()
    }

    private fun registerSensorListener() {
        if (!PermissionUtils.hasActivityRecognitionPermission(this)) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission missing; skipping sensor registration")
            return
        }
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor != null) {
            usingStepDetectorFallback = false
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.w(TAG, "TYPE_STEP_COUNTER unavailable on this device; falling back to TYPE_STEP_DETECTOR")
            val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (stepDetectorSensor != null) {
                usingStepDetectorFallback = true
                seedDetectorCumulativeCount()
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Log.w(TAG, "Neither TYPE_STEP_COUNTER nor TYPE_STEP_DETECTOR available; step tracking disabled on this device")
            }
        }
    }

    /**
     * TYPE_STEP_DETECTOR has no cumulative value of its own, so [detectorCumulativeCount] is a
     * synthetic running counter fed into the same baseline-diffing logic TYPE_STEP_COUNTER uses.
     * Without seeding it from the last persisted baseline, every service restart (process death,
     * not just a device reboot) would reset it to 0, which the calculator would then
     * misinterpret as a device reboot and incorrectly re-baseline. Seeding is asynchronous
     * (DataStore read), so there is a brief window right after registration where an in-flight
     * sensor event could still use the pre-seed value of 0 -- an acceptable, narrow trade-off
     * given how much more common service restarts are than a race landing in this exact window.
     */
    private fun seedDetectorCumulativeCount() {
        serviceScope.launch {
            detectorCumulativeCount = stepRepository.getPersistedBaselineCumulative() ?: 0L
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        ensureGoalObservationStarted()
        val cumulativeCount = if (usingStepDetectorFallback) {
            // TYPE_STEP_DETECTOR fires exactly once per step (values[0] is always 1f), so we
            // maintain our own running cumulative counter to feed the same baseline-diffing
            // logic that TYPE_STEP_COUNTER's cumulative readings use.
            detectorCumulativeCount += 1
            detectorCumulativeCount
        } else {
            event.values.getOrElse(0) { 0f }.toLong()
        }

        val today = LocalDate.now()
        serviceScope.launch {
            val todaySteps = stepRepository.onSensorReading(cumulativeCount, today)
            refreshNotification(todaySteps)
        }
    }

    private fun ensureGoalObservationStarted() {
        if (isGoalObservationStarted) return
        isGoalObservationStarted = true
        serviceScope.launch {
            stepRepository.observeDailyGoal().collect { goal ->
                cachedDailyGoal = goal
            }
        }
    }

    private suspend fun refreshNotification(todaySteps: Int) {
        val notification = notificationHelper.buildNotification(todaySteps, cachedDailyGoal)
        notificationManager.notify(StepCounterNotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StepCounterService"
        private const val DEFAULT_GOAL_PLACEHOLDER = 10_000
    }
}

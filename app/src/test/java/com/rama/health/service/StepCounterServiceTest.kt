package com.rama.health.service

import android.app.NotificationManager
import android.app.Service
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.rama.health.domain.repository.StepRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [StepCounterService]. The service is [dagger.hilt.android.AndroidEntryPoint],
 * so calling its real `onCreate()` would trigger Hilt's generated injection against the app's
 * production Dagger graph (there is no Hilt test harness wired up for plain Robolectric unit
 * tests here). To keep this a fast, isolated unit test we therefore:
 *  - obtain the service instance via `Robolectric.buildService(...).get()`, which attaches the
 *    instance to a Context but deliberately never calls `.create()`/`onCreate()`;
 *  - assign the `@Inject lateinit var` fields directly with MockK mocks (they are non-private,
 *    so this is a plain Kotlin field assignment, no reflection needed for the fields);
 *  - invoke the private `registerSensorListener()` helper via reflection, since it is normally
 *    only reachable from `onCreate()`.
 *
 * This exercises the real sensor-fallback and sensor-event-handling logic without ever running
 * Hilt injection or requiring a full instrumented/Hilt test. `onSensorChanged`'s coroutine
 * dispatch onto `Dispatchers.IO` is verified with MockK's timeout-based `coVerify`, since the
 * service does not expose a way to inject a test dispatcher.
 */
@RunWith(RobolectricTestRunner::class)
class StepCounterServiceTest {

    private lateinit var stepRepository: StepRepository
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationHelper: StepCounterNotificationHelper

    private fun buildService(): StepCounterService {
        val service = Robolectric.buildService(StepCounterService::class.java).get()
        service.stepRepository = stepRepository
        service.sensorManager = sensorManager
        service.notificationManager = notificationManager
        service.notificationHelper = notificationHelper
        return service
    }

    private fun invokeRegisterSensorListener(service: StepCounterService) {
        val method = StepCounterService::class.java.getDeclaredMethod("registerSensorListener")
        method.isAccessible = true
        method.invoke(service)
    }

    private fun newSensorEvent(value: Float): SensorEvent {
        val ctor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
        ctor.isAccessible = true
        val event = ctor.newInstance(1) as SensorEvent
        event.values[0] = value
        return event
    }

    @Before
    fun setUp() {
        stepRepository = mockk(relaxed = true)
        sensorManager = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
    }

    @Test
    fun registerSensorListener_usesStepCounter_whenAvailable() {
        val counterSensor = mockk<Sensor>()
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns counterSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any<Int>()) } returns true

        val service = buildService()
        invokeRegisterSensorListener(service)

        verify { sensorManager.registerListener(service, counterSensor, SensorManager.SENSOR_DELAY_NORMAL) }
        verify(exactly = 0) { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    }

    @Test
    fun registerSensorListener_fallsBackToStepDetector_whenStepCounterUnavailable() {
        val detectorSensor = mockk<Sensor>()
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns null
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) } returns detectorSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any<Int>()) } returns true

        val service = buildService()
        invokeRegisterSensorListener(service)

        verify { sensorManager.registerListener(service, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    @Test
    fun registerSensorListener_doesNothing_whenNeitherSensorAvailable() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns null
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) } returns null

        val service = buildService()
        invokeRegisterSensorListener(service)

        verify(exactly = 0) { sensorManager.registerListener(any(), any<Sensor>(), any<Int>()) }
    }

    @Test
    fun onSensorChanged_stepCounterMode_forwardsCumulativeValueToRepository() {
        val service = buildService()

        service.onSensorChanged(newSensorEvent(4200f))

        coVerify(timeout = 2_000) { stepRepository.onSensorReading(4200L, any()) }
    }

    @Test
    fun onSensorChanged_stepDetectorFallbackMode_incrementsOwnCumulativeCounter() {
        val service = buildService()
        val fallbackField = StepCounterService::class.java.getDeclaredField("usingStepDetectorFallback")
        fallbackField.isAccessible = true
        fallbackField.setBoolean(service, true)

        // TYPE_STEP_DETECTOR fires with values[0] always 1f; the service should ignore the raw
        // value and maintain its own running total instead.
        service.onSensorChanged(newSensorEvent(1f))
        service.onSensorChanged(newSensorEvent(1f))

        coVerify(timeout = 2_000) { stepRepository.onSensorReading(1L, any()) }
        coVerify(timeout = 2_000) { stepRepository.onSensorReading(2L, any()) }
    }

    @Test
    fun onSensorChanged_refreshesNotificationWithLatestTotals() {
        every { stepRepository.observeTodaySteps() } returns flowOf(4200)
        every { stepRepository.observeDailyGoal() } returns flowOf(8000)
        every { notificationHelper.buildNotification(any(), any()) } returns mockk(relaxed = true)

        val service = buildService()
        service.onSensorChanged(newSensorEvent(4200f))

        verify(timeout = 2_000) { notificationHelper.buildNotification(4200, 8000) }
        verify(timeout = 2_000) { notificationManager.notify(StepCounterNotificationHelper.NOTIFICATION_ID, any()) }
    }

    @Test
    fun onBind_returnsNull() {
        val service = buildService()
        assertNull(service.onBind(null))
    }

    @Test
    fun onStartCommand_returnsStartSticky() {
        val service = buildService()
        assertEquals(Service.START_STICKY, service.onStartCommand(null, 0, 0))
    }

    @Test
    fun onDestroy_unregistersSensorListener() {
        every { sensorManager.unregisterListener(any<android.hardware.SensorEventListener>()) } just runs

        val service = buildService()
        service.onDestroy()

        verify { sensorManager.unregisterListener(service) }
    }
}

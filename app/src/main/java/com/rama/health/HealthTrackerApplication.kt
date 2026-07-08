package com.rama.health

import android.app.Application
import com.rama.health.domain.repository.WorkoutRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HealthTrackerApplication : Application() {

    @Inject lateinit var workoutRepository: WorkoutRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            workoutRepository.restoreActiveWorkoutIfNeeded()
        }
    }
}

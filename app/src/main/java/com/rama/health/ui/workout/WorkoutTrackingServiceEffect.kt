package com.rama.health.ui.workout

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.service.WorkoutTrackingService
import com.rama.health.util.PermissionUtils

@Composable
fun WorkoutTrackingServiceEffect(
    viewModel: WorkoutSessionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val isTracking = activeWorkout.status == WorkoutStatus.ACTIVE ||
        activeWorkout.status == WorkoutStatus.PAUSED

    LaunchedEffect(isTracking) {
        if (isTracking && PermissionUtils.hasWorkoutTrackingPermissions(context)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WorkoutTrackingService::class.java),
            )
        }
    }
}

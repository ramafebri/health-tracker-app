package com.rama.health.ui.workout.active

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.domain.model.WorkoutStatus
import com.rama.health.domain.model.WorkoutType
import com.rama.health.service.WorkoutTrackingService
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.ui.workout.components.WorkoutMap
import com.rama.health.ui.workout.components.WorkoutPermissionCard
import com.rama.health.ui.workout.components.WorkoutStatsCard
import com.rama.health.ui.workout.components.WorkoutTypeSelector
import com.rama.health.util.PermissionUtils

@Composable
fun ActiveWorkoutScreen(
    onNavigateBack: () -> Unit,
    onWorkoutStopped: (String?) -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isTracking = uiState.activeWorkout.status == WorkoutStatus.ACTIVE ||
        uiState.activeWorkout.status == WorkoutStatus.PAUSED

    fun startTrackingService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, WorkoutTrackingService::class.java),
        )
    }

    fun sendServiceAction(action: String) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, WorkoutTrackingService::class.java).setAction(action),
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val locationGranted = result[android.Manifest.permission.ACCESS_FINE_LOCATION]
            ?: PermissionUtils.hasFineLocationPermission(context)
        val notificationsGranted = result[android.Manifest.permission.POST_NOTIFICATIONS]
            ?: PermissionUtils.hasPostNotificationsPermission(context)
        viewModel.onPermissionsChecked(locationGranted, notificationsGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionsChecked(
            locationGranted = PermissionUtils.hasFineLocationPermission(context),
            notificationsGranted = PermissionUtils.hasPostNotificationsPermission(context),
        )
    }

    BackHandler(enabled = isTracking) {
        viewModel.showStopConfirmation()
    }

    if (uiState.showStopConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissStopConfirmation,
            title = { Text("Stop workout?") },
            text = { Text("Your progress will be saved to history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onStopWorkout { workoutId ->
                            sendServiceAction(WorkoutTrackingService.ACTION_STOP)
                            onWorkoutStopped(workoutId)
                        }
                    },
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissStopConfirmation) {
                    Text("Continue")
                }
            },
        )
    }

    ActiveWorkoutContent(
        uiState = uiState,
        onNavigateBack = {
            if (isTracking) viewModel.showStopConfirmation() else onNavigateBack()
        },
        onGrantPermissions = {
            permissionLauncher.launch(PermissionUtils.requiredWorkoutRuntimePermissions())
        },
        onTypeSelected = viewModel::onTypeSelected,
        onStartWorkout = {
            viewModel.onStartWorkout {
                startTrackingService()
            }
        },
        onPauseWorkout = { viewModel.onPauseWorkout { sendServiceAction(WorkoutTrackingService.ACTION_PAUSE) } },
        onResumeWorkout = { viewModel.onResumeWorkout { sendServiceAction(WorkoutTrackingService.ACTION_RESUME) } },
        onStopWorkout = viewModel::showStopConfirmation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onNavigateBack: () -> Unit,
    onGrantPermissions: () -> Unit,
    onTypeSelected: (WorkoutType) -> Unit,
    onStartWorkout: () -> Unit,
    onPauseWorkout: () -> Unit,
    onResumeWorkout: () -> Unit,
    onStopWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTracking = uiState.activeWorkout.status == WorkoutStatus.ACTIVE ||
        uiState.activeWorkout.status == WorkoutStatus.PAUSED
    val hasPermissions = uiState.hasLocationPermission && uiState.hasNotificationPermission

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isTracking) "Active Workout" else "New Workout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasPermissions) {
                WorkoutPermissionCard(onRequestClick = onGrantPermissions)
            }

            if (!isTracking) {
                WorkoutTypeSelector(
                    selectedType = uiState.selectedType,
                    onTypeSelected = onTypeSelected,
                )
                Button(
                    onClick = onStartWorkout,
                    enabled = hasPermissions && uiState.selectedType != null && !uiState.isStarting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isStarting) {
                        CircularProgressIndicator()
                    } else {
                        Text("Start Workout")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                ) {
                    WorkoutMap(routePoints = uiState.activeWorkout.routePoints)
                }

                WorkoutStatsCard(
                    workoutType = uiState.activeWorkout.type,
                    elapsedSeconds = uiState.activeWorkout.elapsedSeconds,
                    distanceMeters = uiState.activeWorkout.distanceMeters,
                    avgPaceSecPerKm = uiState.activeWorkout.avgPaceSecPerKm,
                    avgSpeedKmh = uiState.activeWorkout.avgSpeedKmh,
                    elevationGainMeters = uiState.activeWorkout.elevationGainMeters,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.activeWorkout.status == WorkoutStatus.ACTIVE) {
                        OutlinedButton(
                            onClick = onPauseWorkout,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Pause")
                        }
                    } else {
                        Button(
                            onClick = onResumeWorkout,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Resume")
                        }
                    }
                    Button(
                        onClick = onStopWorkout,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveWorkoutContentPreview() {
    HealthTrackerAppTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(
                selectedType = WorkoutType.RUN,
                hasLocationPermission = true,
                hasNotificationPermission = true,
            ),
            onNavigateBack = {},
            onGrantPermissions = {},
            onTypeSelected = {},
            onStartWorkout = {},
            onPauseWorkout = {},
            onResumeWorkout = {},
            onStopWorkout = {},
        )
    }
}

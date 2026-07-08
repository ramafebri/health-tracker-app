package com.rama.health.ui.dashboard

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.service.StepCounterService
import com.rama.health.ui.dashboard.components.PermissionRequestCard
import com.rama.health.ui.dashboard.components.StepCounterCard
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.util.PermissionUtils

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun startTrackingService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, StepCounterService::class.java),
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val activityRecognitionGranted = result[Manifest.permission.ACTIVITY_RECOGNITION]
            ?: PermissionUtils.hasActivityRecognitionPermission(context)
        val notificationsGranted = result[Manifest.permission.POST_NOTIFICATIONS]
            ?: PermissionUtils.hasPostNotificationsPermission(context)
        viewModel.onPermissionsChecked(
            activityRecognitionGranted = activityRecognitionGranted,
            notificationsGranted = notificationsGranted,
        )
        if (activityRecognitionGranted) {
            startTrackingService()
            viewModel.onTrackingStarted()
        }
    }

    LaunchedEffect(Unit) {
        val activityRecognitionGranted = PermissionUtils.hasActivityRecognitionPermission(context)
        val notificationsGranted = PermissionUtils.hasPostNotificationsPermission(context)
        viewModel.onPermissionsChecked(
            activityRecognitionGranted = activityRecognitionGranted,
            notificationsGranted = notificationsGranted,
        )
        if (activityRecognitionGranted) {
            startTrackingService()
            viewModel.onTrackingStarted()
        }
    }

    DashboardContent(
        uiState = uiState,
        onGrantPermissionClick = { permissionLauncher.launch(PermissionUtils.requiredRuntimePermissions()) },
        onGoalChanged = viewModel::onGoalChanged,
        onNavigateToHistory = onNavigateToHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onGrantPermissionClick: () -> Unit,
    onGoalChanged: (Int) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var goalInput by remember(uiState.dailyGoal) { mutableStateOf(uiState.dailyGoal.toString()) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Step Counter",
                style = MaterialTheme.typography.headlineMedium,
            )

            if (!uiState.hasActivityRecognitionPermission || !uiState.hasNotificationPermission) {
                PermissionRequestCard(onRequestClick = onGrantPermissionClick)
            }

            StepCounterCard(
                todaySteps = uiState.todaySteps,
                goal = uiState.dailyGoal,
            )

            Text(
                text = if (uiState.isTrackingActive) "Tracking active" else "Tracking paused",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Daily Goal",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Steps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onGoalChanged(goalInput.toIntOrNull() ?: uiState.dailyGoal) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Goal")
                }
            }

            TextButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View History")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    HealthTrackerAppTheme {
        DashboardContent(
            uiState = DashboardUiState(
                todaySteps = 4213,
                dailyGoal = 10_000,
                hasActivityRecognitionPermission = true,
                hasNotificationPermission = true,
                isTrackingActive = true,
            ),
            onGrantPermissionClick = {},
            onGoalChanged = {},
            onNavigateToHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPermissionsMissingPreview() {
    HealthTrackerAppTheme {
        DashboardContent(
            uiState = DashboardUiState(
                todaySteps = 0,
                dailyGoal = 10_000,
                hasActivityRecognitionPermission = false,
                hasNotificationPermission = false,
                isTrackingActive = false,
            ),
            onGrantPermissionClick = {},
            onGoalChanged = {},
            onNavigateToHistory = {},
        )
    }
}

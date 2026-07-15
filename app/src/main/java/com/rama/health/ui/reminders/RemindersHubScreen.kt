package com.rama.health.ui.reminders

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.R
import com.rama.health.ui.reminders.components.ReminderPermissionBanner
import com.rama.health.ui.reminders.components.ReminderPermissionBannerType
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.util.ExactAlarmUtils
import com.rama.health.util.PermissionUtils

@Composable
fun RemindersHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWaterReminder: () -> Unit,
    onNavigateToMedicationList: () -> Unit,
    viewModel: RemindersHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionsChecked(
            hasNotificationPermission = granted,
            hasExactAlarmPermission = PermissionUtils.hasExactAlarmPermission(context),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionsChecked(
            hasNotificationPermission = PermissionUtils.hasPostNotificationsPermission(context),
            hasExactAlarmPermission = PermissionUtils.hasExactAlarmPermission(context),
        )
    }

    RemindersHubContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToWaterReminder = onNavigateToWaterReminder,
        onNavigateToMedicationList = onNavigateToMedicationList,
        onRequestNotificationPermission = {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenExactAlarmSettings = { ExactAlarmUtils.openExactAlarmSettings(context) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersHubContent(
    uiState: RemindersHubUiState,
    onNavigateBack: () -> Unit,
    onNavigateToWaterReminder: () -> Unit,
    onNavigateToMedicationList: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!uiState.hasNotificationPermission) {
                        ReminderPermissionBanner(
                            type = ReminderPermissionBannerType.NOTIFICATIONS,
                            onActionClick = onRequestNotificationPermission,
                        )
                    }

                    if (!uiState.hasExactAlarmPermission) {
                        ReminderPermissionBanner(
                            type = ReminderPermissionBannerType.EXACT_ALARM,
                            onActionClick = onOpenExactAlarmSettings,
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToWaterReminder),
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.reminders_hub_water_card_title))
                            },
                            supportingContent = {
                                Text(
                                    text = if (uiState.waterEnabled) {
                                        uiState.waterSummary.ifEmpty {
                                            stringResource(R.string.reminders_hub_water_card_subtitle)
                                        }
                                    } else {
                                        stringResource(R.string.reminders_hub_water_empty)
                                    },
                                )
                            },
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToMedicationList),
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.reminders_hub_medication_card_title))
                            },
                            supportingContent = {
                                Text(
                                    text = if (uiState.medicationCount == 0) {
                                        stringResource(R.string.reminders_hub_medication_empty)
                                    } else {
                                        stringResource(
                                            R.string.reminders_hub_medication_card_subtitle,
                                        ) + " · ${uiState.medicationCount}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersHubContentPreview() {
    HealthTrackerAppTheme {
        RemindersHubContent(
            uiState = RemindersHubUiState(
                waterEnabled = true,
                waterSummary = "60m",
                medicationCount = 2,
                hasNotificationPermission = false,
                hasExactAlarmPermission = true,
                isLoading = false,
            ),
            onNavigateBack = {},
            onNavigateToWaterReminder = {},
            onNavigateToMedicationList = {},
            onRequestNotificationPermission = {},
            onOpenExactAlarmSettings = {},
        )
    }
}

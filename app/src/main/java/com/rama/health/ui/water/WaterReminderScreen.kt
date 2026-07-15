package com.rama.health.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.R
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.ui.reminders.ReminderFormatters
import com.rama.health.ui.reminders.components.ReminderTimePickerDialog
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.ui.water.components.WaterFixedTimesEditor
import com.rama.health.ui.water.components.WaterGoalCard
import com.rama.health.ui.water.components.WaterIntervalPicker
import com.rama.health.ui.water.components.WaterScheduleModeSelector

@Composable
fun WaterReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: WaterReminderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WaterReminderContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEnabledChanged = viewModel::onEnabledChanged,
        onScheduleModeChanged = viewModel::onScheduleModeChanged,
        onIntervalChanged = viewModel::onIntervalChanged,
        onActiveStartChanged = viewModel::onActiveStartChanged,
        onActiveEndChanged = viewModel::onActiveEndChanged,
        onFixedTimesChanged = viewModel::onFixedTimesChanged,
        onDailyGoalInputChanged = viewModel::onDailyGoalInputChanged,
        onDailyGoalSaved = viewModel::onDailyGoalSaved,
        onLogWater = viewModel::onLogWater,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterReminderContent(
    uiState: WaterReminderUiState,
    onNavigateBack: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onScheduleModeChanged: (WaterScheduleMode) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onActiveStartChanged: (Int) -> Unit,
    onActiveEndChanged: (Int) -> Unit,
    onFixedTimesChanged: (List<MedicationTime>) -> Unit,
    onDailyGoalInputChanged: (String) -> Unit,
    onDailyGoalSaved: () -> Unit,
    onLogWater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.water_reminder_title)) },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.water_reminder_enabled_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Switch(
                            checked = uiState.enabled,
                            onCheckedChange = onEnabledChanged,
                        )
                    }

                    uiState.validationError?.let { error ->
                        Text(
                            text = stringResource(error.toMessageRes()),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (uiState.enabled) {
                        WaterScheduleModeSelector(
                            selectedMode = uiState.scheduleMode,
                            onModeSelected = onScheduleModeChanged,
                        )

                        when (uiState.scheduleMode) {
                            WaterScheduleMode.INTERVAL -> {
                                WaterIntervalPicker(
                                    selectedMinutes = uiState.intervalMinutes,
                                    onIntervalSelected = onIntervalChanged,
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.water_reminder_active_hours_label),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { showStartPicker = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                stringResource(R.string.water_reminder_active_hours_start_label) +
                                                    ": " +
                                                    ReminderFormatters.formatMinutesFromMidnight(
                                                        uiState.activeStartMinutes,
                                                    ),
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { showEndPicker = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                stringResource(R.string.water_reminder_active_hours_end_label) +
                                                    ": " +
                                                    ReminderFormatters.formatMinutesFromMidnight(
                                                        uiState.activeEndMinutes,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }

                            WaterScheduleMode.FIXED_TIMES -> {
                                WaterFixedTimesEditor(
                                    times = uiState.fixedTimes,
                                    onTimesChanged = onFixedTimesChanged,
                                )
                            }
                        }
                    }

                    WaterGoalCard(
                        dailyGoalInput = uiState.dailyGoalInput,
                        todayIntakeMl = uiState.todayIntakeMl,
                        onDailyGoalInputChanged = onDailyGoalInputChanged,
                        onDailyGoalSaved = onDailyGoalSaved,
                        onLogWater = onLogWater,
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        ReminderTimePickerDialog(
            initialTime = MedicationTime(
                uiState.activeStartMinutes / 60,
                uiState.activeStartMinutes % 60,
            ),
            onDismiss = { showStartPicker = false },
            onConfirm = { time ->
                onActiveStartChanged(time.hour * 60 + time.minute)
                showStartPicker = false
            },
        )
    }

    if (showEndPicker) {
        ReminderTimePickerDialog(
            initialTime = MedicationTime(
                uiState.activeEndMinutes / 60,
                uiState.activeEndMinutes % 60,
            ),
            onDismiss = { showEndPicker = false },
            onConfirm = { time ->
                onActiveEndChanged(time.hour * 60 + time.minute)
                showEndPicker = false
            },
        )
    }
}

private fun WaterReminderValidationError.toMessageRes(): Int = when (this) {
    WaterReminderValidationError.INTERVAL_INVALID -> R.string.water_reminder_error_interval_invalid
    WaterReminderValidationError.ACTIVE_HOURS_INVALID -> R.string.water_reminder_error_active_hours_invalid
    WaterReminderValidationError.FIXED_TIMES_EMPTY -> R.string.water_reminder_error_fixed_times_empty
    WaterReminderValidationError.DAILY_GOAL_INVALID -> R.string.water_reminder_error_daily_goal_invalid
}

@Preview(showBackground = true)
@Composable
private fun WaterReminderContentPreview() {
    HealthTrackerAppTheme {
        WaterReminderContent(
            uiState = WaterReminderUiState(
                enabled = true,
                scheduleMode = WaterScheduleMode.INTERVAL,
                intervalMinutes = 60,
                todayIntakeMl = 500,
                dailyGoalInput = "2000",
                isLoading = false,
            ),
            onNavigateBack = {},
            onEnabledChanged = {},
            onScheduleModeChanged = {},
            onIntervalChanged = {},
            onActiveStartChanged = {},
            onActiveEndChanged = {},
            onFixedTimesChanged = {},
            onDailyGoalInputChanged = {},
            onDailyGoalSaved = {},
            onLogWater = {},
        )
    }
}

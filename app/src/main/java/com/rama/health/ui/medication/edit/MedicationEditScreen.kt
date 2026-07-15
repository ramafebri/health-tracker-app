package com.rama.health.ui.medication.edit

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.R
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.ui.medication.components.MedicationTimesEditor
import com.rama.health.ui.medication.components.RepeatDaysSelector
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun MedicationEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicationEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) {
            viewModel.onSaveHandled()
            onNavigateBack()
        }
    }

    MedicationEditContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNameChanged = viewModel::onNameChanged,
        onDosageChanged = viewModel::onDosageChanged,
        onEnabledChanged = viewModel::onEnabledChanged,
        onDayToggled = viewModel::toggleDay,
        onTimesChanged = viewModel::onTimesChanged,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditContent(
    uiState: MedicationEditUiState,
    onNavigateBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDosageChanged: (String) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onDayToggled: (java.time.DayOfWeek) -> Unit,
    onTimesChanged: (List<MedicationTime>) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = if (uiState.isCreateMode) {
        R.string.medication_edit_title_create
    } else {
        R.string.medication_edit_title_edit
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isSaving) {
                        Text(stringResource(R.string.medication_edit_save))
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
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChanged,
                        label = { Text(stringResource(R.string.medication_edit_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.dosage,
                        onValueChange = onDosageChanged,
                        label = { Text(stringResource(R.string.medication_edit_dosage_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.medication_edit_enabled_label),
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

                    RepeatDaysSelector(
                        repeatDays = uiState.repeatDays,
                        onDayToggled = onDayToggled,
                    )

                    MedicationTimesEditor(
                        times = uiState.times,
                        onTimesChanged = onTimesChanged,
                    )

                    Button(
                        onClick = onSave,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.medication_edit_save))
                    }

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.medication_edit_cancel))
                    }
                }
            }
        }
    }
}

private fun MedicationEditValidationError.toMessageRes(): Int = when (this) {
    MedicationEditValidationError.NAME_REQUIRED -> R.string.medication_edit_error_name_required
    MedicationEditValidationError.TIMES_REQUIRED -> R.string.medication_edit_error_times_required
    MedicationEditValidationError.REPEAT_DAYS_REQUIRED -> R.string.medication_edit_error_repeat_days_required
    MedicationEditValidationError.DUPLICATE_TIME -> R.string.medication_edit_error_duplicate_time
}

@Preview(showBackground = true)
@Composable
private fun MedicationEditContentPreview() {
    HealthTrackerAppTheme {
        MedicationEditContent(
            uiState = MedicationEditUiState(
                isCreateMode = true,
                name = "Ibuprofen",
                dosage = "200mg",
                enabled = true,
                repeatDays = RepeatDays.allDays(),
                times = listOf(MedicationTime(8, 0)),
                isLoading = false,
            ),
            onNavigateBack = {},
            onNameChanged = {},
            onDosageChanged = {},
            onEnabledChanged = {},
            onDayToggled = {},
            onTimesChanged = {},
            onSave = {},
        )
    }
}

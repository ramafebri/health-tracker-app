package com.rama.health.ui.medication.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.R
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.ui.medication.components.MedicationListItem
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun MedicationListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MedicationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MedicationListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = onNavigateToEdit,
        onToggleEnabled = viewModel::onToggleEnabled,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListContent(
    uiState: MedicationListUiState,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDeleteRequested: (MedicationReminder) -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medication_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(com.rama.health.ui.navigation.NavRoutes.NEW_MEDICATION_ID) },
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.medication_list_fab_label),
                )
            }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    if (uiState.medications.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.medication_list_empty_state),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    } else {
                        items(uiState.medications, key = { it.id }) { medication ->
                            MedicationListItem(
                                medication = medication,
                                onClick = { onNavigateToEdit(medication.id) },
                                onEnabledChanged = { enabled ->
                                    onToggleEnabled(medication.id, enabled)
                                },
                                onDeleteClick = { onDeleteRequested(medication) },
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.medicationPendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.medication_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.medication_delete_confirm_message,
                        medication.name,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) {
                    Text(stringResource(R.string.medication_delete_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed) {
                    Text(stringResource(R.string.medication_delete_confirm_cancel))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationListContentPreview() {
    HealthTrackerAppTheme {
        MedicationListContent(
            uiState = MedicationListUiState(
                medications = listOf(
                    MedicationReminder(
                        id = "1",
                        name = "Aspirin",
                        dosage = "81mg",
                        enabled = true,
                        repeatDays = RepeatDays.allDays(),
                        times = listOf(MedicationTime(8, 0)),
                    ),
                ),
                isLoading = false,
            ),
            onNavigateBack = {},
            onNavigateToEdit = {},
            onToggleEnabled = { _, _ -> },
            onDeleteRequested = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {},
        )
    }
}

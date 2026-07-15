package com.rama.health.ui.medication.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.rama.health.R
import com.rama.health.domain.model.MedicationTime
import com.rama.health.ui.reminders.ReminderFormatters
import com.rama.health.ui.reminders.components.ReminderTimePickerDialog
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun MedicationTimesEditor(
    times: List<MedicationTime>,
    onTimesChanged: (List<MedicationTime>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showAddPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_edit_times_label),
            style = MaterialTheme.typography.titleMedium,
        )

        times.forEachIndexed { index, time ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = { editingIndex = index }) {
                    Text(ReminderFormatters.formatTime(time))
                }
                IconButton(
                    onClick = {
                        onTimesChanged(times.filterIndexed { i, _ -> i != index })
                    },
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.medication_edit_remove_time),
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showAddPicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.medication_edit_add_time))
        }
    }

    editingIndex?.let { index ->
        val time = times.getOrNull(index) ?: return@let
        ReminderTimePickerDialog(
            initialTime = time,
            onDismiss = { editingIndex = null },
            onConfirm = { updated ->
                onTimesChanged(times.mapIndexed { i, t -> if (i == index) updated else t })
                editingIndex = null
            },
        )
    }

    if (showAddPicker) {
        ReminderTimePickerDialog(
            initialTime = MedicationTime(8, 0),
            onDismiss = { showAddPicker = false },
            onConfirm = { newTime ->
                onTimesChanged((times + newTime).sortedWith(compareBy({ it.hour }, { it.minute })))
                showAddPicker = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationTimesEditorPreview() {
    HealthTrackerAppTheme {
        MedicationTimesEditor(
            times = listOf(MedicationTime(8, 0), MedicationTime(20, 0)),
            onTimesChanged = {},
        )
    }
}

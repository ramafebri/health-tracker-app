package com.rama.health.ui.reminders.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rama.health.R
import com.rama.health.domain.model.MedicationTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    initialTime: MedicationTime,
    onDismiss: () -> Unit,
    onConfirm: (MedicationTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(MedicationTime(hour = state.hour, minute = state.minute))
                },
            ) {
                Text(stringResource(R.string.medication_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.medication_edit_cancel))
            }
        },
        text = {
            TimePicker(state = state)
        },
    )
}

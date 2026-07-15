package com.rama.health.ui.medication.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rama.health.R
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.ui.reminders.ReminderFormatters
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun MedicationListItem(
    medication: MedicationReminder,
    onClick: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = { Text(medication.name) },
        supportingContent = {
            val dosage = medication.dosage
            val times = ReminderFormatters.formatTimesSummary(medication.times)
            val days = ReminderFormatters.formatRepeatDays(medication.repeatDays)
            Text(
                text = buildString {
                    if (!dosage.isNullOrBlank()) {
                        append(dosage)
                        append(" · ")
                    }
                    append(times)
                    if (days.isNotBlank()) {
                        append(" · ")
                        append(days)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            Switch(
                checked = medication.enabled,
                onCheckedChange = onEnabledChanged,
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.medication_delete_confirm_delete),
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MedicationListItemPreview() {
    HealthTrackerAppTheme {
        MedicationListItem(
            medication = MedicationReminder(
                id = "1",
                name = "Vitamin D",
                dosage = "1000 IU",
                enabled = true,
                repeatDays = RepeatDays.allDays(),
                times = listOf(MedicationTime(8, 0), MedicationTime(20, 0)),
            ),
            onClick = {},
            onEnabledChanged = {},
            onDeleteClick = {},
        )
    }
}

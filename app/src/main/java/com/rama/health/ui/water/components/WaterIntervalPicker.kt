package com.rama.health.ui.water.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.R
import com.rama.health.ui.reminders.ReminderFormatters
import com.rama.health.ui.theme.HealthTrackerAppTheme

val WATER_INTERVAL_OPTIONS = listOf(30, 60, 90, 120, 180, 240)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterIntervalPicker(
    selectedMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.water_reminder_interval_label),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WATER_INTERVAL_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onIntervalSelected(minutes) },
                    label = { Text(ReminderFormatters.formatIntervalMinutes(minutes)) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterIntervalPickerPreview() {
    HealthTrackerAppTheme {
        WaterIntervalPicker(
            selectedMinutes = 60,
            onIntervalSelected = {},
        )
    }
}

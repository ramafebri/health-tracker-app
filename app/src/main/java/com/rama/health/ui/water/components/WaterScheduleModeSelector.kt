package com.rama.health.ui.water.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun WaterScheduleModeSelector(
    selectedMode: WaterScheduleMode,
    onModeSelected: (WaterScheduleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.water_reminder_schedule_mode_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedMode == WaterScheduleMode.INTERVAL,
                onClick = { onModeSelected(WaterScheduleMode.INTERVAL) },
                label = { Text(stringResource(R.string.water_reminder_schedule_mode_interval)) },
            )
            FilterChip(
                selected = selectedMode == WaterScheduleMode.FIXED_TIMES,
                onClick = { onModeSelected(WaterScheduleMode.FIXED_TIMES) },
                label = { Text(stringResource(R.string.water_reminder_schedule_mode_fixed_times)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterScheduleModeSelectorPreview() {
    HealthTrackerAppTheme {
        WaterScheduleModeSelector(
            selectedMode = WaterScheduleMode.INTERVAL,
            onModeSelected = {},
        )
    }
}

package com.rama.health.ui.medication.components

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
import com.rama.health.domain.model.RepeatDays
import com.rama.health.ui.theme.HealthTrackerAppTheme
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepeatDaysSelector(
    repeatDays: RepeatDays,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_edit_repeat_days_label),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = repeatDays.contains(day),
                    onClick = { onDayToggled(day) },
                    label = { Text(dayLabel(day)) },
                )
            }
        }
    }
}

@Composable
private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.medication_day_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.medication_day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.medication_day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.medication_day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.medication_day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.medication_day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.medication_day_sun)
}

@Preview(showBackground = true)
@Composable
private fun RepeatDaysSelectorPreview() {
    HealthTrackerAppTheme {
        RepeatDaysSelector(
            repeatDays = RepeatDays.allDays(),
            onDayToggled = {},
        )
    }
}

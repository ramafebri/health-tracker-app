package com.rama.health.ui.water.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.R
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun WaterGoalCard(
    dailyGoalInput: String,
    todayIntakeMl: Int,
    onDailyGoalInputChanged: (String) -> Unit,
    onDailyGoalSaved: () -> Unit,
    onLogWater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.water_reminder_today_intake_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.water_reminder_daily_goal_ml, todayIntakeMl),
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = dailyGoalInput,
                onValueChange = onDailyGoalInputChanged,
                label = { Text(stringResource(R.string.water_reminder_daily_goal_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onDailyGoalSaved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.medication_edit_save))
            }

            Button(
                onClick = onLogWater,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.water_reminder_log_water_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterGoalCardPreview() {
    HealthTrackerAppTheme {
        WaterGoalCard(
            dailyGoalInput = "2000",
            todayIntakeMl = 750,
            onDailyGoalInputChanged = {},
            onDailyGoalSaved = {},
            onLogWater = {},
        )
    }
}

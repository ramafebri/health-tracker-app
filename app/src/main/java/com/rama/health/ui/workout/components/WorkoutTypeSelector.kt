package com.rama.health.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.domain.model.WorkoutType
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun WorkoutTypeSelector(
    selectedType: WorkoutType?,
    onTypeSelected: (WorkoutType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Workout type", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkoutType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.titlecase() }) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutTypeSelectorPreview() {
    HealthTrackerAppTheme {
        WorkoutTypeSelector(
            selectedType = WorkoutType.RUN,
            onTypeSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

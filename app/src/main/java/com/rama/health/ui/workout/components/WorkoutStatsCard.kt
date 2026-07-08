package com.rama.health.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.domain.model.WorkoutType
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.ui.workout.WorkoutFormatters

@Composable
fun WorkoutStatsCard(
    workoutType: WorkoutType?,
    elapsedSeconds: Long,
    distanceMeters: Double,
    avgPaceSecPerKm: Int?,
    avgSpeedKmh: Double?,
    elevationGainMeters: Double,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = "Duration", value = WorkoutFormatters.formatDuration(elapsedSeconds))
                StatItem(label = "Distance", value = WorkoutFormatters.formatDistanceKm(distanceMeters))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val paceOrSpeed = when (workoutType) {
                    WorkoutType.CYCLE -> WorkoutFormatters.formatSpeedKmh(avgSpeedKmh)
                    WorkoutType.RUN, WorkoutType.WALK, null -> WorkoutFormatters.formatPace(avgPaceSecPerKm)
                }
                val paceLabel = if (workoutType == WorkoutType.CYCLE) "Speed" else "Pace"
                StatItem(label = paceLabel, value = paceOrSpeed)
                StatItem(
                    label = "Elevation",
                    value = WorkoutFormatters.formatElevation(elevationGainMeters),
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutStatsCardPreview() {
    HealthTrackerAppTheme {
        WorkoutStatsCard(
            workoutType = WorkoutType.RUN,
            elapsedSeconds = 1834,
            distanceMeters = 4200.0,
            avgPaceSecPerKm = 437,
            avgSpeedKmh = null,
            elevationGainMeters = 42.0,
            modifier = Modifier.padding(16.dp),
        )
    }
}

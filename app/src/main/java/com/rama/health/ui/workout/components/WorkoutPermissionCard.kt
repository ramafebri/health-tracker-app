package com.rama.health.ui.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.ui.theme.HealthTrackerAppTheme

@Composable
fun WorkoutPermissionCard(
    onRequestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Location permission required",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Grant location and notification access to track your route with GPS.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequestClick) {
                Text("Grant permissions")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutPermissionCardPreview() {
    HealthTrackerAppTheme {
        WorkoutPermissionCard(onRequestClick = {})
    }
}

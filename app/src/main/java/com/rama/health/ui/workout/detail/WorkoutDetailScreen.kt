package com.rama.health.ui.workout.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rama.health.domain.model.RoutePoint
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutType
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.ui.workout.WorkoutFormatters
import com.rama.health.ui.workout.components.WorkoutMap
import com.rama.health.ui.workout.components.WorkoutStatsCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailContent(
    uiState: WorkoutDetailUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Workout Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading || uiState.workout == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val workout = uiState.workout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WorkoutHeader(workout)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    ) {
                        WorkoutMap(routePoints = uiState.routePoints)
                    }
                    WorkoutStatsCard(
                        workoutType = workout.type,
                        elapsedSeconds = workout.durationSeconds,
                        distanceMeters = workout.distanceMeters,
                        avgPaceSecPerKm = workout.avgPaceSecPerKm,
                        avgSpeedKmh = workout.avgSpeedKmh,
                        elevationGainMeters = workout.elevationGainMeters,
                    )
                    if (workout.minAltitude != null && workout.maxAltitude != null) {
                        Text(
                            text = "Altitude: ${WorkoutFormatters.formatElevation(workout.minAltitude)} – ${WorkoutFormatters.formatElevation(workout.maxAltitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHeader(workout: WorkoutRecord) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMM d · HH:mm", Locale.getDefault())
    }
    val dateTime = remember(workout.startTime) {
        Instant.ofEpochMilli(workout.startTime)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }
    val typeLabel = workout.type.name.lowercase().replaceFirstChar { it.titlecase() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = typeLabel, style = MaterialTheme.typography.headlineSmall)
        Text(text = dateTime, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailContentPreview() {
    HealthTrackerAppTheme {
        WorkoutDetailContent(
            uiState = WorkoutDetailUiState(
                workout = WorkoutRecord(
                    id = "1",
                    type = WorkoutType.CYCLE,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    durationSeconds = 3600,
                    distanceMeters = 25_000.0,
                    avgPaceSecPerKm = null,
                    avgSpeedKmh = 25.0,
                    elevationGainMeters = 120.0,
                    minAltitude = 50.0,
                    maxAltitude = 170.0,
                ),
                routePoints = listOf(
                    RoutePoint(37.7749, -122.4194, 10.0, 0L),
                    RoutePoint(37.7759, -122.4184, 15.0, 60_000L),
                ),
                isLoading = false,
            ),
            onNavigateBack = {},
        )
    }
}

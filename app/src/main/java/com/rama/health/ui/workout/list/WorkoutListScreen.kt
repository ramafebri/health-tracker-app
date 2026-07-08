package com.rama.health.ui.workout.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rama.health.domain.model.WorkoutRecord
import com.rama.health.domain.model.WorkoutType
import com.rama.health.ui.theme.HealthTrackerAppTheme
import com.rama.health.ui.workout.WorkoutFormatters
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: WorkoutListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToActiveWorkout = onNavigateToActiveWorkout,
        onNavigateToDetail = onNavigateToDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListContent(
    uiState: WorkoutListUiState,
    onNavigateBack: () -> Unit,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Workout Logger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToActiveWorkout) {
                Icon(Icons.Default.Add, contentDescription = "New workout")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    if (uiState.hasActiveWorkout) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable(onClick = onNavigateToActiveWorkout),
                            ) {
                                ListItem(
                                    headlineContent = { Text("Workout in progress") },
                                    supportingContent = { Text("Tap to resume tracking") },
                                )
                            }
                        }
                    }

                    if (uiState.workouts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No workouts yet — start your first run, walk, or ride!",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    } else {
                        items(uiState.workouts, key = { it.id }) { workout ->
                            WorkoutListItem(
                                workout = workout,
                                onClick = { onNavigateToDetail(workout.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutListItem(
    workout: WorkoutRecord,
    onClick: () -> Unit,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.getDefault())
    }
    val dateTime = remember(workout.startTime) {
        Instant.ofEpochMilli(workout.startTime)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }
    val typeLabel = workout.type.name.lowercase().replaceFirstChar { it.titlecase() }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text("$typeLabel · ${WorkoutFormatters.formatDistanceKm(workout.distanceMeters)}") },
        supportingContent = { Text(dateTime) },
        trailingContent = { Text(WorkoutFormatters.formatDuration(workout.durationSeconds)) },
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkoutListContentPreview() {
    HealthTrackerAppTheme {
        WorkoutListContent(
            uiState = WorkoutListUiState(
                workouts = listOf(
                    WorkoutRecord(
                        id = "1",
                        type = WorkoutType.RUN,
                        startTime = System.currentTimeMillis(),
                        endTime = System.currentTimeMillis(),
                        durationSeconds = 2400,
                        distanceMeters = 5200.0,
                        avgPaceSecPerKm = 461,
                        avgSpeedKmh = null,
                        elevationGainMeters = 35.0,
                        minAltitude = 10.0,
                        maxAltitude = 45.0,
                    ),
                ),
                hasActiveWorkout = false,
                isLoading = false,
            ),
            onNavigateBack = {},
            onNavigateToActiveWorkout = {},
            onNavigateToDetail = {},
        )
    }
}

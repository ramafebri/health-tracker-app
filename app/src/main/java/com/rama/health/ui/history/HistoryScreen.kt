package com.rama.health.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.ui.theme.HealthTrackerAppTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Step History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No step history yet — start walking!",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(uiState.records, key = { it.date.toString() }) { record ->
                        DailyStepRecordRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStepRecordRow(record: DailyStepRecord) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()) }
    val stepsFormatter = remember { NumberFormat.getIntegerInstance() }

    ListItem(
        headlineContent = { Text(record.date.format(dateFormatter)) },
        trailingContent = { Text("${stepsFormatter.format(record.steps)} steps") },
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    HealthTrackerAppTheme {
        HistoryContent(
            uiState = HistoryUiState(
                records = listOf(
                    DailyStepRecord(date = LocalDate.now(), steps = 5234),
                    DailyStepRecord(date = LocalDate.now().minusDays(1), steps = 8721),
                    DailyStepRecord(date = LocalDate.now().minusDays(2), steps = 3012),
                    DailyStepRecord(date = LocalDate.now().minusDays(3), steps = 10_540),
                ),
                isLoading = false,
            ),
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentEmptyPreview() {
    HealthTrackerAppTheme {
        HistoryContent(
            uiState = HistoryUiState(records = emptyList(), isLoading = false),
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentLoadingPreview() {
    HealthTrackerAppTheme {
        HistoryContent(
            uiState = HistoryUiState(records = emptyList(), isLoading = true),
            onNavigateBack = {},
        )
    }
}

package com.rama.health.ui.history

import app.cash.turbine.test
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.domain.usecase.ObserveStepHistoryUseCase
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [HistoryViewModel], with [ObserveStepHistoryUseCase] fully mocked via MockK
 * (no repository is constructed).
 */
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeStepHistory = mockk<ObserveStepHistoryUseCase>()

    private fun viewModel() = HistoryViewModel(observeStepHistory)

    @Test
    fun uiState_emitsRecordsAndStopsLoading_whenHistoryIsNonEmpty() = runTest {
        val sampleRecords = listOf(
            DailyStepRecord(date = LocalDate.of(2026, 7, 6), steps = 3_000),
            DailyStepRecord(date = LocalDate.of(2026, 7, 7), steps = 5_000),
        )
        every { observeStepHistory() } returns flowOf(sampleRecords)

        viewModel().uiState.test {
            // stateIn's initialValue (isLoading = true, records = empty) is emitted first,
            // before the mapped mocked flow has had a chance to run on the test dispatcher.
            skipItems(1)

            val state = awaitItem()
            assertEquals(sampleRecords, state.records)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun uiState_emitsEmptyRecordsAndStopsLoading_whenHistoryIsEmpty() = runTest {
        every { observeStepHistory() } returns flowOf(emptyList())

        viewModel().uiState.test {
            skipItems(1) // initial default state (isLoading = true)

            val state = awaitItem()
            assertTrue(state.records.isEmpty())
            assertFalse(state.isLoading)
        }
    }
}

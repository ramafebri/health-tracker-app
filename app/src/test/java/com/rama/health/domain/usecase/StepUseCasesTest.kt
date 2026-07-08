package com.rama.health.domain.usecase

import com.rama.health.domain.model.DailyStepRecord
import com.rama.health.domain.repository.StepRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies each step-tracking use case is a thin pass-through to [StepRepository], with no
 * logic of its own to diverge from the mocked repository's behavior.
 */
class StepUseCasesTest {

    private val repository = mockk<StepRepository>()

    @Test
    fun observeTodaySteps_delegatesToRepository() = runTest {
        every { repository.observeTodaySteps() } returns flowOf(4200)
        val useCase = ObserveTodayStepsUseCase(repository)
        assertEquals(4200, useCase().first())
    }

    @Test
    fun observeStepHistory_delegatesToRepository() = runTest {
        val records = listOf(DailyStepRecord(LocalDate.of(2026, 7, 7), 5000))
        every { repository.observeHistory() } returns flowOf(records)
        val useCase = ObserveStepHistoryUseCase(repository)
        assertEquals(records, useCase().first())
    }

    @Test
    fun observeDailyGoal_delegatesToRepository() = runTest {
        every { repository.observeDailyGoal() } returns flowOf(8000)
        val useCase = ObserveDailyGoalUseCase(repository)
        assertEquals(8000, useCase().first())
    }

    @Test
    fun observeTrackingEnabled_delegatesToRepository() = runTest {
        every { repository.observeTrackingEnabled() } returns flowOf(true)
        val useCase = ObserveTrackingEnabledUseCase(repository)
        assertEquals(true, useCase().first())
    }

    @Test
    fun setDailyGoal_delegatesToRepository() = runTest {
        coEvery { repository.setDailyGoal(any()) } returns Unit
        val useCase = SetDailyGoalUseCase(repository)
        useCase(9000)
        coVerify { repository.setDailyGoal(9000) }
    }

    @Test
    fun setTrackingEnabled_delegatesToRepository() = runTest {
        coEvery { repository.setTrackingEnabled(any()) } returns Unit
        val useCase = SetTrackingEnabledUseCase(repository)
        useCase(true)
        coVerify { repository.setTrackingEnabled(true) }
    }
}

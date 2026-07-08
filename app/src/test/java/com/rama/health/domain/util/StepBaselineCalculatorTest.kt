package com.rama.health.domain.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [StepBaselineCalculator], covering the reboot detection, day rollover,
 * and same-day normal diff branches (checked in that priority order).
 */
class StepBaselineCalculatorTest {

    @Test
    fun sameDay_normalDelta_returnsDifferenceAndKeepsBaseline() {
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 1250L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 1000L,
            baselineDate = LocalDate.of(2026, 7, 7),
            baselineOffsetSteps = 0,
            existingTodaySteps = 250,
        )

        assertEquals(250, result.todaySteps)
        assertEquals(1000L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(0, result.newBaselineOffsetSteps)
    }

    @Test
    fun sameDay_withNonZeroOffset_addsOffsetToRawDelta() {
        // Simulates a baseline that was re-captured mid-day after a reboot recovery, with 400
        // steps already credited (the offset) before the baseline's reference point.
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 120L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 100L,
            baselineDate = LocalDate.of(2026, 7, 7),
            baselineOffsetSteps = 400,
            existingTodaySteps = 400,
        )

        assertEquals(420, result.todaySteps)
        assertEquals(100L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(400, result.newBaselineOffsetSteps)
    }

    @Test
    fun dayRollover_resetsBaselineAndStartsTodayAtZero() {
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 5300L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 5000L,
            baselineDate = LocalDate.of(2026, 7, 6),
            baselineOffsetSteps = 200,
            existingTodaySteps = 0,
        )

        assertEquals(0, result.todaySteps)
        assertEquals(5300L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(0, result.newBaselineOffsetSteps)
    }

    @Test
    fun rebootDetected_midDay_preservesExistingStepsAsNewOffset() {
        // Existing persisted total for today (800) must survive a same-day reboot: the sensor's
        // cumulative reading resets to a small post-reboot value (150), which must NOT overwrite
        // today's total with a smaller number, and must become the new offset going forward.
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 150L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 8000L,
            baselineDate = LocalDate.of(2026, 7, 7),
            baselineOffsetSteps = 0,
            existingTodaySteps = 800,
        )

        assertEquals(800, result.todaySteps)
        assertEquals(150L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(800, result.newBaselineOffsetSteps)
    }

    @Test
    fun rebootDetected_midDay_subsequentReadingAccumulatesOnTopOfPreservedOffset() {
        // Follow-up reading after the reboot-recovery baseline above: cumulative advances from
        // 150 to 170 (20 post-reboot steps), which must land on top of the preserved 800, not
        // reset or ignore it.
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 170L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 150L,
            baselineDate = LocalDate.of(2026, 7, 7),
            baselineOffsetSteps = 800,
            existingTodaySteps = 800,
        )

        assertEquals(820, result.todaySteps)
        assertEquals(150L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(800, result.newBaselineOffsetSteps)
    }

    @Test
    fun zeroDelta_returnsZeroSteps_neverNegative() {
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 2000L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 2000L,
            baselineDate = LocalDate.of(2026, 7, 7),
            baselineOffsetSteps = 0,
            existingTodaySteps = 0,
        )

        assertEquals(0, result.todaySteps)
        assertEquals(2000L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(0, result.newBaselineOffsetSteps)
    }

    @Test
    fun rebootDetected_onNewDay_takesPriorityOverDayRollover_andStartsNewDayAtZero() {
        // The new day has no existing persisted steps yet, so even though this is detected via
        // the reboot branch (priority order), the result must match a fresh day starting at 0 --
        // NOT the raw post-reboot cumulative reading, which would otherwise leak an arbitrary
        // hardware value into the new day's total.
        val result = StepBaselineCalculator.computeTodaySteps(
            currentCumulative = 150L,
            currentDate = LocalDate.of(2026, 7, 7),
            baselineValue = 8000L,
            baselineDate = LocalDate.of(2026, 7, 6),
            baselineOffsetSteps = 0,
            existingTodaySteps = 0,
        )

        assertEquals(0, result.todaySteps)
        assertEquals(150L, result.newBaselineValue)
        assertEquals(LocalDate.of(2026, 7, 7), result.newBaselineDate)
        assertEquals(0, result.newBaselineOffsetSteps)
    }
}

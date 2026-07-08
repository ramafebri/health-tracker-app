package com.rama.health.domain.util

import java.time.LocalDate

data class StepBaselineResult(
    val todaySteps: Int,
    val newBaselineValue: Long,
    val newBaselineDate: LocalDate,
    val newBaselineOffsetSteps: Int,
)

object StepBaselineCalculator {

    /**
     * Derives today's step count from the raw cumulative hardware step counter reading,
     * reconciling it against the last known baseline.
     *
     * The baseline is a triple of ([baselineValue], [baselineDate], [baselineOffsetSteps]):
     * [baselineOffsetSteps] is how many steps were already credited to [baselineDate] at the
     * moment [baselineValue] was captured, so that a same-day reset of the hardware counter
     * (e.g. a reboot) never loses previously-earned steps for the day. Without this offset, a
     * mid-day reboot would permanently freeze today's total at its pre-reboot value, since every
     * post-reboot reading would compute a smaller raw diff than what was already persisted.
     *
     * Handles three cases, checked in priority order:
     * 1. **Reboot detected** ([currentCumulative] < [baselineValue]): the hardware sensor resets
     *    its cumulative count on device reboot, so the previous baseline is no longer valid. The
     *    current reading becomes the new baseline for [currentDate], and [existingTodaySteps]
     *    (the caller's already-persisted total for [currentDate], 0 if none) becomes both
     *    today's steps and the new offset -- so steps earned before the reboot are preserved and
     *    subsequent readings continue accumulating on top of them.
     * 2. **Day rollover** ([currentDate] != [baselineDate]): a new day has started since the
     *    baseline was last captured. The baseline resets to the current reading/date with a
     *    zero offset, and today's steps restart at zero. Callers MUST persist the previous day's
     *    final step total to history storage BEFORE invoking this function, since this function
     *    does not have access to (and will not return) the prior day's total.
     * 3. **Same-day normal case**: today's steps are [baselineOffsetSteps] plus the difference
     *    between the current cumulative reading and the baseline; the baseline is left
     *    unchanged.
     *
     * The returned `todaySteps` is never negative.
     */
    fun computeTodaySteps(
        currentCumulative: Long,
        currentDate: LocalDate,
        baselineValue: Long,
        baselineDate: LocalDate,
        baselineOffsetSteps: Int,
        existingTodaySteps: Int,
    ): StepBaselineResult {
        return when {
            currentCumulative < baselineValue -> {
                val preservedSteps = existingTodaySteps.coerceAtLeast(0)
                StepBaselineResult(
                    todaySteps = preservedSteps,
                    newBaselineValue = currentCumulative,
                    newBaselineDate = currentDate,
                    newBaselineOffsetSteps = preservedSteps,
                )
            }

            currentDate != baselineDate -> StepBaselineResult(
                todaySteps = 0,
                newBaselineValue = currentCumulative,
                newBaselineDate = currentDate,
                newBaselineOffsetSteps = 0,
            )

            else -> StepBaselineResult(
                todaySteps = (baselineOffsetSteps + (currentCumulative - baselineValue).toInt()).coerceAtLeast(0),
                newBaselineValue = baselineValue,
                newBaselineDate = baselineDate,
                newBaselineOffsetSteps = baselineOffsetSteps,
            )
        }
    }
}

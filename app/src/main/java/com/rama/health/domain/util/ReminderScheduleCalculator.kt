package com.rama.health.domain.util

import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

object ReminderScheduleCalculator {

    /**
     * Computes the next wall-clock instant at which a medication reminder should fire for a
     * single daily time slot, respecting a [repeatDaysMask] weekday filter.
     *
     * The search starts at [now] (inclusive of "later today") and scans forward up to six
     * additional calendar days. For each candidate day, the function checks whether that
     * [DayOfWeek] is set in [repeatDaysMask] (Mon=1, Tue=2, …, Sun=64) and whether
     * [hour]:[minute] on that day is strictly after [now].
     *
     * If no matching day exists within the one-week window (e.g. [repeatDaysMask] is
     * [RepeatDays.none]), returns the same clock time seven days after [now] as an unreachable
     * sentinel — callers should avoid scheduling alarms when the mask is empty.
     */
    fun nextMedicationFireTime(
        repeatDaysMask: Int,
        hour: Int,
        minute: Int,
        now: ZonedDateTime,
    ): ZonedDateTime {
        require(hour in 0..23) { "hour must be 0..23, was $hour" }
        require(minute in 0..59) { "minute must be 0..59, was $minute" }

        val repeatDays = RepeatDays(repeatDaysMask)
        for (daysAhead in 0..6) {
            val candidateDate = now.toLocalDate().plusDays(daysAhead.toLong())
            if (!repeatDays.contains(candidateDate.dayOfWeek)) continue

            val candidate = atTimeOfDay(now, candidateDate, hour, minute)
            if (candidate.isAfter(now)) return candidate
        }

        return atTimeOfDay(now, now.toLocalDate().plusDays(7), hour, minute)
    }

    /**
     * Computes the next water-reminder fire time by chaining a fixed [intervalMinutes] offset
     * from [lastFireTime], clamping the result to the daily active window
     * [[activeStartMinutes], [activeEndMinutes]] (minutes from midnight, inclusive).
     *
     * After adding the interval:
     * - If the candidate falls **before** [activeStartMinutes], it snaps forward to
     *   [activeStartMinutes] on the same calendar day.
     * - If the candidate falls **after** [activeEndMinutes], it rolls to [activeStartMinutes]
     *   on the **next** calendar day.
     * - Otherwise the candidate is returned as-is (seconds/nanos zeroed).
     *
     * This models interval-based hydration nudges that only fire during waking hours — e.g.
     * with a 08:00–22:00 window, a reminder at 22:00 plus 60 minutes lands at 08:00 the
     * following day rather than 23:00.
     */
    fun nextIntervalFireTime(
        lastFireTime: ZonedDateTime,
        intervalMinutes: Int,
        activeStartMinutes: Int,
        activeEndMinutes: Int,
    ): ZonedDateTime {
        require(intervalMinutes > 0) { "intervalMinutes must be positive, was $intervalMinutes" }
        require(activeStartMinutes in 0..1439) {
            "activeStartMinutes must be 0..1439, was $activeStartMinutes"
        }
        require(activeEndMinutes in 0..1439) {
            "activeEndMinutes must be 0..1439, was $activeEndMinutes"
        }
        require(activeStartMinutes <= activeEndMinutes) {
            "activeStartMinutes ($activeStartMinutes) must be <= activeEndMinutes ($activeEndMinutes)"
        }

        var candidate = lastFireTime.plusMinutes(intervalMinutes.toLong())
        val minutesOfDay = candidate.hour * 60 + candidate.minute

        candidate = when {
            minutesOfDay < activeStartMinutes ->
                withMinutesOfDay(candidate, candidate.toLocalDate(), activeStartMinutes)

            minutesOfDay > activeEndMinutes ->
                withMinutesOfDay(
                    candidate,
                    candidate.toLocalDate().plusDays(1),
                    activeStartMinutes,
                )

            else -> candidate.withSecond(0).withNano(0)
        }
        return candidate
    }

    /**
     * Finds the next fixed daily fire time from [fixedTimes] relative to [now].
     *
     * Times are evaluated in ascending (hour, minute) order. The first slot strictly after
     * [now] on the current calendar day wins; if every slot today has already passed, the
     * earliest slot tomorrow is returned. Returns `null` when [fixedTimes] is empty.
     */
    fun nextFixedTimeFireTime(
        fixedTimes: List<MedicationTime>,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        if (fixedTimes.isEmpty()) return null

        val sorted = fixedTimes.sortedWith(compareBy({ it.hour }, { it.minute }))

        for (time in sorted) {
            val candidate = atTimeOfDay(now, now.toLocalDate(), time.hour, time.minute)
            if (candidate.isAfter(now)) return candidate
        }

        val first = sorted.first()
        return atTimeOfDay(now, now.toLocalDate().plusDays(1), first.hour, first.minute)
    }

    private fun atTimeOfDay(
        anchor: ZonedDateTime,
        date: LocalDate,
        hour: Int,
        minute: Int,
    ): ZonedDateTime {
        return anchor.with(date)
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
    }

    private fun withMinutesOfDay(
        anchor: ZonedDateTime,
        date: LocalDate,
        totalMinutes: Int,
    ): ZonedDateTime {
        return anchor.with(date)
            .withHour(totalMinutes / 60)
            .withMinute(totalMinutes % 60)
            .withSecond(0)
            .withNano(0)
    }
}

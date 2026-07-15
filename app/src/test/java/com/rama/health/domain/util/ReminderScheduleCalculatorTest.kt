package com.rama.health.domain.util

import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderScheduleCalculatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun nextMedicationFireTime_sameDayLaterSlot_returnsLaterToday() {
        val now = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextMedicationFireTime(
            repeatDaysMask = RepeatDays.allDays().mask,
            hour = 14,
            minute = 0,
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 8, 14, 0, 0, 0, zone), result)
    }

    @Test
    fun nextMedicationFireTime_dayRollover_returnsNextMatchingDay() {
        val now = ZonedDateTime.of(2026, 7, 8, 15, 30, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextMedicationFireTime(
            repeatDaysMask = RepeatDays.allDays().mask,
            hour = 14,
            minute = 0,
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 9, 14, 0, 0, 0, zone), result)
    }

    @Test
    fun nextMedicationFireTime_bitmaskSkip_skipsNonMatchingDays() {
        // Friday 2026-07-10; only Monday (bit 1) is enabled.
        val now = ZonedDateTime.of(2026, 7, 10, 15, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextMedicationFireTime(
            repeatDaysMask = 1,
            hour = 9,
            minute = 0,
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 13, 9, 0, 0, 0, zone), result)
    }

    @Test
    fun nextMedicationFireTime_emptyMask_returnsSentinelSevenDaysOut() {
        val now = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextMedicationFireTime(
            repeatDaysMask = RepeatDays.none().mask,
            hour = 8,
            minute = 30,
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 15, 8, 30, 0, 0, zone), result)
    }

    @Test
    fun nextIntervalFireTime_withinWindow_returnsIntervalOffset() {
        val lastFire = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextIntervalFireTime(
            lastFireTime = lastFire,
            intervalMinutes = 60,
            activeStartMinutes = 480,
            activeEndMinutes = 1320,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 8, 11, 0, 0, 0, zone), result)
    }

    @Test
    fun nextIntervalFireTime_pastEndRollsToNextDayStart() {
        val lastFire = ZonedDateTime.of(2026, 7, 8, 22, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextIntervalFireTime(
            lastFireTime = lastFire,
            intervalMinutes = 60,
            activeStartMinutes = 480,
            activeEndMinutes = 1320,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 9, 8, 0, 0, 0, zone), result)
    }

    @Test
    fun nextIntervalFireTime_beforeStartSnapsToWindowStart() {
        val lastFire = ZonedDateTime.of(2026, 7, 8, 7, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextIntervalFireTime(
            lastFireTime = lastFire,
            intervalMinutes = 30,
            activeStartMinutes = 480,
            activeEndMinutes = 1320,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 8, 8, 0, 0, 0, zone), result)
    }

    @Test
    fun nextFixedTimeFireTime_today_returnsNextSlotToday() {
        val now = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextFixedTimeFireTime(
            fixedTimes = listOf(
                MedicationTime(8, 0),
                MedicationTime(14, 0),
                MedicationTime(18, 0),
            ),
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 8, 14, 0, 0, 0, zone), result)
    }

    @Test
    fun nextFixedTimeFireTime_tomorrow_returnsEarliestSlotNextDay() {
        val now = ZonedDateTime.of(2026, 7, 8, 19, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextFixedTimeFireTime(
            fixedTimes = listOf(
                MedicationTime(8, 0),
                MedicationTime(14, 0),
            ),
            now = now,
        )

        assertEquals(ZonedDateTime.of(2026, 7, 9, 8, 0, 0, 0, zone), result)
    }

    @Test
    fun nextFixedTimeFireTime_emptyList_returnsNull() {
        val now = ZonedDateTime.of(2026, 7, 8, 10, 0, 0, 0, zone)
        val result = ReminderScheduleCalculator.nextFixedTimeFireTime(
            fixedTimes = emptyList(),
            now = now,
        )

        assertNull(result)
    }
}

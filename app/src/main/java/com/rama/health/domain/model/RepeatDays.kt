package com.rama.health.domain.model

import java.time.DayOfWeek

/**
 * Bitmask of days on which a reminder repeats.
 *
 * Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64.
 */
@JvmInline
value class RepeatDays(val mask: Int) {

    fun contains(dayOfWeek: DayOfWeek): Boolean {
        return mask and dayBit(dayOfWeek) != 0
    }

    companion object {
        fun allDays(): RepeatDays = RepeatDays(ALL_DAYS_MASK)

        fun none(): RepeatDays = RepeatDays(0)

        fun fromSetOfDays(days: Set<DayOfWeek>): RepeatDays {
            return RepeatDays(days.fold(0) { acc, day -> acc or dayBit(day) })
        }

        private const val ALL_DAYS_MASK = 1 or 2 or 4 or 8 or 16 or 32 or 64

        private fun dayBit(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)
    }
}

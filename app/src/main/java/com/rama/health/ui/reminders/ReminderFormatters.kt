package com.rama.health.ui.reminders

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rama.health.R
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.model.WaterScheduleMode
import java.time.DayOfWeek
import java.util.Locale

object ReminderFormatters {

    fun formatTime(time: MedicationTime): String =
        String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute)

    fun formatMinutesFromMidnight(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return formatTime(MedicationTime(hour, minute))
    }

    @Composable
    fun formatIntervalMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> stringResource(R.string.water_reminder_interval_minutes, minutes)
            minutes % 60 == 0 -> stringResource(R.string.water_reminder_interval_hours, minutes / 60)
            else -> stringResource(
                R.string.water_reminder_interval_hours_minutes,
                minutes / 60,
                minutes % 60,
            )
        }
    }

    @Composable
    fun formatScheduleMode(mode: WaterScheduleMode): String = when (mode) {
        WaterScheduleMode.INTERVAL -> stringResource(R.string.water_reminder_schedule_mode_interval)
        WaterScheduleMode.FIXED_TIMES -> stringResource(R.string.water_reminder_schedule_mode_fixed_times)
    }

    @Composable
    fun formatRepeatDays(repeatDays: RepeatDays): String {
        val labels = DayOfWeek.entries.mapNotNull { day ->
            if (repeatDays.contains(day)) dayLabel(day) else null
        }
        return labels.joinToString(", ")
    }

    @Composable
    private fun dayLabel(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> stringResource(R.string.medication_day_mon)
        DayOfWeek.TUESDAY -> stringResource(R.string.medication_day_tue)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.medication_day_wed)
        DayOfWeek.THURSDAY -> stringResource(R.string.medication_day_thu)
        DayOfWeek.FRIDAY -> stringResource(R.string.medication_day_fri)
        DayOfWeek.SATURDAY -> stringResource(R.string.medication_day_sat)
        DayOfWeek.SUNDAY -> stringResource(R.string.medication_day_sun)
    }

    fun formatTimesSummary(times: List<MedicationTime>): String =
        times.sortedWith(compareBy({ it.hour }, { it.minute }))
            .joinToString(", ") { formatTime(it) }
}

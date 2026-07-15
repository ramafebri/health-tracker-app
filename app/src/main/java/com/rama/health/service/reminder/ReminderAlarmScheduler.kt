package com.rama.health.service.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rama.health.MainActivity
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.repository.ReminderScheduler
import com.rama.health.domain.util.ReminderScheduleCalculator
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAlarmScheduler @Inject constructor(
    private val alarmManager: AlarmManager,
    @param:ApplicationContext private val context: Context,
    private val intentFactory: ReminderIntentFactory,
) : ReminderScheduler {

    private val scheduledMedicationIds = mutableSetOf<String>()

    override suspend fun scheduleWater(settings: WaterReminderSettings) {
        cancelWater()
        if (!settings.enabled) return

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        when (settings.scheduleMode) {
            WaterScheduleMode.INTERVAL -> {
                val fireTime = ReminderScheduleCalculator.nextIntervalFireTime(
                    lastFireTime = now,
                    intervalMinutes = settings.intervalMinutes,
                    activeStartMinutes = settings.activeStartMinutes,
                    activeEndMinutes = settings.activeEndMinutes,
                )
                scheduleWaterAlarm(
                    slotIndex = 0,
                    isInterval = true,
                    triggerAtMillis = fireTime.toInstant().toEpochMilli(),
                )
            }

            WaterScheduleMode.FIXED_TIMES -> {
                settings.fixedTimes.forEachIndexed { index, time ->
                    val fireTime = ReminderScheduleCalculator.nextMedicationFireTime(
                        repeatDaysMask = RepeatDays.allDays().mask,
                        hour = time.hour,
                        minute = time.minute,
                        now = now,
                    )
                    scheduleWaterAlarm(
                        slotIndex = index,
                        isInterval = false,
                        triggerAtMillis = fireTime.toInstant().toEpochMilli(),
                    )
                }
            }
        }
    }

    override suspend fun cancelWater() {
        cancelAlarm(
            type = ReminderAlarmRequestCodes.Type.WATER,
            entityId = ReminderAlarmRequestCodes.WATER_ENTITY_ID,
            slotIndex = 0,
            reminderType = ReminderNotificationHelper.REMINDER_TYPE_WATER,
            medicationId = null,
            isInterval = true,
        )
        for (slotIndex in 0 until ReminderAlarmRequestCodes.MAX_WATER_FIXED_SLOTS) {
            cancelAlarm(
                type = ReminderAlarmRequestCodes.Type.WATER,
                entityId = ReminderAlarmRequestCodes.WATER_ENTITY_ID,
                slotIndex = slotIndex,
                reminderType = ReminderNotificationHelper.REMINDER_TYPE_WATER,
                medicationId = null,
                isInterval = false,
            )
        }
    }

    override suspend fun scheduleMedication(reminder: MedicationReminder) {
        cancelMedication(reminder.id)
        if (!reminder.enabled || reminder.times.isEmpty() || reminder.repeatDays.mask == 0) {
            return
        }

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        reminder.times.forEachIndexed { index, time ->
            val fireTime = ReminderScheduleCalculator.nextMedicationFireTime(
                repeatDaysMask = reminder.repeatDays.mask,
                hour = time.hour,
                minute = time.minute,
                now = now,
            )
            scheduleMedicationAlarm(
                medicationId = reminder.id,
                slotIndex = index,
                triggerAtMillis = fireTime.toInstant().toEpochMilli(),
            )
        }
        scheduledMedicationIds.add(reminder.id)
    }

    override suspend fun cancelMedication(medicationId: String) {
        for (slotIndex in 0 until ReminderAlarmRequestCodes.MAX_MEDICATION_SLOTS) {
            cancelAlarm(
                type = ReminderAlarmRequestCodes.Type.MEDICATION,
                entityId = medicationId,
                slotIndex = slotIndex,
                reminderType = ReminderNotificationHelper.REMINDER_TYPE_MEDICATION,
                medicationId = medicationId,
                isInterval = false,
            )
        }
        scheduledMedicationIds.remove(medicationId)
    }

    override suspend fun cancelAll() {
        cancelWater()
        scheduledMedicationIds.toList().forEach { cancelMedication(it) }
    }

    override suspend fun rescheduleAll(
        waterSettings: WaterReminderSettings,
        medications: List<MedicationReminder>,
    ) {
        cancelWater()
        medications.forEach { cancelMedication(it.id) }
        if (waterSettings.enabled) {
            scheduleWater(waterSettings)
        }
        medications.filter { it.enabled }.forEach { scheduleMedication(it) }
    }

    private fun scheduleWaterAlarm(
        slotIndex: Int,
        isInterval: Boolean,
        triggerAtMillis: Long,
    ) {
        val pendingIntent = intentFactory.createAlarmPendingIntent(
            type = ReminderAlarmRequestCodes.Type.WATER,
            entityId = ReminderAlarmRequestCodes.WATER_ENTITY_ID,
            slotIndex = slotIndex,
            reminderType = ReminderNotificationHelper.REMINDER_TYPE_WATER,
            medicationId = null,
            isInterval = isInterval,
        )
        setAlarm(triggerAtMillis, pendingIntent)
    }

    private fun scheduleMedicationAlarm(
        medicationId: String,
        slotIndex: Int,
        triggerAtMillis: Long,
    ) {
        val pendingIntent = intentFactory.createAlarmPendingIntent(
            type = ReminderAlarmRequestCodes.Type.MEDICATION,
            entityId = medicationId,
            slotIndex = slotIndex,
            reminderType = ReminderNotificationHelper.REMINDER_TYPE_MEDICATION,
            medicationId = medicationId,
            isInterval = false,
        )
        setAlarm(triggerAtMillis, pendingIntent)
    }

    private fun cancelAlarm(
        type: ReminderAlarmRequestCodes.Type,
        entityId: String,
        slotIndex: Int,
        reminderType: String,
        medicationId: String?,
        isInterval: Boolean,
    ) {
        val pendingIntent = intentFactory.createAlarmPendingIntent(
            type = type,
            entityId = entityId,
            slotIndex = slotIndex,
            reminderType = reminderType,
            medicationId = medicationId,
            isInterval = isInterval,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun setAlarm(triggerAtMillis: Long, operation: PendingIntent) {
        if (PermissionUtils.hasExactAlarmPermission(context)) {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                operation,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
        }
    }
}

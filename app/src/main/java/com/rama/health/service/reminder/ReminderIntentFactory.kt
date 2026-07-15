package com.rama.health.service.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderIntentFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun createAlarmPendingIntent(
        type: ReminderAlarmRequestCodes.Type,
        entityId: String,
        slotIndex: Int,
        reminderType: String,
        medicationId: String?,
        isInterval: Boolean,
    ): PendingIntent {
        val requestCode = ReminderAlarmRequestCodes.requestCode(type, entityId, slotIndex)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntentExtras.ACTION_REMINDER_ALARM
            putExtra(ReminderIntentExtras.EXTRA_REMINDER_TYPE, reminderType)
            putExtra(ReminderIntentExtras.EXTRA_SLOT_INDEX, slotIndex)
            putExtra(ReminderIntentExtras.EXTRA_IS_INTERVAL, isInterval)
            if (medicationId != null) {
                putExtra(ReminderIntentExtras.EXTRA_MEDICATION_ID, medicationId)
            }
            putExtra(
                ReminderIntentExtras.EXTRA_NOTIFICATION_ID,
                notificationIdFor(requestCode),
            )
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationIdFor(requestCode: Int): Int {
        return ReminderNotificationHelper.NOTIFICATION_ID_BASE + (requestCode and 0x7FFF)
    }
}

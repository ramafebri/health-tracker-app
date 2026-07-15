package com.rama.health.service.reminder

object ReminderIntentExtras {
    const val ACTION_REMINDER_ALARM = "com.rama.health.action.REMINDER_ALARM"

    const val EXTRA_REMINDER_TYPE = ReminderNotificationHelper.EXTRA_REMINDER_TYPE
    const val EXTRA_MEDICATION_ID = ReminderNotificationHelper.EXTRA_MEDICATION_ID
    const val EXTRA_SLOT_INDEX = "extra_slot_index"
    const val EXTRA_IS_INTERVAL = "extra_is_interval"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
}

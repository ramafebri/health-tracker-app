package com.rama.health.service.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rama.health.MainActivity
import com.rama.health.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReminderNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showWaterReminder(notificationManager: NotificationManager, notificationId: Int) {
        val title = context.getString(R.string.reminder_water_title)
        val body = context.getString(R.string.reminder_water_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(
                contentIntent(
                    notificationId = notificationId,
                    reminderType = REMINDER_TYPE_WATER,
                    medicationId = null,
                ),
            )
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showMedicationReminder(
        notificationManager: NotificationManager,
        notificationId: Int,
        medicationId: String,
        name: String,
        dosage: String?,
    ) {
        val title = context.getString(R.string.reminder_medication_title, name)
        val body = if (dosage != null) {
            context.getString(
                R.string.reminder_medication_body_with_dosage,
                name,
                dosage,
            )
        } else {
            context.getString(R.string.reminder_medication_body, name)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(
                contentIntent(
                    notificationId = notificationId,
                    reminderType = REMINDER_TYPE_MEDICATION,
                    medicationId = medicationId,
                ),
            )
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun contentIntent(
        notificationId: Int,
        reminderType: String,
        medicationId: String?,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
            if (medicationId != null) {
                putExtra(EXTRA_MEDICATION_ID, medicationId)
            }
        }
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "health_reminders_channel"

        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"

        const val REMINDER_TYPE_WATER = "water"
        const val REMINDER_TYPE_MEDICATION = "medication"

        const val NOTIFICATION_ID_BASE = 3000
    }
}

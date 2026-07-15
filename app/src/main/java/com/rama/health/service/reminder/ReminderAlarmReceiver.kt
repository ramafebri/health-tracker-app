package com.rama.health.service.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.ReminderScheduler
import com.rama.health.domain.repository.WaterReminderRepository
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles fired reminder alarms: posts the notification and chains the next occurrence.
 * Disabled, deleted, or unknown reminders are ignored without crashing.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: ReminderNotificationHelper
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var waterReminderRepository: WaterReminderRepository
    @Inject lateinit var medicationReminderRepository: MedicationReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderIntentExtras.ACTION_REMINDER_ALARM) return

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                handleAlarm(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra(ReminderIntentExtras.EXTRA_REMINDER_TYPE) ?: return
        val notificationId = intent.getIntExtra(
            ReminderIntentExtras.EXTRA_NOTIFICATION_ID,
            ReminderNotificationHelper.NOTIFICATION_ID_BASE,
        )
        val canNotify = PermissionUtils.hasPostNotificationsPermission(context)

        when (reminderType) {
            ReminderNotificationHelper.REMINDER_TYPE_WATER -> handleWaterAlarm(notificationId, canNotify)
            ReminderNotificationHelper.REMINDER_TYPE_MEDICATION -> {
                handleMedicationAlarm(intent, notificationId, canNotify)
            }
        }
    }

    private suspend fun handleWaterAlarm(notificationId: Int, canNotify: Boolean) {
        val settings = waterReminderRepository.observeSettings().first()
        if (!settings.enabled) return

        if (canNotify) {
            notificationHelper.ensureChannel(notificationManager)
            notificationHelper.showWaterReminder(notificationManager, notificationId)
        }
        reminderScheduler.scheduleWater(settings)
    }

    private suspend fun handleMedicationAlarm(
        intent: Intent,
        notificationId: Int,
        canNotify: Boolean,
    ) {
        val medicationId = intent.getStringExtra(ReminderIntentExtras.EXTRA_MEDICATION_ID) ?: return
        val reminder = medicationReminderRepository.observeById(medicationId).first() ?: return
        if (!reminder.enabled) return

        if (canNotify) {
            notificationHelper.ensureChannel(notificationManager)
            notificationHelper.showMedicationReminder(
                notificationManager = notificationManager,
                notificationId = notificationId,
                medicationId = reminder.id,
                name = reminder.name,
                dosage = reminder.dosage,
            )
        }

        reminderScheduler.scheduleMedication(reminder)
    }
}

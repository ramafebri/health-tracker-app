package com.rama.health.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rama.health.domain.usecase.RescheduleAllRemindersUseCase
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Restores all enabled water and medication alarms after device reboot.
 * Separate from [com.rama.health.service.BootCompletedReceiver], which only handles step tracking.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduleAllRemindersUseCase: RescheduleAllRemindersUseCase

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!PermissionUtils.hasPostNotificationsPermission(context)) return

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                rescheduleAllRemindersUseCase()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

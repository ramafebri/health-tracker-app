package com.rama.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rama.health.service.reminder.ReminderNotificationHelper
import com.rama.health.ui.navigation.NavGraph
import com.rama.health.ui.navigation.ReminderDeepLink
import com.rama.health.ui.theme.HealthTrackerAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var reminderDeepLink by mutableStateOf<ReminderDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderDeepLink = parseReminderDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            HealthTrackerAppTheme {
                NavGraph(
                    reminderDeepLink = reminderDeepLink,
                    onReminderDeepLinkHandled = { reminderDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderDeepLink = parseReminderDeepLink(intent)
    }

    private fun parseReminderDeepLink(intent: Intent?): ReminderDeepLink? {
        val type = intent?.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_TYPE) ?: return null
        return when (type) {
            ReminderNotificationHelper.REMINDER_TYPE_WATER -> ReminderDeepLink.Water
            ReminderNotificationHelper.REMINDER_TYPE_MEDICATION -> {
                val medicationId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_MEDICATION_ID)
                    ?: return null
                ReminderDeepLink.Medication(medicationId)
            }
            else -> null
        }
    }
}

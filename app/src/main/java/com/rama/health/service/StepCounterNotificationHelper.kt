package com.rama.health.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StepCounterNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Step Counter",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing step count tracking"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(todaySteps: Int, goal: Int): Notification {
        val progressText = if (goal > 0) "$todaySteps / $goal steps today" else "$todaySteps steps today"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Tracking your steps")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // TODO: replace with a dedicated app icon asset
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIFICATION_ID = 1001
    }
}

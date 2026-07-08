package com.rama.health.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rama.health.R
import com.rama.health.domain.model.WorkoutType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkoutNotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workout Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing GPS workout tracking"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        workoutType: WorkoutType?,
        elapsedSeconds: Long,
        distanceMeters: Double,
    ): Notification {
        val typeLabel = workoutType?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Workout"
        val distanceKm = distanceMeters / 1_000.0
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val elapsedText = "%d:%02d".format(minutes, seconds)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Tracking $typeLabel")
            .setContentText("$elapsedText · %.2f km".format(distanceKm))
            .setSmallIcon(R.drawable.ic_notification_workout)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "workout_tracking_channel"
        const val NOTIFICATION_ID = 1002
    }
}

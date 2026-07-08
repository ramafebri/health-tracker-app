package com.rama.health.ui.workout

object WorkoutFormatters {
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%d:%02d".format(minutes, secs)
        }
    }

    fun formatDistanceKm(meters: Double): String = "%.2f km".format(meters / 1_000.0)

    fun formatPace(secPerKm: Int?): String {
        if (secPerKm == null || secPerKm <= 0) return "—"
        val minutes = secPerKm / 60
        val seconds = secPerKm % 60
        return "%d:%02d /km".format(minutes, seconds)
    }

    fun formatSpeedKmh(speed: Double?): String {
        if (speed == null || speed <= 0) return "—"
        return "%.1f km/h".format(speed)
    }

    fun formatElevation(meters: Double): String = "%.0f m".format(meters)
}

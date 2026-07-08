package com.rama.health.domain.model

data class WorkoutRecord(
    val id: String,
    val type: WorkoutType,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Int?,
    val avgSpeedKmh: Double?,
    val elevationGainMeters: Double,
    val minAltitude: Double?,
    val maxAltitude: Double?,
)

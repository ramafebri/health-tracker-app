package com.rama.health.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val type: String,
    val status: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double?,
    val avgSpeedKmh: Double?,
    val elevationGainMeters: Double,
    val minAltitude: Double?,
    val maxAltitude: Double?,
)

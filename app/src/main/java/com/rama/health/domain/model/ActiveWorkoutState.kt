package com.rama.health.domain.model

data class ActiveWorkoutState(
    val workoutId: String? = null,
    val type: WorkoutType? = null,
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val startTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Int? = null,
    val avgPaceSecPerKm: Int? = null,
    val avgSpeedKmh: Double? = null,
    val elevationGainMeters: Double = 0.0,
    val minAltitude: Double? = null,
    val maxAltitude: Double? = null,
    val routePoints: List<RoutePoint> = emptyList(),
)

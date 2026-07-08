package com.rama.health.domain.model

data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double?,
    val timestamp: Long,
)

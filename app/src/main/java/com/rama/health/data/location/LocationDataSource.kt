package com.rama.health.data.location

interface LocationDataSource {
    fun startUpdates(intervalMs: Long, onLocation: (latitude: Double, longitude: Double, altitude: Double?, timestampMillis: Long) -> Unit)
    fun stopUpdates()
}

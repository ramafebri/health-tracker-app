package com.rama.health.data.location

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedLocationDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationDataSource {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startUpdates(
        intervalMs: Long,
        onLocation: (latitude: Double, longitude: Double, altitude: Double?, timestampMillis: Long) -> Unit,
    ) {
        stopUpdates()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onLocation(
                    location.latitude,
                    location.longitude,
                    location.altitude.takeIf { location.hasAltitude() },
                    location.time,
                )
            }
        }
        callback = locationCallback
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun stopUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
    }
}

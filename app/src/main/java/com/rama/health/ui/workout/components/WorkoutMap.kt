package com.rama.health.ui.workout.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.rama.health.domain.model.RoutePoint

@Composable
fun WorkoutMap(
    routePoints: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState()
    val latLngs = routePoints.map { LatLng(it.lat, it.lng) }

    LaunchedEffect(routePoints) {
        when {
            latLngs.size >= 2 -> {
                val boundsBuilder = LatLngBounds.builder()
                latLngs.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
            latLngs.size == 1 -> {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLngs.first(), 16f)
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
    ) {
        if (latLngs.size >= 2) {
            Polyline(points = latLngs)
        }
    }
}

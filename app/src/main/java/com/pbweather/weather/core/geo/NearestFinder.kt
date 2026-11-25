package com.pbweather.weather.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordinates in this record belong to public reference metadata, never a user location. */
data class StationPoint(val id: String, val name: String, val latitude: Double, val longitude: Double)
data class NearbyStation(val station: StationPoint, val distanceKm: Double)

object NearestFinder {
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val a = Math.toRadians(lat2 - lat1)
        val b = Math.toRadians(lon2 - lon1)
        val h = sin(a / 2) * sin(a / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(b / 2) * sin(b / 2)
        return 6371.00877 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
    fun nearest(latitude: Double, longitude: Double, stations: List<StationPoint>, count: Int = 1): List<NearbyStation> {
        require(latitude.isFinite() && longitude.isFinite() && count >= 0)
        return stations.map { NearbyStation(it, distanceKm(latitude, longitude, it.latitude, it.longitude)) }
            .sortedWith(compareBy<NearbyStation> { it.distanceKm }.thenBy { it.station.id }).take(count)
    }
}

package com.pbweather.weather.core.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

data class GridPoint(val nx: Int, val ny: Int)

object KmaGridConverter {
    /** Convert a transient WGS84 position to the official KMA 5 km LCC grid. */
    fun convert(latitude: Double, longitude: Double): GridPoint {
        require(latitude.isFinite() && longitude.isFinite())
        require(latitude > -90 && latitude < 90 && longitude in -180.0..180.0)
        val rad = PI / 180.0
        val first = 30.0 * rad
        val second = 60.0 * rad
        val sn = ln(cos(first) / cos(second)) / ln(tan(PI / 4 + second / 2) / tan(PI / 4 + first / 2))
        val sf = tan(PI / 4 + first / 2).pow(sn) * cos(first) / sn
        val re = 6371.00877 / 5.0
        val ro = re * sf / tan(PI / 4 + 38.0 * rad / 2).pow(sn)
        val ra = re * sf / tan(PI / 4 + latitude * rad / 2).pow(sn)
        var theta = longitude * rad - 126.0 * rad
        if (theta > PI) theta -= 2 * PI
        if (theta < -PI) theta += 2 * PI
        theta *= sn
        return GridPoint(floor(ra * sin(theta) + 43 + 0.5).toInt(), floor(ro - ra * cos(theta) + 136 + 0.5).toInt())
    }
}

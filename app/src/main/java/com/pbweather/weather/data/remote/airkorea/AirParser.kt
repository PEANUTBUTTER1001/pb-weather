package com.pbweather.weather.data.remote.airkorea

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.AirStationEntity
import com.pbweather.weather.data.remote.integer
import com.pbweather.weather.data.remote.number
import com.pbweather.weather.data.remote.text
import com.pbweather.weather.domain.AirMeasurement
import com.pbweather.weather.domain.AirQualityData
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.JsonObject

object AirParser {
    fun time(raw: String?): Long? = runCatching {
        val value = requireNotNull(raw).trim()
        val day = LocalDate.parse(value.take(10))
        val time = value.drop(10).trim().take(5)
        if (time == "24:00") {
            day.plusDays(1).atStartOfDay(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
        } else {
            day.atTime(LocalTime.parse(time)).atZone(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
        }
    }.getOrNull()

    /** Keep one-hour grades and independent pollutant flags; normalize midnight before station/time deduplication. */
    fun readings(station: String, items: List<JsonObject>): AirQualityData {
        val readings = items.mapNotNull { row ->
            val at = time(row.text("dataTime")) ?: return@mapNotNull null
            fun flag(field: String) = row.text(field)?.trim()?.takeIf { it.isNotEmpty() && it != "-" && it != "null" }
            val pm10Flag = flag("pm10Flag")
            val pm25Flag = flag("pm25Flag")
            fun value(field: String, flag: String?) = if (flag == null) row.number(field)?.takeIf { it >= 0 } else null
            fun grade(field: String, flag: String?) = if (flag == null) row.integer(field)?.takeIf { it in 1..4 } else null
            val pm10 = value("pm10Value", pm10Flag)
            val pm25 = value("pm25Value", pm25Flag)
            AirMeasurement(
                station, at, row.text("dataTime").orEmpty(), pm10, pm25,
                if (pm10 != null) grade("pm10Grade1h", pm10Flag) else null,
                if (pm25 != null) grade("pm25Grade1h", pm25Flag) else null, pm10Flag, pm25Flag
            )
        }.groupBy { it.at }.values.map { duplicates ->
            duplicates.maxBy { (if (it.pm10 != null) 1 else 0) + (if (it.pm25 != null) 1 else 0) }
        }.sortedBy { it.at }.takeLast(24)
        return AirQualityData(station, readings)
    }
    fun stations(items: List<JsonObject>, fetchedAt: Long): List<AirStationEntity> = items.mapNotNull { row ->
        val name = row.text("stationName")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        // Official metadata ver=1.1 fixes the older reversed-coordinate contract.
        val latitude = row.number("dmY")?.takeIf { it in 30.0..40.0 } ?: return@mapNotNull null
        val longitude = row.number("dmX")?.takeIf { it in 120.0..140.0 } ?: return@mapNotNull null
        AirStationEntity(name, latitude, longitude, fetchedAt, row.text("mangName").orEmpty())
    }.groupBy { it.name }.values.mapNotNull { duplicates ->
        // The selected realtime operation accepts names, so ambiguous names cannot be resolved safely by station ID.
        duplicates.distinctBy { it.latitude to it.longitude }.singleOrNull()
    }
}

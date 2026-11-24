package com.pbweather.weather.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface AirQualitySource {
    fun observe(region: Region): Flow<Resource<AirQualityData>>
    suspend fun refresh(region: Region)
}
class ObserveAirQuality @Inject constructor(private val source: AirQualitySource) {
    operator fun invoke(region: Region) = source.observe(region)
    suspend fun refresh(region: Region) = source.refresh(region)
}
object AirRules {
    fun latestCoverage(data: AirQualityData?): Int = data?.readings?.lastOrNull()?.let {
        (if (it.pm10 != null) 1 else 0) + (if (it.pm25 != null) 1 else 0)
    } ?: 0

    /** Never mix measurements from two stations under a single station heading. */
    fun useFallback(primary: AirQualityData?, fallback: AirQualityData?): Boolean = latestCoverage(fallback) > latestCoverage(primary)
}

object AirTrend {
    fun segments(data: AirQualityData, pm25: Boolean): List<List<AirMeasurement>> {
        val result = mutableListOf<MutableList<AirMeasurement>>()
        var previous: AirMeasurement? = null
        for (row in data.readings) {
            if ((if (pm25) row.pm25 else row.pm10) == null) {
                previous = null
                continue
            }
            if (previous == null || row.at - previous.at != 3600000L) {
                result += mutableListOf(row)
            } else {
                result.last() += row
            }
            previous = row
        }
        return result
    }
}

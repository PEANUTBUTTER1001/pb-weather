package com.pbweather.weather.domain

import kotlinx.serialization.Serializable

@Serializable
enum class DataKind { NOWCAST, ULTRA, SHORT, MID_LAND, MID_TA, AIR, HISTORY, EXTREMES, STATIONS }

@Serializable
enum class Provider { KMA, AIRKOREA }
enum class DomainError { OFFLINE, TIMEOUT, NO_DATA, QUOTA, KEY_CONFIGURATION, INVALID_RESPONSE, REGION_MAPPING, UNKNOWN }
class DataException(val reason: DomainError) : Exception(reason.name)

@Serializable
data class SourceMeta(
    val provider: Provider,
    val kind: DataKind,
    val locationKey: String,
    val receivedAt: Long,
    val issuedAt: Long? = null,
    val observedAt: Long? = null,
    val validFrom: Long? = null,
    val validTo: Long? = null,
    val version: String = ""
)

data class Resource<T>(
    val data: T? = null,
    val meta: SourceMeta? = null,
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: DomainError? = null
)

/** Public reference coordinates only. A raw device position must never enter this record. */
@Serializable
data class Region(
    val id: String,
    val name: String,
    val nx: Int,
    val ny: Int,
    val publicLatitude: Double,
    val publicLongitude: Double,
    val asosStationId: String,
    val asosStationName: String,
    val asosDistanceKm: Double,
    val midLandRegId: String? = null,
    val midTaRegId: String? = null,
    val usingCurrentLocation: Boolean = false
) {
    val key: String get() = "${nx}_$ny"
}

@Serializable
enum class PrecipitationKind { EXACT, RANGE, QUALITATIVE, NONE }

@Serializable
data class Precipitation(val text: String, val kind: PrecipitationKind, val exactMm: Double? = null)

@Serializable
data class ObservedWeather(
    val temperature: Double? = null,
    val humidity: Double? = null,
    val windMs: Double? = null,
    val windDirection: Double? = null,
    val precipitation: Precipitation? = null,
    val precipitationType: Int? = null
)

@Serializable
data class ForecastHour(
    val at: Long,
    val temperature: Double? = null,
    val sky: Int? = null,
    val precipitationType: Int? = null,
    val probability: Int? = null,
    val precipitation: Precipitation? = null,
    val origins: Map<String, SourceMeta> = emptyMap()
)

@Serializable
data class DailyForecast(
    val date: String,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val morningWeather: String? = null,
    val afternoonWeather: String? = null,
    val wholeDayWeather: String? = null,
    val morningProbability: Int? = null,
    val afternoonProbability: Int? = null,
    val wholeDayProbability: Int? = null,
    val origins: Map<String, SourceMeta> = emptyMap(),
    val periodMaximum: Boolean = false
)

@Serializable
data class ForecastData(val hours: List<ForecastHour> = emptyList(), val days: List<DailyForecast> = emptyList())

@Serializable
data class DailyObservation(
    val stationId: String,
    val date: String,
    val average: Double? = null,
    val maximum: Double? = null,
    val minimum: Double? = null,
    val precipitation: Double? = null,
    val windMs: Double? = null,
    val humidity: Double? = null,
    val meta: SourceMeta
)

@Serializable
data class AirMeasurement(
    val station: String,
    val at: Long,
    val originalTime: String,
    val pm10: Double? = null,
    val pm25: Double? = null,
    val pm10Grade: Int? = null,
    val pm25Grade: Int? = null,
    val pm10Flag: String? = null,
    val pm25Flag: String? = null
)

@Serializable
data class AirQualityData(
    val station: String,
    val readings: List<AirMeasurement>,
    val isFallback: Boolean = false,
    val distanceKm: Double? = null,
    val gradeBasis: String = "1h/ver=1.5"
)

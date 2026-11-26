package com.pbweather.weather.ui.design

import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.domain.AirMeasurement
import com.pbweather.weather.domain.AirQualityData
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.ForecastData
import com.pbweather.weather.domain.ForecastHour
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.ObservedWeather
import com.pbweather.weather.domain.Precipitation
import com.pbweather.weather.domain.PrecipitationKind
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.ui.navigation.LocationUi
import com.pbweather.weather.ui.navigation.WeatherData
import com.pbweather.weather.ui.navigation.WeatherUiState
import java.time.Instant
import java.time.LocalDate

/** Public reference position; fixed prototype data never enters a production repository. */
object PrototypeFixtures {
    val now = Instant.parse("2026-11-24T05:20:00Z").toEpochMilli()
    val region = Region("prototype-jongno", "서울 종로구 청운효자동", 60, 127, 37.5635694, 126.9800083, "108", "서울", 1.8, usingCurrentLocation = true)
    private fun meta(kind: DataKind) = SourceMeta(
        if (kind == DataKind.AIR) Provider.AIRKOREA else Provider.KMA,
        kind,
        region.key,
        now - 120000,
        issuedAt = now - 1200000,
        observedAt = now - if (kind == DataKind.AIR) 4800000 else 1200000
    )
    fun state(): WeatherUiState {
        val first = now - 1200000
        val hourly = (0 until 48).map { index ->
            ForecastHour(
                first + index * 3600000L,
                7.4 + kotlin.math.sin(index * .5) * 3,
                if (index == 6) 4 else 1,
                if (index == 6) 1 else 0,
                if (index == 6) 60 else 10,
                Precipitation(if (index == 6) "1~4mm" else "강수 없음", if (index == 6) PrecipitationKind.RANGE else PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            )
        }
        val days = (0..10).map { index ->
            DailyForecast(
                LocalDate.parse("2026-11-24").plusDays(index.toLong()).toString(),
                2.0 - index % 4,
                10.0 - index % 5,
                "CLEAR",
                "PARTLY",
                morningProbability = 10,
                afternoonProbability = 30
            )
        }
        val observations = (1..10).map { index ->
            DailyObservation(
                "108", LocalDate.parse("2026-11-24").minusDays(index.toLong()).toString(),
                5.2, 11.7 - index * .1, 3.3, if (index == 6) null else index * .5, 2.1, 60.0, meta(DataKind.HISTORY)
            )
        }
        val air = AirQualityData(
            "종로구 측정소",
            (0..23).map { index ->
                AirMeasurement(
                    "종로구 측정소",
                    first - (24 - index) * 3600000L,
                    "2026-11-24 13:00",
                    34.0,
                    17.0,
                    2,
                    2
                )
            }
        )
        return WeatherUiState(
            preferences = AppPreferences(region, listOf(region), region.id),
            data = WeatherData(
                region.key,
                ForecastState(
                    Resource(
                        ObservedWeather(
                            7.4,
                            52.0,
                            3.2,
                            315.0,
                            Precipitation("강수 없음", PrecipitationKind.NONE),
                            0
                        ),
                        meta(DataKind.NOWCAST)
                    ),
                    Resource(ForecastData(hours = hourly), meta(DataKind.SHORT)),
                    Resource(days, meta(DataKind.MID_TA))
                ),
                Resource(air, meta(DataKind.AIR)),
                Resource(observations, meta(DataKind.HISTORY))
            ),
            range = DateRange(LocalDate.parse("2026-11-14"), LocalDate.parse("2026-11-23")),
            location = LocationUi(granted = true),
            now = now
        )
    }
}

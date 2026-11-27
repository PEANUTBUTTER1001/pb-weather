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
    val favorites = listOf(
        region,
        Region("busan-haeundae", "부산 해운대구 우1동", 99, 75, 35.1600194444444, 129.165808333333, "159", "부산", 11.4),
        Region("jeju-ido", "제주 제주시 이도2동", 53, 38, 33.4963111111111, 126.533208333333, "184", "제주", 2.4),
        Region("gangneung-gyo", "강원 강릉시 교1동", 92, 131, 37.7491361111111, 128.878497222222, "105", "강릉", 2.0)
    )
    private fun meta(kind: DataKind) = SourceMeta(
        if (kind == DataKind.AIR) Provider.AIRKOREA else Provider.KMA,
        kind,
        region.key,
        now - 120000,
        issuedAt = now - 1200000,
        observedAt = now - if (kind == DataKind.AIR) 4800000 else 1200000
    )
    fun favoriteForecasts(): Map<String, ForecastState> {
        val base = state().data.forecast
        return favorites.mapIndexed { index, region ->
            val temperature = listOf(7.4, 13.8, 14.4, 9.1)[index]
            val hour = base.hourly.data!!.hours.first().copy(sky = if (index == 0) 3 else 1, precipitationType = if (index == 2) 1 else 0)
            region.id to
                base.copy(
                    now = base.now.copy(
                        data = base.now.data!!.copy(
                            temperature = temperature,
                            precipitationType = if (index ==
                                2
                            ) {
                                1
                            } else {
                                0
                            }
                        )
                    ),
                    hourly = base.hourly.copy(data = ForecastData(hours = listOf(hour)))
                )
        }.toMap()
    }
    fun state(): WeatherUiState {
        val first = now - 1200000
        val hourly = listOf(
            ForecastHour(
                first + 0 * 3600000L,
                7.4,
                3,
                0,
                10,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 1 * 3600000L,
                7.6,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 2 * 3600000L,
                8.4,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 3 * 3600000L,
                8.2,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 4 * 3600000L,
                8.7,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 5 * 3600000L,
                7.5,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.ULTRA))
            ),
            ForecastHour(
                first + 6 * 3600000L,
                7.4,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 7 * 3600000L,
                7.0,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 8 * 3600000L,
                5.6,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 9 * 3600000L,
                5.2,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 10 * 3600000L,
                3.8,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 11 * 3600000L,
                2.9,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 12 * 3600000L,
                1.8,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 13 * 3600000L,
                2.0,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 14 * 3600000L,
                1.4,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 15 * 3600000L,
                1.0,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 16 * 3600000L,
                1.6,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 17 * 3600000L,
                1.5,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 18 * 3600000L,
                2.3,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 19 * 3600000L,
                3.2,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 20 * 3600000L,
                3.4,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 21 * 3600000L,
                4.7,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 22 * 3600000L,
                6.3,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 23 * 3600000L,
                7.0,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 24 * 3600000L,
                7.9,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 25 * 3600000L,
                7.6,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 26 * 3600000L,
                8.6,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 27 * 3600000L,
                8.5,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 28 * 3600000L,
                8.7,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 29 * 3600000L,
                8.0,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 30 * 3600000L,
                7.8,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 31 * 3600000L,
                6.2,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 32 * 3600000L,
                5.4,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 33 * 3600000L,
                4.4,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 34 * 3600000L,
                4.3,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 35 * 3600000L,
                2.5,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 36 * 3600000L,
                2.3,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 37 * 3600000L,
                1.8,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 38 * 3600000L,
                1.4,
                4,
                1,
                60,
                Precipitation("1~4mm", PrecipitationKind.RANGE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 39 * 3600000L,
                1.5,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 40 * 3600000L,
                1.3,
                3,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 41 * 3600000L,
                2.2,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 42 * 3600000L,
                1.8,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 43 * 3600000L,
                2.7,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 44 * 3600000L,
                4.3,
                1,
                0,
                0,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 45 * 3600000L,
                5.2,
                3,
                0,
                20,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 46 * 3600000L,
                5.9,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(
                first + 47 * 3600000L,
                7.2,
                4,
                0,
                30,
                Precipitation("강수 없음", PrecipitationKind.NONE),
                mapOf("temperature" to meta(DataKind.SHORT))
            ),
            ForecastHour(first + 48 * 3600000L, 7.2, 4, 0, 30, Precipitation("강수 없음", PrecipitationKind.NONE))
        )
        val days = listOf(
            DailyForecast(
                "2026-11-24",
                1.6,
                9.8,
                "RAIN",
                "PARTLY",
                morningProbability = 60,
                afternoonProbability = 10,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-25",
                4.0,
                12.0,
                "CLEAR",
                "CLOUDY",
                morningProbability = 10,
                afternoonProbability = 30,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-26",
                3.0,
                11.0,
                "CLEAR",
                "CLOUDY",
                morningProbability = 20,
                afternoonProbability = 30,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-27",
                1.0,
                9.0,
                "RAIN",
                "RAIN",
                morningProbability = 60,
                afternoonProbability = 70,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-28",
                -1.0,
                7.0,
                "CLEAR",
                "PARTLY",
                morningProbability = 20,
                afternoonProbability = 10,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-29",
                -2.0,
                7.0,
                "CLEAR",
                "RAIN",
                morningProbability = 20,
                afternoonProbability = 70,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-11-30",
                -1.0,
                7.0,
                "RAIN",
                "PARTLY",
                morningProbability = 60,
                afternoonProbability = 10,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-12-01",
                1.0,
                9.0,
                "RAIN",
                "RAIN",
                morningProbability = 60,
                afternoonProbability = 70,
                wholeDayWeather = null,
                wholeDayProbability = null
            ),
            DailyForecast(
                "2026-12-02",
                2.0,
                11.0,
                null,
                null,
                morningProbability = null,
                afternoonProbability = null,
                wholeDayWeather = "PARTLY",
                wholeDayProbability = 20
            ),
            DailyForecast(
                "2026-12-03",
                3.0,
                11.0,
                null,
                null,
                morningProbability = null,
                afternoonProbability = null,
                wholeDayWeather = "RAIN",
                wholeDayProbability = 60
            ),
            DailyForecast(
                "2026-12-04",
                3.0,
                11.0,
                null,
                null,
                morningProbability = null,
                afternoonProbability = null,
                wholeDayWeather = "RAIN",
                wholeDayProbability = 60
            )
        )
        val observations = listOf(
            DailyObservation("108", "2026-11-14", 10.2, 14.5, 5.8, 13.4, 3.1, 66.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-15", 10.1, 14.4, 5.7, 0.0, 1.7, 59.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-16", 8.9, 13.3, 4.6, 0.0, 2.0, 80.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-17", 7.3, 11.6, 2.9, null, 4.4, 52.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-18", 5.5, 9.9, 1.2, 12.4, 2.4, 48.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-19", 4.3, 8.7, 0.0, 0.0, null, 82.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-20", 4.3, 8.6, -0.1, 10.5, 1.7, 70.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-21", 5.0, 9.3, 0.6, 0.0, 3.0, 62.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-22", 6.2, 10.5, 1.8, 0.0, 2.9, 71.0, meta(DataKind.HISTORY)),
            DailyObservation("108", "2026-11-23", 7.3, 11.7, 3.0, 0.0, 2.6, 63.0, meta(DataKind.HISTORY))
        )
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
            preferences = AppPreferences(region, favorites, region.id),
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

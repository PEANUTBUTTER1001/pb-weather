package com.pbweather.weather.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.ForecastData
import com.pbweather.weather.domain.ForecastHour
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.ui.components.WeatherDetails
import com.pbweather.weather.ui.navigation.WeatherData
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DetailNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summerCalculationDisplaysResultAndBothWindUnits() {
        val at = Instant.parse("2026-07-01T03:00:00Z").toEpochMilli()
        val meta = com.pbweather.weather.domain.SourceMeta(
            com.pbweather.weather.domain.Provider.KMA,
            com.pbweather.weather.domain.DataKind.NOWCAST,
            "60_127",
            at,
            observedAt = at
        )
        val state = WeatherUiState(
            range = DateRange(LocalDate.parse("2026-06-21"), LocalDate.parse("2026-06-30")),
            now = at,
            data = WeatherData(
                forecast = ForecastState(now = Resource(com.pbweather.weather.domain.ObservedWeather(30.0, 70.0, 5.0), meta))
            )
        )
        compose.setContent { WeatherTheme { WeatherDetails("apparent", state) {} } }
        compose.onNodeWithText("31.3°").fetchSemanticsNode()
        compose.onNodeWithText("5.0 m/s · 18.0 km/h").performScrollTo().fetchSemanticsNode()
    }

    @Test fun airShowsBothIndicatorsAndPreservesIndependentMissingReason() {
        val at = Instant.parse("2026-11-25T03:00:00Z").toEpochMilli()
        val data = com.pbweather.weather.domain.AirQualityData(
            "테스트 측정소",
            listOf(
                com.pbweather.weather.domain.AirMeasurement("테스트 측정소", at, "2026-11-25 12:00", 25.0, 10.0, 1, 1),
                com.pbweather.weather.domain.AirMeasurement(
                    "테스트 측정소",
                    at + 3600000L,
                    "2026-11-25 13:00",
                    null,
                    11.0,
                    null,
                    1,
                    pm10Flag = "장비점검"
                )
            )
        )
        val state = WeatherUiState(
            range = DateRange(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-24")),
            now = at,
            data = WeatherData(air = Resource(data))
        )
        compose.setContent { WeatherTheme { WeatherDetails("air", state) {} } }
        compose.onNodeWithText("11.0 µg/m³ · 좋음").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("장비점검", substring = true).performScrollTo().fetchSemanticsNode()
    }

    @Test fun dayOpensOnlyActualHoursAndNavigationStopsAtBoundaries() {
        val now = Instant.parse("2026-11-25T03:00:00Z").toEpochMilli()
        val target = mutableStateOf("day:2026-11-25")
        val state = WeatherUiState(
            range = DateRange(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-24")),
            now = now,
            data = WeatherData(
                forecast = ForecastState(
                    hourly = Resource(ForecastData(hours = listOf(ForecastHour(now + 3600000, 7.0), ForecastHour(now + 7200000, 8.0)))),
                    weekly = Resource(listOf(DailyForecast("2026-11-25", 1.0, 9.0)))
                )
            )
        )
        compose.setContent { WeatherTheme { WeatherDetails(target.value, state, { target.value = it }) {} } }
        compose.onNodeWithText("시간별 보기").performScrollTo().performClick()
        compose.onNodeWithText("앞 시간으로 이동").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("다음 시간으로 이동").performClick()
        compose.onNodeWithText("다음 시간으로 이동").assertIsNotEnabled()
        compose.onNodeWithText("앞 시간으로 이동").performClick()
        compose.onNodeWithText("앞 시간으로 이동").assertIsNotEnabled()
    }
}

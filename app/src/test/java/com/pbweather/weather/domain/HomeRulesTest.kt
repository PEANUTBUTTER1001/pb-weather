package com.pbweather.weather.domain

import com.pbweather.weather.data.remote.kma.KmaParser
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRulesTest {
    @Test fun `display rounding is half up and never negative zero`() {
        assertEquals("0.0", com.pbweather.weather.ui.components.number(-0.04))
        assertEquals("0.1", com.pbweather.weather.ui.components.number(0.05))
        assertEquals("10.0", com.pbweather.weather.ui.components.number(9.95))
        assertEquals("-10.0", com.pbweather.weather.ui.components.number(-9.95))
        assertNull(com.pbweather.weather.ui.components.number(Double.NaN))
    }

    @Test fun `summary uses future six entries earliest maximum tie and temperature fallback`() {
        val hours = listOf(
            ForecastHour(1, 5.0, probability = 100),
            ForecastHour(3, 6.0, probability = 60),
            ForecastHour(4, 7.0, probability = 60),
            ForecastHour(2, 8.0, probability = 10)
        )
        assertEquals(3L, ForecastSummary.select(hours, 1)?.at)
        assertEquals(2L, ForecastSummary.select(hours.map { it.copy(probability = null) }, 1)?.at)
        assertNull(ForecastSummary.select(hours.map { it.copy(probability = null, temperature = null) }, 1))
        val seven = (1L..7L).map { ForecastHour(it, 10.0, probability = if (it == 7L) 100 else 0) }
        assertEquals(1L, ForecastSummary.select(seven, 0)?.at)
    }

    @Test fun `apparent calculations validate seasonal inputs and withhold unconfirmed winter spec`() {
        val summer = Instant.parse("2026-07-01T05:00:00Z").toEpochMilli()
        val winter = Instant.parse("2026-01-01T05:00:00Z").toEpochMilli()
        val warm = ApparentTemperature.calculate(ObservedWeather(temperature = 30.0, humidity = 70.0), summer)
        assertEquals(ApparentStatus.AVAILABLE, warm.status)
        assertEquals(31.3, warm.celsius!!, .1)
        val cold = ObservedWeather(temperature = -10.0, windMs = 5.0)
        assertEquals(ApparentStatus.INPUT_SPEC_UNCONFIRMED, ApparentTemperature.calculate(cold, winter).status)
        assertEquals(-17.4, ApparentTemperature.calculate(cold, winter, true).celsius!!, .1)
        assertEquals(ApparentStatus.OUTSIDE_RANGE, ApparentTemperature.calculate(cold.copy(temperature = 11.0), winter).status)
        assertEquals(ApparentStatus.MISSING, ApparentTemperature.calculate(cold.copy(windMs = Double.NaN), winter).status)
        assertEquals(ApparentStatus.MISSING, ApparentTemperature.calculate(cold, null).status)
    }

    @Test fun `observed variables from a different base time cannot enter apparent calculation`() {
        fun row(text: String) = Json.parseToJsonElement(text) as JsonObject
        val data = KmaParser.observed(
            listOf(
                row("""{"baseDate":"20261125","baseTime":"0100","category":"T1H","obsrValue":"-10"}"""),
                row("""{"baseDate":"20261125","baseTime":"0000","category":"WSD","obsrValue":"5"}""")
            )
        )
        assertEquals(-10.0, data.temperature)
        assertNull(data.windMs)
    }
}

package com.pbweather.weather.data.remote

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.data.remote.kma.MidParser
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.domain.WeeklyForecast
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MidForecastTest {
    private val release = Release(LocalDateTime.parse("2026-11-24T18:00:00"))
    private val meta = SourceMeta(Provider.KMA, DataKind.MID_LAND, "11B00000", 0, issuedAt = release.epochMillis)

    @Test fun `midnight does not shift previous release offsets and whole day is not duplicated`() {
        val row = Json.parseToJsonElement(
            """{"wf5Am":"맑음","rnSt5Am":"10","wf8":"흐림","rnSt8":"60"}"""
        ) as JsonObject
        val days = MidParser.days(row, meta, release)
        assertEquals(listOf("2026-11-29", "2026-12-02"), days.map { it.date })
        val whole = days.last()
        assertNull(whole.morningWeather)
        assertNull(whole.afternoonProbability)
        assertEquals("흐림", whole.wholeDayWeather)
        assertEquals(60, whole.wholeDayProbability)
        assertEquals(86400000L, whole.origins.getValue("wholeDay").let { it.validTo!! - it.validFrom!! })
    }

    @Test fun `near forecast takes priority without erasing mid extrema and missing dates remain missing`() {
        val today = LocalDate.parse("2026-11-25")
        val mid = listOf(DailyForecast("2026-11-29", minimum = 2.0, maximum = 12.0, wholeDayWeather = "맑음"))
        val short = listOf(DailyForecast("2026-11-29", minimum = 3.0))
        val result = WeeklyForecast.merge(short, emptyList(), mid, emptyList(), today)
        assertEquals(11, result.size)
        assertEquals(today.toString(), result.first().date)
        assertEquals("2026-12-05", result.last().date)
        assertNull(result.first().minimum)
        assertTrue(result.first().origins.isEmpty())
        val overlap = result.first { it.date == "2026-11-29" }
        assertEquals(3.0, overlap.minimum)
        assertEquals(12.0, overlap.maximum)
    }

    @Test fun `morning release and temperature null are handled by provided fields only`() {
        val row = Json.parseToJsonElement("""{"taMin4":"-","taMax4":"12","taMin10":"-3"}""") as JsonObject
        val days = MidParser.days(row, meta.copy(kind = DataKind.MID_TA), Release(release.at.withHour(6)))
        assertEquals("2026-11-28", days.first().date)
        assertNull(days.first().minimum)
        assertEquals(12.0, days.first().maximum)
        assertEquals(-3.0, days.last().minimum)
    }
}

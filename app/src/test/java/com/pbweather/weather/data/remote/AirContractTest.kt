package com.pbweather.weather.data.remote

import com.pbweather.weather.data.remote.airkorea.AirParser
import com.pbweather.weather.domain.AirRules
import com.pbweather.weather.domain.AirTrend
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirContractTest {
    private fun row(value: String) = Json.parseToJsonElement(value) as JsonObject

    @Test fun `midnight duplicate gaps and independent PM flags are preserved`() {
        val data = AirParser.readings(
            "station",
            listOf(
                row("""{"dataTime":"2026-11-24 24:00","pm10Value":"-","pm25Value":"8"}"""),
                row("""{"dataTime":"2026-11-25 00:00","pm10Value":"20","pm25Value":"8","pm10Grade1h":"2","pm10Grade":"4"}"""),
                row("""{"dataTime":"2026-11-25 01:00","pm10Value":"22","pm25Value":"9","pm10Flag":"점검및교정"}"""),
                row("""{"dataTime":"2026-11-25 03:00","pm10Value":"24","pm25Value":"10"}""")
            )
        )
        assertEquals(3, data.readings.size)
        assertEquals(2, data.readings.first().pm10Grade)
        assertNull(data.readings[1].pm10)
        assertNull(data.readings[1].pm10Grade)
        assertEquals(9.0, data.readings[1].pm25)
        assertEquals("점검및교정", data.readings[1].pm10Flag)
        assertEquals(listOf(1, 1), AirTrend.segments(data, false).map { it.size })
        assertEquals(listOf(2, 1), AirTrend.segments(data, true).map { it.size })
    }

    @Test fun `catalog version coordinates and ambiguous names are validated`() {
        val stations = AirParser.stations(
            listOf(
                row("""{"stationName":"unique","dmX":"126.9","dmY":"37.5"}"""),
                row("""{"stationName":"wrong_version","dmX":"37.5","dmY":"126.9"}"""),
                row("""{"stationName":"duplicate","dmX":"126.8","dmY":"37.4"}"""),
                row("""{"stationName":"duplicate","dmX":"127.8","dmY":"36.4"}""")
            ),
            123
        )
        assertEquals(1, stations.size)
        assertEquals(37.5, stations.single().latitude, 0.0)
        assertEquals(126.9, stations.single().longitude, 0.0)
    }

    @Test fun `fallback must improve latest coverage and cannot blend two stations`() {
        val first = AirParser.readings("first", listOf(row("""{"dataTime":"2026-11-25 00:00","pm10Value":"10"}""")))
        val equal = AirParser.readings("equal", listOf(row("""{"dataTime":"2026-11-25 00:00","pm25Value":"10"}""")))
        val better = equal.copy(readings = equal.readings.map { it.copy(pm10 = 20.0) })
        assertFalse(AirRules.useFallback(first, equal))
        assertTrue(AirRules.useFallback(first, better))
        assertEquals("equal", better.station)
    }

    @Test fun `only latest 24 actual entries are returned without filling missing hours`() {
        val items = (0..30).map { index ->
            val date = java.time.LocalDateTime.parse("2026-11-23T00:00:00").plusHours(index.toLong())
            row("""{"dataTime":"${date.toLocalDate()} ${date.toLocalTime()}","pm10Value":"-"}""")
        }
        val data = AirParser.readings("s", items.reversed())
        assertEquals(24, data.readings.size)
        assertTrue(data.readings.zipWithNext().all { (a, b) -> a.at < b.at })
        assertTrue(data.readings.all { it.pm10 == null && it.pm10Grade == null })
    }
}

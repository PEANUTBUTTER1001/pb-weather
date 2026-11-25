package com.pbweather.weather.data.remote

import com.pbweather.weather.data.remote.kma.AsosParser
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.HistorySummary
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.RangeProblem
import com.pbweather.weather.domain.YesterdayComparison
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryContractTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), ZoneOffset.UTC)

    @Test fun `KST midnight window is ten inclusive past dates and validation rejects today`() {
        val range = HistoryWindow.recent(clock)
        assertEquals("2026-11-15", range.start.toString())
        assertEquals("2026-11-24", range.end.toString())
        assertEquals(10, range.dates.size)
        assertEquals(RangeProblem.OUTSIDE_WINDOW, HistoryWindow.validate("2026-11-15", "2026-11-25", clock).error)
        assertEquals(RangeProblem.REVERSED, HistoryWindow.validate("2026-11-24", "2026-11-23", clock).error)
        assertEquals(RangeProblem.INVALID_FORMAT, HistoryWindow.validate("2026-02-30", "2026-11-23", clock).error)
    }

    @Test fun `missing dates form only contiguous gaps`() {
        val range = HistoryWindow.recent(clock)
        val available = setOf("2026-11-16", "2026-11-17", "2026-11-21", "2026-11-22")
        assertEquals(
            listOf("2026-11-15/2026-11-15", "2026-11-18/2026-11-20", "2026-11-23/2026-11-24"),
            HistoryWindow.missing(range, available).map { "${it.start}/${it.end}" }
        )
    }

    @Test fun `missing rainfall stays null and delta uses forecast extrema only`() {
        val range = DateRange(LocalDate.parse("2026-11-24"), LocalDate.parse("2026-11-24"))
        val row = Json.parseToJsonElement(
            """{"stnId":"108","tm":"2026-11-24","maxTa":"12.0","minTa":"2.0","avgTa":"-","sumRn":""}"""
        ) as JsonObject
        val observation = AsosParser.observations(listOf(row), "108", range, 123).single()
        assertNull(observation.precipitation)
        val stats = HistorySummary.calculate(listOf(observation))
        assertEquals(0, stats.rainfallDays)
        assertEquals(1, stats.maximumDays)
        assertNull(stats.rainfallTotal)
        val delta = YesterdayComparison.delta(DailyForecast("2026-11-25", minimum = 3.0, maximum = 14.0), observation)
        assertEquals(2.0, delta.high)
        assertEquals(1.0, delta.low)
        assertNull(YesterdayComparison.delta(null, observation).high)
        assertNull(observation.meta.observedAt)
        assertEquals(86400000L, observation.meta.let { it.validTo!! - it.validFrom!! })
        assertEquals(emptyList<Any>(), AsosParser.observations(listOf(row), "109", range, 123))
    }
}

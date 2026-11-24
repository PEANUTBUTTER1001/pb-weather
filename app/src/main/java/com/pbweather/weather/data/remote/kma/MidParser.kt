package com.pbweather.weather.data.remote.kma

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.remote.integer
import com.pbweather.weather.data.remote.number
import com.pbweather.weather.data.remote.text
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.SourceMeta
import kotlinx.serialization.json.JsonObject

object MidParser {
    /** Day offsets are anchored to the release date, including a previous-day 18:00 release after midnight. */
    fun days(row: JsonObject, meta: SourceMeta, release: Release): List<DailyForecast> {
        val start = if (release.at.hour < 18) 4 else 5
        return (start..10).mapNotNull { offset ->
            val day = release.at.toLocalDate().plusDays(offset.toLong())
            val date = day.toString()
            val startAt = day.atStartOfDay(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
            val daily = meta.copy(validFrom = startAt, validTo = startAt + 86400000L)
            fun probability(field: String) = row.integer(field)?.takeIf { it in 0..100 }
            fun text(field: String) = row.text(field)?.trim()?.takeIf { it.isNotBlank() && it != "-" && it != "null" }
            val temperature = meta.kind == DataKind.MID_TA
            val min = if (temperature) row.number("taMin$offset") else null
            val max = if (temperature) row.number("taMax$offset") else null
            val morning = if (temperature) null else text("wf${offset}Am")
            val afternoon = if (temperature) null else text("wf${offset}Pm")
            val whole = if (temperature) null else text("wf$offset")
            val am = if (temperature) null else probability("rnSt${offset}Am")
            val pm = if (temperature) null else probability("rnSt${offset}Pm")
            val all = if (temperature) null else probability("rnSt$offset")
            val origins = buildMap {
                if (min != null) put("minimum", daily)
                if (max != null) put("maximum", daily)
                if (morning != null || am != null) put("morning", daily.copy(validTo = startAt + 43200000L))
                if (afternoon != null || pm != null) put("afternoon", daily.copy(validFrom = startAt + 43200000L))
                if (whole != null || all != null) put("wholeDay", daily)
            }
            if (origins.isEmpty()) null else DailyForecast(date, min, max, morning, afternoon, whole, am, pm, all, origins)
        }
    }
}

package com.pbweather.weather.data.remote.kma

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.remote.number
import com.pbweather.weather.data.remote.text
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.SourceMeta
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject

object AsosParser {
    fun observations(items: List<JsonObject>, station: String, range: DateRange, receivedAt: Long): List<DailyObservation> =
        items.mapNotNull { row ->
            if (row.text("stnId") != station) return@mapNotNull null
            val date = runCatching { LocalDate.parse(row.text("tm")) }.getOrNull() ?: return@mapNotNull null
            if (date < range.start || date > range.end) return@mapNotNull null
            val begin = date.atStartOfDay(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
            val meta = SourceMeta(
                Provider.KMA,
                DataKind.HISTORY,
                station,
                receivedAt,
                validFrom = begin,
                validTo = begin + 86400000,
                version = "${station}_$date"
            )
            DailyObservation(
                station, date.toString(), row.number("avgTa"), row.number("maxTa"), row.number("minTa"),
                row.number("sumRn")?.takeIf { it >= 0 }, row.number("avgWs")?.takeIf { it >= 0 },
                row.number("avgRhm")?.takeIf { it in 0.0..100.0 }, meta
            )
        }.distinctBy { it.date }.sortedBy { it.date }
}

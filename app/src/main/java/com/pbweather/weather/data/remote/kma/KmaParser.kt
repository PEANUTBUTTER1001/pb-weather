package com.pbweather.weather.data.remote.kma

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.remote.integer
import com.pbweather.weather.data.remote.number
import com.pbweather.weather.data.remote.text
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.ForecastData
import com.pbweather.weather.domain.ForecastHour
import com.pbweather.weather.domain.ObservedWeather
import com.pbweather.weather.domain.Precipitation
import com.pbweather.weather.domain.PrecipitationKind
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.domain.WeatherCondition
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonObject

object KmaParser {
    fun at(date: String, rawTime: String): Long? = runCatching {
        val time = if (rawTime.length > 4) rawTime.padStart(6, '0').take(4) else rawTime.padStart(4, '0')
        val day = LocalDate.parse(date.replace("-", ""), DateTimeFormatter.BASIC_ISO_DATE)
        val hour = time.take(2).toInt()
        val minute = time.takeLast(2).toInt()
        if (hour == 24 && minute == 0) {
            day.plusDays(1).atStartOfDay(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
        } else {
            day.atTime(hour, minute).atZone(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
        }
    }.getOrNull()
    fun metadata(items: List<JsonObject>, release: Release, kind: DataKind, key: String, receivedAt: Long): SourceMeta {
        val first = items.firstOrNull()
        val base = first?.text("baseDate")?.let { date -> first.text("baseTime")?.let { at(date, it) } } ?: release.epochMillis
        return SourceMeta(
            Provider.KMA,
            kind,
            key,
            receivedAt,
            issuedAt = if (kind == DataKind.NOWCAST) null else base,
            observedAt = if (kind == DataKind.NOWCAST) base else null,
            version = Instant.ofEpochMilli(base).toString()
        )
    }
    fun precipitation(raw: String?, qualitative: Boolean = false): Precipitation? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() && it != "-" && it != "null" } ?: return null
        if (qualitative) return Precipitation(text, PrecipitationKind.QUALITATIVE)
        if (text in listOf("강수없음", "강수 없음")) return Precipitation(text, PrecipitationKind.NONE, 0.0)
        val numeric = text.removeSuffix("mm").trim().toDoubleOrNull()
        if (numeric != null) {
            return numeric.takeIf { it.isFinite() && it >= 0 }?.let {
                Precipitation(text, PrecipitationKind.EXACT, it)
            }
        }
        val type = if ('~' in text || "미만" in text || "이상" in text) PrecipitationKind.RANGE else PrecipitationKind.QUALITATIVE
        return Precipitation(text, type)
    }
    fun observed(items: List<JsonObject>): ObservedWeather {
        val base = items.firstOrNull()
        val fields = items.filter {
            it.text("baseDate") == base?.text("baseDate") && it.text("baseTime") == base?.text("baseTime")
        }.associateBy { it.text("category") }
        fun value(name: String) = fields[name]?.number("obsrValue")
        return ObservedWeather(
            value("T1H"),
            value("REH")?.takeIf { it in 0.0..100.0 },
            value("WSD")?.takeIf { it >= 0 },
            value("VEC")?.takeIf { it in 0.0..360.0 },
            precipitation(fields["RN1"]?.text("obsrValue")),
            fields["PTY"]?.integer("obsrValue")
        )
    }
    fun forecast(items: List<JsonObject>, meta: SourceMeta, release: Release): ForecastData {
        val timed = items.mapNotNull { item ->
            val date = item.text("fcstDate") ?: return@mapNotNull null
            val time = item.text("fcstTime") ?: return@mapNotNull null
            at(date, time)?.let { it to item }
        }.groupBy({ it.first }, { it.second })
        val hours = timed.map { (time, values) ->
            val fields = values.associateBy { it.text("category") }
            val temperature = fields[if (meta.kind == DataKind.ULTRA) "T1H" else "TMP"]?.number("fcstValue")
            val sky = fields["SKY"]?.integer("fcstValue")?.takeIf { it in listOf(1, 3, 4) }
            val pty = fields["PTY"]?.integer("fcstValue")
            val pop = fields["POP"]?.integer("fcstValue")?.takeIf { it in 0..100 }
            val day = Instant.ofEpochMilli(time).atZone(ReleaseTimeCalculator.KST).toLocalDate()
            val extendedFrom = release.at.toLocalDate().plusDays(if (release.at.hour >= 17) 4 else 3)
            val precip = precipitation(
                fields[if (meta.kind == DataKind.ULTRA) "RN1" else "PCP"]?.text("fcstValue"),
                meta.kind == DataKind.SHORT && day >= extendedFrom
            )
            val fieldMeta = meta.copy(validFrom = time)
            val origins = buildMap {
                if (temperature != null) put("temperature", fieldMeta)
                if (sky != null) put("sky", fieldMeta)
                if (pty != null) put("precipitationType", fieldMeta)
                if (pop != null) put("probability", fieldMeta)
                if (precip != null) put("precipitation", fieldMeta)
            }
            ForecastHour(time, temperature, sky, pty, pop, precip, origins)
        }.filter { it.origins.isNotEmpty() }.sortedBy { it.at }
        val days = items.groupBy { it.text("fcstDate") }.mapNotNull { (date, values) ->
            val day = runCatching { LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull() ?: return@mapNotNull null
            val minimum = values.firstOrNull { it.text("category") == "TMN" }?.number("fcstValue")
            val maximum = values.firstOrNull { it.text("category") == "TMX" }?.number("fcstValue")
            val relevant = hours.filter { Instant.ofEpochMilli(it.at).atZone(ReleaseTimeCalculator.KST).toLocalDate() == day }
            val morning = relevant.filter { Instant.ofEpochMilli(it.at).atZone(ReleaseTimeCalculator.KST).hour < 12 }
            val afternoon = relevant.filter { Instant.ofEpochMilli(it.at).atZone(ReleaseTimeCalculator.KST).hour >= 12 }
            val am = morning.maxByOrNull { it.probability ?: -1 }
            val pm = afternoon.maxByOrNull { it.probability ?: -1 }
            DailyForecast(
                day.toString(), minimum, maximum,
                morningWeather = am?.let { WeatherCondition.from(it.sky, it.precipitationType).name },
                afternoonWeather = pm?.let { WeatherCondition.from(it.sky, it.precipitationType).name },
                morningProbability = morning.mapNotNull { it.probability }.maxOrNull(),
                afternoonProbability = afternoon.mapNotNull { it.probability }.maxOrNull(),
                origins = buildMap {
                    if (minimum != null) put("minimum", meta)
                    if (maximum != null) put("maximum", meta)
                    am?.origins?.values?.firstOrNull()?.let { put("morning", it) }
                    pm?.origins?.values?.firstOrNull()?.let { put("afternoon", it) }
                },
                periodMaximum = true
            )
        }.sortedBy { it.date }
        return ForecastData(hours, days)
    }
}

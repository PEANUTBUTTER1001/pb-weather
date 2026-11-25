package com.pbweather.weather.domain

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(start <= end && ChronoUnit.DAYS.between(start, end) < 10)
    }
    val dates: List<LocalDate> get() = (0L..ChronoUnit.DAYS.between(start, end)).map(start::plusDays)
}
enum class RangeProblem { INVALID_FORMAT, REVERSED, OUTSIDE_WINDOW }
data class RangeValidation(val range: DateRange? = null, val error: RangeProblem? = null)
object HistoryWindow {
    /** Ten inclusive KST dates, ending yesterday. Today's aggregate observations are never substituted by nowcast. */
    fun recent(clock: Clock): DateRange {
        val today = LocalDate.now(clock.withZone(ReleaseTimeCalculator.KST))
        return DateRange(today.minusDays(10), today.minusDays(1))
    }
    fun validate(start: String, end: String, clock: Clock): RangeValidation {
        val from = runCatching { LocalDate.parse(start) }.getOrNull()
        val to = runCatching { LocalDate.parse(end) }.getOrNull()
        if (from == null || to == null) return RangeValidation(error = RangeProblem.INVALID_FORMAT)
        if (from > to) return RangeValidation(error = RangeProblem.REVERSED)
        val window = recent(clock)
        if (from < window.start || to > window.end) return RangeValidation(error = RangeProblem.OUTSIDE_WINDOW)
        return RangeValidation(DateRange(from, to))
    }
    fun missing(range: DateRange, available: Set<String>): List<DateRange> {
        val result = mutableListOf<DateRange>()
        var start: LocalDate? = null
        for (day in range.dates) {
            if (day.toString() !in available) {
                if (start == null) start = day
            } else if (start != null) {
                result += DateRange(start, day.minusDays(1))
                start = null
            }
        }
        if (start != null) result += DateRange(start, range.end)
        return result
    }
}
data class TemperatureDelta(val high: Double?, val low: Double?)
object YesterdayComparison {
    /** Forecast extremes and ASOS yesterday extremes remain different data sources; missing input produces no delta. */
    fun delta(today: DailyForecast?, yesterday: DailyObservation?): TemperatureDelta = TemperatureDelta(
        today?.maximum?.let { high -> yesterday?.maximum?.let { high - it } },
        today?.minimum?.let { low -> yesterday?.minimum?.let { low - it } }
    )
}
data class HistoryStatistics(
    val maximumDays: Int,
    val minimumDays: Int,
    val averageDays: Int,
    val rainfallDays: Int,
    val highest: Pair<String, Double>?,
    val lowest: Pair<String, Double>?,
    val rainfallTotal: Double?
)
object HistorySummary {
    /** Rain totals count only actual valid daily values. A fully missing series is null rather than zero. */
    fun calculate(rows: List<DailyObservation>): HistoryStatistics {
        val rain = rows.mapNotNull { it.precipitation }
        return HistoryStatistics(
            rows.count { it.maximum != null },
            rows.count { it.minimum != null },
            rows.count { it.average != null },
            rain.size,
            rows.filter { it.maximum != null }.maxByOrNull { it.maximum!! }?.let { it.date to it.maximum!! },
            rows.filter { it.minimum != null }.minByOrNull { it.minimum!! }?.let { it.date to it.minimum!! },
            rain.takeIf { it.isNotEmpty() }?.sum()
        )
    }
}

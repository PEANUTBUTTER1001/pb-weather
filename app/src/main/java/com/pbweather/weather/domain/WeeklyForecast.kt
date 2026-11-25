package com.pbweather.weather.domain

import java.time.LocalDate

object WeeklyForecast {
    /** Always keep eleven calendar slots. A missing source creates a missing-valued slot, never interpolation. */
    fun merge(
        short: List<DailyForecast>,
        extremes: List<DailyForecast>,
        land: List<DailyForecast>,
        temperature: List<DailyForecast>,
        today: LocalDate
    ): List<DailyForecast> {
        val mid = ForecastMerger.days(temperature, land)
        val near = ForecastMerger.days(short, extremes)
        val byDate = ForecastMerger.days(near, mid).associateBy { it.date }
        return (0L..10L).map { today.plusDays(it).toString() }.map { date -> byDate[date] ?: DailyForecast(date) }
    }
}

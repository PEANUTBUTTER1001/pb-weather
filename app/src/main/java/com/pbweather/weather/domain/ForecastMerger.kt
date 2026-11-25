package com.pbweather.weather.domain

object ForecastMerger {
    /** Merge per field; an absent preferred value must never erase a valid short forecast. */
    fun hours(short: List<ForecastHour>, ultra: List<ForecastHour>, now: Long): List<ForecastHour> {
        val all = short.associateBy { it.at }.toMutableMap()
        for (value in ultra.filter { it.at <= now + 6 * 3600000L }) {
            val old = all[value.at]
            all[value.at] = if (old == null) {
                value
            } else {
                old.copy(
                    temperature = value.temperature ?: old.temperature,
                    sky = value.sky ?: old.sky,
                    precipitationType = value.precipitationType ?: old.precipitationType,
                    probability = value.probability ?: old.probability,
                    precipitation = value.precipitation ?: old.precipitation,
                    origins = old.origins + value.origins
                )
            }
        }
        return all.values.sortedBy { it.at }
    }
    fun days(primary: List<DailyForecast>, fallback: List<DailyForecast>): List<DailyForecast> {
        val all = fallback.associateBy { it.date }.toMutableMap()
        for (day in primary) {
            val old = all[day.date]
            all[day.date] = if (old == null) {
                day
            } else {
                old.copy(
                    minimum = day.minimum ?: old.minimum, maximum = day.maximum ?: old.maximum,
                    morningWeather = day.morningWeather ?: old.morningWeather,
                    afternoonWeather = day.afternoonWeather ?: old.afternoonWeather,
                    wholeDayWeather = day.wholeDayWeather ?: old.wholeDayWeather,
                    morningProbability = day.morningProbability ?: old.morningProbability,
                    afternoonProbability = day.afternoonProbability ?: old.afternoonProbability,
                    wholeDayProbability = day.wholeDayProbability ?: old.wholeDayProbability,
                    origins = old.origins + day.origins,
                    periodMaximum = day.periodMaximum
                )
            }
        }
        return all.values.sortedBy { it.date }
    }
}

package com.pbweather.weather.domain

object ForecastSummary {
    /** At most six actual future forecast entries; maximum numeric POP, earliest tie, then earliest valid temperature. */
    fun select(hours: List<ForecastHour>, now: Long): ForecastHour? {
        val future = hours.filter { it.at > now }.sortedBy { it.at }.take(6)
        return future.filter { it.probability != null }.maxByOrNull { it.probability!! }
            ?: future.firstOrNull { it.temperature != null }
    }
}

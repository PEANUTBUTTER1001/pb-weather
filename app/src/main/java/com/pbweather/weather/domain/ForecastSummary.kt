package com.pbweather.weather.domain

object ForecastSummary {
    /** At most six actual future forecast entries; maximum numeric POP, earliest tie, then earliest valid temperature. */
    fun select(hours: List<ForecastHour>, now: Long): ForecastHour? {
        val future = hours.filter { it.at > now }.sortedBy { it.at }.take(6)
        return future.mapNotNull { hour -> hour.probability?.let { hour to it } }
            .maxByOrNull { it.second }?.first
            ?: future.firstOrNull { it.temperature != null }
    }
}

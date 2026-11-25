package com.pbweather.weather.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class ForecastState(
    val now: Resource<ObservedWeather> = Resource(),
    val hourly: Resource<ForecastData> = Resource(),
    val weekly: Resource<List<DailyForecast>> = Resource()
)
interface ForecastSource {
    fun observe(region: Region): Flow<ForecastState>
    suspend fun refresh(region: Region)
}
class ObserveForecast @Inject constructor(private val source: ForecastSource) {
    operator fun invoke(region: Region): Flow<ForecastState> = source.observe(region)
    suspend fun refresh(region: Region) = source.refresh(region)
}
enum class SkyCondition { CLEAR, PARTLY, CLOUDY, RAIN, SLEET, SNOW, UNKNOWN }
object WeatherCondition {
    fun from(sky: Int?, precipitationType: Int?): SkyCondition = when (precipitationType) {
        1, 4, 5 -> SkyCondition.RAIN
        2, 6 -> SkyCondition.SLEET
        3, 7 -> SkyCondition.SNOW
        else -> when (sky) {
            1 -> SkyCondition.CLEAR
            3 -> SkyCondition.PARTLY
            4 -> SkyCondition.CLOUDY
            else -> SkyCondition.UNKNOWN
        }
    }
}

package com.pbweather.weather.data.repository

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.domain.ForecastSource
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.Region
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class WidgetData(
    val preferences: AppPreferences,
    val region: Region?,
    val forecast: ForecastState = ForecastState(),
    val today: String
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRepository @Inject constructor(
    private val preferences: PreferencesRepository,
    private val forecasts: ForecastSource,
    private val clock: Clock
) {
    fun observe() = preferences.preferences.map { prefs -> prefs.favorites.firstOrNull { it.id == prefs.primaryId } }
        .distinctUntilChanged().flatMapLatest { region ->
            combine(preferences.preferences, region?.let(forecasts::observe) ?: flowOf(ForecastState())) { prefs, data ->
                WidgetData(prefs, region, data, LocalDate.now(clock.withZone(ReleaseTimeCalculator.KST)).toString())
            }
        }

    suspend fun refreshPrimary() {
        val prefs = preferences.preferences.first()
        val region = prefs.favorites.firstOrNull { it.id == prefs.primaryId } ?: return
        forecasts.refresh(region)
        val data = forecasts.observe(region).first()
        val failed = data.now.data == null || data.now.error != null || data.hourly.error != null
        preferences.update { current -> if (current.primaryId == region.id) current.copy(widgetError = failed) else current }
    }
}

package com.pbweather.weather.di

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.domain.ForecastSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataEntryPoint {
    fun api(): ApiClient
    fun releases(): ReleaseTimeCalculator
    fun clock(): Clock
    fun cache(): CacheStore
    fun preferences(): PreferencesRepository
    fun forecasts(): ForecastSource
    fun widgets(): com.pbweather.weather.data.repository.WidgetRepository
    fun traffic(): ApiTraffic
}

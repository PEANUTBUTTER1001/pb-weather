package com.pbweather.weather.di

import com.pbweather.weather.data.repository.AirQualityRepository
import com.pbweather.weather.domain.AirQualitySource
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AirModule {
    @Provides @Singleton
    fun air(repository: AirQualityRepository): AirQualitySource = repository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AirEntryPoint {
    fun airQuality(): AirQualitySource
}

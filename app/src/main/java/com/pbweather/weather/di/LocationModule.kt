package com.pbweather.weather.di

import com.pbweather.weather.data.repository.LocationRepository
import com.pbweather.weather.domain.LocationSource
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides @Singleton
    fun locations(repository: LocationRepository): LocationSource = repository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationEntryPoint {
    fun locations(): LocationSource
}

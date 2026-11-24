package com.pbweather.weather.di

import com.pbweather.weather.data.repository.HistoryRepository
import com.pbweather.weather.domain.HistorySource
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HistoryModule {
    @Provides @Singleton
    fun history(repository: HistoryRepository): HistorySource = repository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HistoryEntryPoint {
    fun history(): HistorySource
}

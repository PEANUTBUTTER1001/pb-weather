package com.pbweather.weather.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.pbweather.weather.data.local.AssetImporter
import com.pbweather.weather.data.local.CachePolicy
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.local.WeatherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun clock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): WeatherDatabase =
        Room.databaseBuilder(context, WeatherDatabase::class.java, "weather.db").build()

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context, json: Json): PreferencesRepository =
        PreferencesRepository(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }, json)

    @Provides @Singleton
    fun importer(@ApplicationContext context: Context, db: WeatherDatabase) = AssetImporter(context, db)

    @Provides @Singleton
    fun cachePolicy(clock: Clock) = CachePolicy(clock)
}

package com.pbweather.weather.data.repository

import com.pbweather.weather.data.local.AssetImporter
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.location.CoarseLocationProvider
import com.pbweather.weather.data.location.RegionResolver
import com.pbweather.weather.domain.LocationSource
import com.pbweather.weather.domain.Region
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LocationRepository @Inject constructor(
    private val db: WeatherDatabase,
    private val importer: AssetImporter,
    private val provider: CoarseLocationProvider
) : LocationSource {
    private val mutex = Mutex()
    private var catalog: RegionResolver? = null
    private suspend fun resolver(): RegionResolver = mutex.withLock {
        catalog ?: run {
            importer.ensureLoaded()
            val dao = db.weatherDao()
            RegionResolver(dao.regions(), dao.asosStations(), dao.midRegions()).also { catalog = it }
        }
    }
    override suspend fun search(query: String): List<Region> = withContext(Dispatchers.IO) {
        val value = query.trim().replace(Regex("\\s+"), " ")
        if (value.length !in 2..30) return@withContext emptyList()
        val mapping = resolver()
        db.weatherDao().search(value).map(mapping::named)
    }
    override suspend fun current(): Region = withContext(Dispatchers.Default) { provider.resolve(resolver()) }
    override fun permissionGranted() = provider.permissionGranted()
    override fun serviceEnabled() = provider.serviceEnabled()
}

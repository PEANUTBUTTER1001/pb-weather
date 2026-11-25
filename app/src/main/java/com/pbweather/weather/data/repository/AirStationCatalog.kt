package com.pbweather.weather.data.repository

import androidx.room.withTransaction
import com.pbweather.weather.data.local.AirStationEntity
import com.pbweather.weather.data.local.CachePolicy
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.airkorea.AirParser
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class AirStationCatalog @Inject constructor(
    private val db: WeatherDatabase,
    private val api: ApiClient,
    private val clock: Clock,
    private val policy: CachePolicy
) {
    private val mutex = Mutex()
    suspend fun stations(): List<AirStationEntity> = mutex.withLock {
        val existing = db.weatherDao().airStations()
        val now = clock.instant().toEpochMilli()
        if (existing.isNotEmpty() && existing.all { now < policy.expiry(DataKind.STATIONS, it.fetchedAt) }) return@withLock existing
        try {
            val items = api.fetch(
                "B552584/MsrstnInfoInqireSvc/getMsrstnList",
                mapOf("returnType" to "json", "numOfRows" to "1000", "ver" to "1.1"),
                "air_stations"
            )
            val fresh = AirParser.stations(items, now)
            if (fresh.isEmpty()) throw DataException(DomainError.INVALID_RESPONSE)
            db.withTransaction {
                db.weatherDao().clearAirStations()
                db.weatherDao().saveAirStations(fresh)
            }
            fresh
        } catch (
            cancelled: CancellationException
        ) {
            throw cancelled
        } catch (error: Exception) {
            if (existing.isNotEmpty()) existing else throw error
        }
    }
}

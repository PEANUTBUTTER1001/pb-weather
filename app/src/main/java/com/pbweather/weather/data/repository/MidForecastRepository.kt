package com.pbweather.weather.data.repository

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.kma.MidParser
import com.pbweather.weather.data.remote.text
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.encodeToString

@Singleton
class MidForecastRepository @Inject constructor(
    private val cache: CacheStore,
    private val api: ApiClient,
    private val releases: ReleaseTimeCalculator,
    private val coordinator: RequestCoordinator,
    private val clock: Clock
) {
    fun snapshots(region: Region) = combine(
        cache.dao.observeSnapshots(key(region, DataKind.MID_LAND)),
        cache.dao.observeSnapshots(key(region, DataKind.MID_TA))
    ) { a, b -> a + b }
    fun key(region: Region, kind: DataKind): String = when (kind) {
        DataKind.MID_LAND -> "mid_land_${region.midLandRegId.orEmpty()}"
        else -> "mid_ta_${region.midTaRegId.orEmpty()}"
    }
    suspend fun refresh(region: Region) = supervisorScope {
        listOf(DataKind.MID_LAND, DataKind.MID_TA).map { kind -> async { refreshKind(region, kind) } }.awaitAll()
    }
    private suspend fun refreshKind(region: Region, kind: DataKind) = coordinator.refresh(key(region, kind), kind) {
        val regId = (if (kind == DataKind.MID_LAND) region.midLandRegId else region.midTaRegId)
            ?: throw DataException(DomainError.REGION_MAPPING)
        val latest = releases.latest(kind)
        suspend fun call(release: Release) = api.fetch(
            "1360000/MidFcstInfoService/${if (kind == DataKind.MID_LAND) "getMidLandFcst" else "getMidTa"}",
            mapOf("dataType" to "JSON", "numOfRows" to "10", "regId" to regId, "tmFc" to release.version),
            "mid"
        )
        val (items, release) = try {
            call(latest) to latest
        } catch (error: DataException) {
            if (error.reason != DomainError.NO_DATA) throw error
            val previous = releases.previous(kind, latest)
            call(previous) to previous
        }
        val row = items.firstOrNull { it.text("regId") == regId } ?: throw DataException(DomainError.INVALID_RESPONSE)
        val meta = SourceMeta(
            Provider.KMA,
            kind,
            regId,
            clock.instant().toEpochMilli(),
            issuedAt = release.epochMillis,
            version = release.version
        )
        val days: List<DailyForecast> = MidParser.days(row, meta, release)
        if (days.isEmpty()) throw DataException(DomainError.NO_DATA)
        LoadedSnapshot(cache.json.encodeToString(days), meta)
    }
}

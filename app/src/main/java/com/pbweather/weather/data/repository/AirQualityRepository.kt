package com.pbweather.weather.data.repository

import com.pbweather.weather.core.geo.NearestFinder
import com.pbweather.weather.core.geo.StationPoint
import com.pbweather.weather.data.local.AirLinkEntity
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.NetworkMonitor
import com.pbweather.weather.data.remote.airkorea.AirParser
import com.pbweather.weather.domain.AirQualityData
import com.pbweather.weather.domain.AirQualitySource
import com.pbweather.weather.domain.AirRules
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

@Singleton
class AirQualityRepository @Inject constructor(
    private val cache: CacheStore,
    private val api: ApiClient,
    private val catalog: AirStationCatalog,
    private val coordinator: RequestCoordinator,
    private val network: NetworkMonitor,
    private val clock: Clock
) : AirQualitySource {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val states = MutableStateFlow<Map<String, RequestStatus>>(emptyMap())
    private val ticks = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(60000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(region: Region): Flow<Resource<AirQualityData>> {
        val linked = cache.dao.observeAirLink(region.key).flatMapLatest { link ->
            if (link == null) {
                flowOf(null to null)
            } else {
                cache.dao.observeSnapshots("air_${link.stationName}").let { rows ->
                    combine(flowOf(link), rows) { l, r -> l to r.firstOrNull { it.kind == DataKind.AIR.name } }
                }
            }
        }
        return combine(linked, states, network.connected, ticks) { (link, row), status, online, _ ->
            val data = row?.let { runCatching { cache.json.decodeFromString<AirQualityData>(it.payload) }.getOrNull() }
            val meta = row?.let { runCatching { cache.json.decodeFromString<SourceMeta>(it.meta) }.getOrNull() }
            Resource(
                data?.copy(isFallback = link?.isFallback ?: false, distanceKm = link?.distanceKm),
                meta,
                row?.let { cache.policy.expired(it.expiresAt) } ?: false,
                !online,
                status[region.key]?.refreshing ?: false,
                status[region.key]?.error
            )
        }
    }
    override suspend fun refresh(region: Region) = locks.computeIfAbsent(region.key) { Mutex() }.withLock {
        states.update { it + (region.key to RequestStatus(refreshing = true)) }
        try {
            val stations = catalog.stations()
            val nearest = NearestFinder.nearest(
                region.publicLatitude,
                region.publicLongitude,
                stations.map { StationPoint(it.name, it.name, it.latitude, it.longitude) },
                2
            )
            val first = nearest.firstOrNull() ?: throw DataException(DomainError.REGION_MAPPING)
            var chosen = first
            val primary = load(first.station.name)
            if (AirRules.latestCoverage(primary) < 2 && nearest.size > 1) {
                val second = nearest[1]
                val fallback = load(second.station.name)
                if (AirRules.useFallback(primary, fallback)) chosen = second
            }
            val chosenData = read(chosen.station.name)
            if (chosenData == null) {
                val reason = coordinator.states.value[RequestKey("air_${chosen.station.name}", DataKind.AIR)]?.error
                throw DataException(reason ?: DomainError.NO_DATA)
            }
            cache.dao.saveAirLink(AirLinkEntity(region.key, chosen.station.name, chosen != first, chosen.distanceKm))
            val stationError = coordinator.states.value[RequestKey("air_${chosen.station.name}", DataKind.AIR)]?.error
            states.update { it + (region.key to RequestStatus(error = stationError)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val reason = (error as? DataException)?.reason ?: DomainError.INVALID_RESPONSE
            states.update { it + (region.key to RequestStatus(error = reason)) }
        } finally {
            states.update { it + (region.key to (it[region.key] ?: RequestStatus()).copy(refreshing = false)) }
        }
        Unit
    }
    private suspend fun read(station: String): AirQualityData? = cache.read("air_$station", DataKind.AIR)?.let {
        runCatching { cache.json.decodeFromString<AirQualityData>(it.payload) }.getOrNull()
    }
    private suspend fun load(station: String): AirQualityData? {
        coordinator.refresh("air_$station", DataKind.AIR) {
            val items = api.fetch(
                "B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty",
                mapOf("returnType" to "json", "numOfRows" to "24", "stationName" to station, "dataTerm" to "DAILY", "ver" to "1.5"),
                "air",
                24
            )
            val data = AirParser.readings(station, items)
            if (data.readings.isEmpty()) throw DataException(DomainError.NO_DATA)
            val latest = data.readings.last().at
            val meta = SourceMeta(Provider.AIRKOREA, DataKind.AIR, station, clock.instant().toEpochMilli(), observedAt = latest)
            LoadedSnapshot(cache.json.encodeToString(data), meta)
        }
        return read(station)
    }
}

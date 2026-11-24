package com.pbweather.weather.data.repository

import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.NetworkMonitor
import com.pbweather.weather.data.remote.kma.AsosParser
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.HistorySource
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

@Singleton
class HistoryRepository @Inject constructor(
    private val cache: CacheStore,
    private val api: ApiClient,
    private val coordinator: RequestCoordinator,
    private val network: NetworkMonitor,
    private val clock: Clock
) : HistorySource {
    private val stationLocks = ConcurrentHashMap<String, Mutex>()
    private fun key(region: Region, range: DateRange) = "history_${region.asosStationId}_${range.start}_${range.end}"
    override fun observe(region: Region, range: DateRange) = combine(
        cache.dao.observeHistory(region.asosStationId, range.start.toString(), range.end.toString()),
        coordinator.states,
        network.connected
    ) { rows, states, online ->
        val data = rows.mapNotNull { runCatching { cache.json.decodeFromString<DailyObservation>(it.payload) }.getOrNull() }
        val status = states[RequestKey(key(region, range), DataKind.HISTORY)]
        Resource(
            data.takeIf { it.isNotEmpty() },
            data.maxByOrNull { it.meta.receivedAt }?.meta,
            isOffline = !online,
            isRefreshing = status?.refreshing ?: false,
            error = status?.error ?: if (data.isNotEmpty() && data.size < range.dates.size) DomainError.NO_DATA else null
        )
    }
    override suspend fun refresh(region: Region, range: DateRange) {
        val validation = HistoryWindow.validate(range.start.toString(), range.end.toString(), clock)
        if (validation.error != null) return
        val key = key(region, range)
        val rows = cache.dao.history(region.asosStationId, range.start.toString(), range.end.toString())
        if (rows.size < range.dates.size) cache.dao.deleteSnapshot(key, DataKind.HISTORY.name)
        coordinator.refresh(key, DataKind.HISTORY) {
            stationLocks.computeIfAbsent(region.asosStationId) { Mutex() }.withLock {
                val stored = cache.dao.history(region.asosStationId, range.start.toString(), range.end.toString())
                val missing = HistoryWindow.missing(range, stored.map { it.date }.toSet())
                var lastMeta = stored.mapNotNull {
                    runCatching { cache.json.decodeFromString<DailyObservation>(it.payload).meta }.getOrNull()
                }.maxByOrNull { it.receivedAt }
                for (gap in missing) {
                    val items = api.fetch(
                        "1360000/AsosDalyInfoService/getWthrDataList",
                        mapOf(
                            "dataType" to "JSON",
                            "numOfRows" to "999",
                            "dataCd" to "ASOS",
                            "dateCd" to "DAY",
                            "startDt" to gap.start.format(DateTimeFormatter.BASIC_ISO_DATE),
                            "endDt" to gap.end.format(DateTimeFormatter.BASIC_ISO_DATE),
                            "stnIds" to region.asosStationId
                        ),
                        "asos"
                    )
                    val parsed = AsosParser.observations(items, region.asosStationId, gap, clock.instant().toEpochMilli())
                    for (row in parsed) {
                        cache.saveObservation(row)
                        lastMeta = row.meta
                    }
                }
                val updated = cache.dao.history(region.asosStationId, range.start.toString(), range.end.toString())
                if (updated.size != range.dates.size) throw DataException(DomainError.NO_DATA)
                cache.dao.touchHistory(region.asosStationId, range.start.toString(), range.end.toString(), clock.instant().toEpochMilli())
                val meta = lastMeta ?: SourceMeta(Provider.KMA, DataKind.HISTORY, region.asosStationId, clock.instant().toEpochMilli())
                LoadedSnapshot(cache.json.encodeToString(updated.map { it.date }), meta)
            }
        }
    }
}

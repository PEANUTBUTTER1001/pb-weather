package com.pbweather.weather.data.repository

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.local.WeatherSnapshotEntity
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.NetworkMonitor
import com.pbweather.weather.data.remote.kma.KmaParser
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.ForecastData
import com.pbweather.weather.domain.ForecastMerger
import com.pbweather.weather.domain.ForecastSource
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.ObservedWeather
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

@Singleton
class ForecastRepository @Inject constructor(
    private val cache: CacheStore,
    private val api: ApiClient,
    private val releases: ReleaseTimeCalculator,
    private val coordinator: RequestCoordinator,
    private val network: NetworkMonitor,
    private val clock: Clock
) : ForecastSource {
    private val ticks = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.instant().toEpochMilli())
            delay(60000)
        }
    }
    override fun observe(region: Region): Flow<ForecastState> = combine(
        cache.dao.observeSnapshots(region.key),
        coordinator.states,
        network.connected,
        ticks
    ) { snapshots, statuses, online, now ->
        val rows = snapshots.associateBy { it.kind }
        val observed = resource<ObservedWeather>(rows[DataKind.NOWCAST.name], statuses[RequestKey(region.key, DataKind.NOWCAST)], online)
        val short = resource<ForecastData>(rows[DataKind.SHORT.name], statuses[RequestKey(region.key, DataKind.SHORT)], online)
        val ultra = resource<ForecastData>(rows[DataKind.ULTRA.name], statuses[RequestKey(region.key, DataKind.ULTRA)], online)
        val extremes = resource<ForecastData>(rows[DataKind.EXTREMES.name], statuses[RequestKey(region.key, DataKind.EXTREMES)], online)
        val hours = ForecastMerger.hours(short.data?.hours.orEmpty(), ultra.data?.hours.orEmpty(), now)
        val days = ForecastMerger.days(short.data?.days.orEmpty(), extremes.data?.days.orEmpty())
        val combined = if (hours.isEmpty() && days.isEmpty()) null else ForecastData(hours, days)
        val hourly = Resource(
            combined,
            short.meta ?: ultra.meta,
            isStale = if (short.data != null) short.isStale else ultra.isStale,
            isOffline = !online,
            isRefreshing = short.isRefreshing || ultra.isRefreshing,
            error = short.error ?: if (short.data == null) ultra.error else null
        )
        ForecastState(
            observed,
            hourly,
            Resource(
                days.takeIf { it.isNotEmpty() },
                short.meta ?: extremes.meta,
                isStale = short.isStale,
                isOffline = !online,
                isRefreshing = short.isRefreshing || extremes.isRefreshing,
                error = short.error
            )
        )
    }
    private inline fun <reified T> resource(row: WeatherSnapshotEntity?, state: RequestStatus?, online: Boolean): Resource<T> {
        val data = row?.let { runCatching { cache.json.decodeFromString<T>(it.payload) }.getOrNull() }
        val meta = row?.let { runCatching { cache.json.decodeFromString<SourceMeta>(it.meta) }.getOrNull() }
        return Resource(
            data,
            meta,
            row?.let { cache.policy.expired(it.expiresAt) } ?: false,
            !online,
            state?.refreshing ?: false,
            state?.error ?: if (row != null && data == null) DomainError.INVALID_RESPONSE else null
        )
    }
    override suspend fun refresh(region: Region) {
        supervisorScope {
            listOf(DataKind.NOWCAST, DataKind.SHORT, DataKind.ULTRA).map { kind ->
                async { refreshKind(region, kind) }
            }.awaitAll()
        }
        refreshExtremes(region)
    }
    private suspend fun refreshKind(region: Region, kind: DataKind) = coordinator.refresh(region.key, kind) {
        val (items, release) = fetch(region, kind, releases.latest(kind))
        val meta = KmaParser.metadata(items, release, kind, region.key, clock.instant().toEpochMilli())
        val payload = if (kind == DataKind.NOWCAST) {
            cache.json.encodeToString(KmaParser.observed(items))
        } else {
            cache.json.encodeToString(KmaParser.forecast(items, meta, release))
        }
        LoadedSnapshot(payload, meta)
    }
    private suspend fun fetch(region: Region, kind: DataKind, release: Release): Pair<List<JsonObject>, Release> {
        val operation = when (kind) {
            DataKind.NOWCAST -> "getUltraSrtNcst"
            DataKind.ULTRA -> "getUltraSrtFcst"
            else -> "getVilageFcst"
        }
        suspend fun call(base: Release) = api.fetch(
            "1360000/VilageFcstInfoService_2.0/$operation",
            mapOf(
                "dataType" to "JSON",
                "numOfRows" to "1000",
                "base_date" to base.date,
                "base_time" to base.time,
                "nx" to region.nx.toString(),
                "ny" to region.ny.toString()
            ),
            "short"
        )
        return try {
            call(release) to release
        } catch (failure: DataException) {
            if (failure.reason != DomainError.NO_DATA) throw failure
            val previous = releases.previous(kind, release)
            call(previous) to previous
        }
    }
    private suspend fun refreshExtremes(region: Region) {
        val short = cache.read(region.key, DataKind.SHORT) ?: return
        val current = runCatching { cache.json.decodeFromString<ForecastData>(short.payload) }.getOrNull() ?: return
        val today = LocalDate.now(clock.withZone(ReleaseTimeCalculator.KST))
        val daily = current.days.firstOrNull { it.date == today.toString() }
        coordinator.refresh(region.key, DataKind.EXTREMES) {
            val meta = cache.json.decodeFromString<SourceMeta>(short.meta)
            val expiry = today.plusDays(1).atStartOfDay(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
            if (daily?.minimum != null && daily.maximum != null) {
                LoadedSnapshot(cache.json.encodeToString(ForecastData(days = listOf(daily))), meta.copy(kind = DataKind.EXTREMES), expiry)
            } else {
                val early = if (releases.latest(DataKind.SHORT).at.toLocalDate() < today) {
                    releases.latest(DataKind.SHORT)
                } else {
                    Release(today.atTime(2, 0))
                }
                val (items, release) = fetch(region, DataKind.SHORT, early)
                val source = KmaParser.metadata(items, release, DataKind.EXTREMES, region.key, clock.instant().toEpochMilli())
                val data = KmaParser.forecast(items, source, release)
                LoadedSnapshot(
                    cache.json.encodeToString(ForecastData(days = data.days.filter { it.date == today.toString() })),
                    source,
                    expiry
                )
            }
        }
    }
}

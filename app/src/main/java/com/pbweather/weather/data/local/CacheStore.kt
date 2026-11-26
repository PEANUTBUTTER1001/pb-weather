package com.pbweather.weather.data.local

import androidx.room.withTransaction
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private class CacheGeneration(val value: Long) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CacheGeneration>
}

@Singleton
class CacheStore @Inject constructor(val db: WeatherDatabase, val json: Json, val clock: Clock, val policy: CachePolicy) {
    private val generation = AtomicLong(0)
    fun requestContext(): CoroutineContext = CacheGeneration(generation.get())
    private suspend fun requestGeneration() = currentCoroutineContext()[CacheGeneration]?.value ?: generation.get()
    val dao get() = db.weatherDao()
    suspend fun read(key: String, kind: DataKind): WeatherSnapshotEntity? {
        val row = dao.snapshot(key, kind.name)
        if (row != null) dao.touchSnapshots(key, clock.instant().toEpochMilli())
        return row
    }
    suspend fun write(key: String, kind: DataKind, payload: String, meta: SourceMeta, expiresAt: Long? = null) {
        val expectedGeneration = requestGeneration()
        val metaJson = json.encodeToString(meta)
        val now = clock.instant().toEpochMilli()
        db.withTransaction {
            if (expectedGeneration != generation.get()) return@withTransaction
            val old = dao.snapshot(key, kind.name)
            val previous = old?.let { runCatching { json.decodeFromString<SourceMeta>(it.meta) }.getOrNull() }
            val previousVersion = previous?.observedAt ?: previous?.issuedAt ?: Long.MIN_VALUE
            val newVersion = meta.observedAt ?: meta.issuedAt ?: Long.MIN_VALUE
            if (newVersion < previousVersion) return@withTransaction
            val bytes = payload.toByteArray(Charsets.UTF_8).size + metaJson.toByteArray(Charsets.UTF_8).size
            dao.saveSnapshot(
                WeatherSnapshotEntity(
                    key,
                    kind.name,
                    payload,
                    metaJson,
                    now,
                    expiresAt ?: policy.expiry(kind, now),
                    now,
                    bytes.toLong()
                )
            )
            prune()
        }
    }
    suspend fun saveObservation(row: DailyObservation) {
        val expectedGeneration = requestGeneration()
        val payload = json.encodeToString(row)
        db.withTransaction {
            if (expectedGeneration != generation.get()) return@withTransaction
            dao.saveObservation(
                DailyObservationEntity(
                    row.stationId,
                    row.date,
                    payload,
                    clock.instant().toEpochMilli(),
                    payload.toByteArray(Charsets.UTF_8).size.toLong()
                )
            )
            prune()
        }
    }
    private suspend fun prune() {
        for (row in policy.evictions(dao.cacheIndex())) {
            if (row.store == "snapshot") {
                dao.deleteSnapshot(row.key1, row.key2)
            } else {
                dao.deleteObservation(row.key1, row.key2)
            }
        }
    }
    suspend fun sizeBytes(): Long = dao.cacheIndex().sumOf { it.byteCount }
    suspend fun clearWeather() = db.withTransaction {
        generation.incrementAndGet()
        dao.clearSnapshots()
        dao.clearObservations()
    }
}

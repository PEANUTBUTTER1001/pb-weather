package com.pbweather.weather.data.local

import com.pbweather.weather.domain.DataKind
import java.time.Clock
import java.time.Duration

class CachePolicy(private val clock: Clock) {
    fun ttl(kind: DataKind): Duration = when (kind) {
        DataKind.NOWCAST -> Duration.ofMinutes(30)
        DataKind.ULTRA, DataKind.SHORT -> Duration.ofHours(3)
        DataKind.MID_LAND, DataKind.MID_TA -> Duration.ofHours(12)
        DataKind.AIR -> Duration.ofHours(1)
        DataKind.EXTREMES -> Duration.ofDays(1)
        DataKind.STATIONS -> Duration.ofDays(7)
        DataKind.HISTORY -> Duration.ofDays(36500)
    }
    fun expired(expiresAt: Long): Boolean = clock.instant().toEpochMilli() >= expiresAt
    fun expiry(kind: DataKind, fetchedAt: Long): Long = fetchedAt + ttl(kind).toMillis()

    fun evictions(entries: List<CacheIndexRow>, maxBytes: Long = MAX_BYTES): List<CacheIndexRow> {
        var size = entries.sumOf { it.byteCount }
        return entries.sortedWith(compareBy<CacheIndexRow> { it.lastAccessedAt }.thenBy { it.key1 }.thenBy { it.key2 })
            .takeWhile { entry ->
                if (size <= maxBytes) {
                    false
                } else {
                    size -= entry.byteCount
                    true
                }
            }
    }
    companion object {
        const val MAX_BYTES: Long = 50L * 1024 * 1024
    }
}

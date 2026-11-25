package com.pbweather.weather.core.time

import com.pbweather.weather.domain.DataKind
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Release(val at: LocalDateTime) {
    val date: String get() = at.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
    val time: String get() = at.toLocalTime().format(DateTimeFormatter.ofPattern("HHmm"))
    val epochMillis: Long get() = at.atZone(ReleaseTimeCalculator.KST).toInstant().toEpochMilli()
    val version: String get() = date + time
}

class ReleaseTimeCalculator(private val clock: Clock) {
    /** Latest guide cadence. A NO_DATA compatibility fallback is separately exposed, never silently relabelled. */
    fun latest(kind: DataKind): Release {
        val now = LocalDateTime.now(clock.withZone(KST))
        return when (kind) {
            DataKind.NOWCAST -> {
                val available = now.minusMinutes(10).truncatedTo(ChronoUnit.MINUTES)
                Release(available.withMinute(available.minute / 10 * 10))
            }
            DataKind.ULTRA -> {
                val available = now.minusMinutes(15)
                val base = available.withMinute(30).withSecond(0).withNano(0)
                Release(if (available.minute >= 30) base else base.minusHours(1))
            }
            DataKind.MID_LAND, DataKind.MID_TA -> schedule(now, listOf(6, 18))
            else -> schedule(now.minusMinutes(10), listOf(2, 5, 8, 11, 14, 17, 20, 23))
        }
    }

    /** Legacy hourly envelope fallback, preserving the actual response observation time. */
    fun legacyNowcast(): Release = Release(LocalDateTime.now(clock.withZone(KST)).minusMinutes(40).truncatedTo(ChronoUnit.HOURS))
    fun previous(kind: DataKind, release: Release): Release = when (kind) {
        DataKind.NOWCAST -> legacyNowcast().let { if (it.at < release.at) it else Release(release.at.minusMinutes(10)) }
        DataKind.ULTRA -> Release(release.at.minusHours(1))
        DataKind.MID_LAND, DataKind.MID_TA -> Release(release.at.minusHours(12))
        else -> Release(release.at.minusHours(3))
    }
    private fun schedule(at: LocalDateTime, hours: List<Int>): Release {
        val hour = hours.lastOrNull { it <= at.hour }
        return if (hour == null) {
            Release(at.toLocalDate().minusDays(1).atTime(hours.last(), 0))
        } else {
            Release(at.toLocalDate().atTime(hour, 0))
        }
    }
    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

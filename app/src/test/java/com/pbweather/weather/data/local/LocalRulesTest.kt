package com.pbweather.weather.data.local

import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.Region
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRulesTest {
    private val at = Instant.parse("2026-11-24T05:00:00Z")
    private val policy = CachePolicy(Clock.fixed(at, ZoneOffset.UTC))
    private fun region(id: String) = Region(id, id, 60, 127, 37.5635694, 126.9800083, "108", "서울", 1.0)

    @Test
    fun `ttl expiration includes exact boundary without extending offline data`() {
        for ((kind, minutes) in listOf(
            DataKind.NOWCAST to 30L,
            DataKind.SHORT to 180L,
            DataKind.MID_TA to 720L,
            DataKind.AIR to 60L
        )) {
            val expiry = policy.expiry(kind, at.minusSeconds(minutes * 60).toEpochMilli())
            assertTrue(policy.expired(expiry))
            assertFalse(policy.expired(expiry + 1))
        }
    }

    @Test
    fun `lru evicts only enough payload and respects recent access`() {
        val rows = listOf(
            CacheIndexRow("snapshot", "new", "SHORT", 20, 30),
            CacheIndexRow("history", "old", "date", 20, 10),
            CacheIndexRow("snapshot", "middle", "AIR", 20, 20)
        )
        assertEquals(listOf("old"), policy.evictions(rows, 40).map { it.key1 })
        assertEquals(emptyList<CacheIndexRow>(), policy.evictions(rows, 60))
    }

    @Test
    fun `favorites cap duplicate and consecutive undo preserve order and primary`() {
        var prefs = AppPreferences()
        repeat(11) { prefs = FavoriteRules.add(prefs, region(it.toString())) }
        assertEquals(10, prefs.favorites.size)
        assertEquals("0", prefs.primaryId)
        assertEquals(prefs, FavoriteRules.add(prefs, region("0")))
        val (first, a) = FavoriteRules.remove(prefs, "0")
        val (second, b) = FavoriteRules.remove(first, "1")
        assertEquals("2", second.primaryId)
        prefs = FavoriteRules.restore(FavoriteRules.restore(second, requireNotNull(b)), requireNotNull(a))
        assertEquals((0..9).map { it.toString() }, prefs.favorites.map { it.id })
        assertEquals("0", prefs.primaryId)
    }

    @Test
    fun `last deletion leaves no representative and serialization restores preferences`() {
        val initial = FavoriteRules.add(AppPreferences(theme = "DARK", palette = "TEAL"), region("A"))
        val (removed, undo) = FavoriteRules.remove(initial, "A")
        assertEquals(null, removed.primaryId)
        assertEquals(emptyList<Region>(), removed.favorites)
        val restored = FavoriteRules.restore(removed, requireNotNull(undo))
        assertEquals(initial, Json.decodeFromString<AppPreferences>(Json.encodeToString(restored)))
    }
}

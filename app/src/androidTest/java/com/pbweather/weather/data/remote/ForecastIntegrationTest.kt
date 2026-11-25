package com.pbweather.weather.data.remote

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.data.local.CachePolicy
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.repository.LoadedSnapshot
import com.pbweather.weather.data.repository.RequestCoordinator
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForecastIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun coalescedRequestsAndFailuresPreserveSuccessfulCache() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val at = Instant.parse("2026-11-24T05:00:00Z").toEpochMilli()
            val clock = Clock.fixed(Instant.ofEpochMilli(at), ZoneOffset.UTC)
            val cache = CacheStore(db, Json, clock, CachePolicy(clock))
            val coordinator = RequestCoordinator(cache)
            val count = AtomicInteger()
            val meta = SourceMeta(Provider.KMA, DataKind.SHORT, "60_127", at)
            coroutineScope {
                (1..5).map {
                    async {
                        coordinator.refresh("60_127", DataKind.SHORT) {
                            count.incrementAndGet()
                            delay(20)
                            LoadedSnapshot("{}", meta)
                        }
                    }
                }.awaitAll()
            }
            assertEquals(1, count.get())
            assertNotNull(cache.read("60_127", DataKind.SHORT))
            cache.write("air_test", DataKind.AIR, "original", meta, at - 1)
            assertEquals(false, coordinator.refresh("air_test", DataKind.AIR) { throw DataException(DomainError.TIMEOUT) })
            val row = requireNotNull(cache.read("air_test", DataKind.AIR))
            assertEquals("original", row.payload)
            assertEquals(at - 1, row.expiresAt)
            assertEquals(at, Json.decodeFromString<SourceMeta>(row.meta).receivedAt)
            assertNotNull(cache.read("60_127", DataKind.SHORT))
            count.set(0)
            coroutineScope {
                (1..5).map {
                    async {
                        coordinator.refresh("failed", DataKind.ULTRA) {
                            count.incrementAndGet()
                            delay(30)
                            throw DataException(DomainError.TIMEOUT)
                        }
                    }
                }.awaitAll()
            }
            assertEquals(1, count.get())
        } finally {
            db.close()
        }
    }
}

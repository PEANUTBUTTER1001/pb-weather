package com.pbweather.weather.data.remote

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.data.local.CachePolicy
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.repository.HistoryRepository
import com.pbweather.weather.data.repository.RequestCoordinator
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.Region
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class HistoryCacheTest {
    @Test fun partialPublicationRetriesOnlyMissingDateAndRecoversAfterLru() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val clock = Clock.fixed(Instant.parse("2026-11-25T05:00:00Z"), ZoneOffset.UTC)
            val cache = CacheStore(db, Json, clock, CachePolicy(clock))
            val calls = mutableListOf<Map<String, String>>()
            val available = mutableSetOf("2026-11-22", "2026-11-23")
            val fake = object : PublicDataApi {
                override suspend fun get(operation: String, parameters: Map<String, String>): Response<ResponseBody> {
                    calls += parameters
                    val records = available.sorted().filter {
                        it.replace("-", "") in
                            parameters.getValue("startDt")..parameters.getValue("endDt")
                    }
                    val body = buildJsonObject {
                        put(
                            "response",
                            buildJsonObject {
                                put("header", buildJsonObject { put("resultCode", "00") })
                                put(
                                    "body",
                                    buildJsonObject {
                                        put("totalCount", records.size)
                                        put(
                                            "items",
                                            buildJsonObject {
                                                put(
                                                    "item",
                                                    buildJsonArray {
                                                        records.forEach { date ->
                                                            add(
                                                                buildJsonObject {
                                                                    put("stnId", "108")
                                                                    put("tm", date)
                                                                    put("maxTa", "12")
                                                                    put("sumRn", "")
                                                                }
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                    return Response.success(body.toString().toResponseBody())
                }
            }
            val quota = object : QuotaRegistry {
                override suspend fun limited(family: String) = false
                override suspend fun mark(family: String) = Unit
            }
            val repo = HistoryRepository(cache, ApiClient(fake, Json, quota), RequestCoordinator(cache), NetworkMonitor(context), clock)
            val region = Region("test", "서울", 60, 127, 37.57, 126.98, "108", "서울", 0.0)
            val range = DateRange(LocalDate.parse("2026-11-22"), LocalDate.parse("2026-11-24"))
            repo.refresh(region, range)
            val partial = repo.observe(region, range).first()
            assertEquals(2, partial.data?.size)
            assertEquals(DomainError.NO_DATA, partial.error)
            available += "2026-11-24"
            repo.refresh(region, range)
            assertEquals("20261124", calls.last()["startDt"])
            assertEquals("20261124", calls.last()["endDt"])
            assertEquals(2, calls.size)
            repo.refresh(region, range)
            assertEquals(2, calls.size)
            cache.dao.deleteObservation("108", "2026-11-23")
            repo.refresh(region, range)
            assertEquals("20261123", calls.last()["startDt"])
            assertEquals("20261123", calls.last()["endDt"])
            assertEquals(3, calls.size)
        } finally {
            db.close()
        }
    }
}

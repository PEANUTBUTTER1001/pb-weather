package com.pbweather.weather.data.remote

import com.pbweather.weather.core.time.Release
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.remote.kma.KmaParser
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.ForecastHour
import com.pbweather.weather.domain.ForecastMerger
import com.pbweather.weather.domain.PrecipitationKind
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class ForecastContractTest {
    private fun releases(time: String): ReleaseTimeCalculator {
        val local = LocalDateTime.parse(time)
        return ReleaseTimeCalculator(Clock.fixed(local.atZone(ReleaseTimeCalculator.KST).toInstant(), ReleaseTimeCalculator.KST))
    }

    @Test fun `release boundaries and midnight preserve the actual date`() {
        assertEquals("202611232350", releases("2026-11-24T00:05:00").latest(DataKind.NOWCAST).version)
        assertEquals("202611240000", releases("2026-11-24T00:10:00").latest(DataKind.NOWCAST).version)
        assertEquals("202611240100", releases("2026-11-24T02:09:00").legacyNowcast().version)
        assertEquals("202611232300", releases("2026-11-24T02:09:00").latest(DataKind.SHORT).version)
        assertEquals("202611240200", releases("2026-11-24T02:10:00").latest(DataKind.SHORT).version)
        assertEquals("202611231800", releases("2026-11-24T05:59:00").latest(DataKind.MID_LAND).version)
        assertEquals("202611240600", releases("2026-11-24T06:01:00").latest(DataKind.MID_LAND).version)
        assertEquals("202611241330", releases("2026-11-24T14:44:00").latest(DataKind.ULTRA).version)
        assertEquals("202611241430", releases("2026-11-24T14:45:00").latest(DataKind.ULTRA).version)
    }

    @Test fun `preferred missing fields cannot erase valid probability or precipitation`() {
        val short = ForecastHour(1000, 10.0, 1, 0, 60, KmaParser.precipitation("1~4mm"))
        val ultra = ForecastHour(1000, 11.0, 4)
        val merged = ForecastMerger.hours(listOf(short), listOf(ultra), 0).single()
        assertEquals(11.0, merged.temperature)
        assertEquals(60, merged.probability)
        assertEquals("1~4mm", merged.precipitation?.text)
        assertNull(merged.precipitation?.exactMm)
    }

    @Test fun `precipitation ranges qualitative codes and missing values stay distinct`() {
        assertEquals(0.0, KmaParser.precipitation("강수없음")?.exactMm)
        assertEquals(PrecipitationKind.RANGE, KmaParser.precipitation("1mm 미만")?.kind)
        assertEquals(PrecipitationKind.QUALITATIVE, KmaParser.precipitation("1", true)?.kind)
        assertNull(KmaParser.precipitation("1", true)?.exactMm)
        assertNull(KmaParser.precipitation("-"))
        assertNull(KmaParser.precipitation("-999"))
        assertEquals(KmaParser.at("20261125", "0000"), KmaParser.at("20261124", "2400"))
    }
    private class MemoryQuota : QuotaRegistry {
        private val blocked = mutableSetOf<String>()
        override suspend fun limited(family: String): Boolean = family in blocked
        override suspend fun mark(family: String) {
            blocked += family
        }
    }
    private fun client(server: MockWebServer, quota: QuotaRegistry): ApiClient {
        val http = OkHttpClient.Builder().addInterceptor(
            ServiceKeyInterceptor("test%2Bkey%2F%3D", "air-test", server.url("/").host, false)
        ).build()
        val api = Retrofit.Builder().baseUrl(server.url("/")).client(http).build().create(PublicDataApi::class.java)
        return ApiClient(api, Json, quota)
    }

    @Test fun `pagination uses a single encoded decoding key and accepts result zero`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"response":{"header":{"resultCode":"0"},
"body":{"totalCount":2,"items":{"item":[{"v":1}]}}}}"""
                )
            )
            server.enqueue(
                MockResponse().setBody(
                    """{"response":{"header":{"resultCode":"00"},
"body":{"totalCount":2,"items":{"item":[{"v":2}]}}}}"""
                )
            )
            server.start()
            val items = client(server, MemoryQuota()).fetch("1360000/test", mapOf("numOfRows" to "1"), "short")
            assertEquals(2, items.size)
            val first = requireNotNull(server.takeRequest().requestUrl)
            assertEquals(listOf("test+key/="), first.queryParameterValues("serviceKey"))
            assertEquals("2", server.takeRequest().requestUrl?.queryParameter("pageNo"))
        }
    }

    @Test fun `quota prevents later requests and XML key failures are classified`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"response":{"header":{"resultCode":"22"}}}"""))
            server.start()
            val api = client(server, MemoryQuota())
            repeat(2) {
                val error = runCatching { api.fetch("1360000/test", emptyMap(), "short") }.exceptionOrNull()
                assertEquals(DomainError.QUOTA, (error as DataException).reason)
            }
            assertEquals(1, server.requestCount)
            val error = runCatching { api.parse("<returnReasonCode>30</returnReasonCode>") }.exceptionOrNull()
            assertEquals(DomainError.KEY_CONFIGURATION, (error as DataException).reason)
        }
    }

    @Test fun `invalid numeric API fields remain null`() {
        val field = Json.parseToJsonElement("""{"value":"-","nan":"NaN","missing":null}""") as JsonObject
        assertNull(field.number("value"))
        assertNull(field.number("nan"))
        assertNull(field.number("missing"))
        assertTrue(
            KmaParser.forecast(
                emptyList(),
                com.pbweather.weather.domain.SourceMeta(com.pbweather.weather.domain.Provider.KMA, DataKind.SHORT, "60_127", 0),
                Release(LocalDateTime.parse("2026-11-24T14:00:00"))
            ).hours.isEmpty()
        )
    }
}

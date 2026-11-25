package com.pbweather.weather.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.BuildConfig
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.DataException
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.ForecastData
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.SourceMeta
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LiveApiContractTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun realNowcastContractSmoke() = runBlocking {
        assumeTrue(BuildConfig.KMA_SERVICE_KEY.isNotBlank() && !BuildConfig.KMA_SERVICE_KEY.startsWith("<"))
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val latest = entry.releases().latest(DataKind.NOWCAST)
        suspend fun call(date: String, time: String) = entry.api().fetch(
            "1360000/VilageFcstInfoService_2.0/getUltraSrtNcst",
            mapOf(
                "dataType" to "JSON",
                "numOfRows" to "1000",
                "base_date" to date,
                "base_time" to time,
                "nx" to "60",
                "ny" to "127"
            ),
            "short"
        )
        var fallbackUsed = false
        val items = try {
            call(latest.date, latest.time)
        } catch (error: DataException) {
            if (error.reason != DomainError.NO_DATA) {
                context.cacheDir.resolve("forecast-smoke.json").writeText(
                    buildJsonObject {
                        put("result", error.reason.name)
                        put("httpStatus", error.httpStatus)
                        put("gatewayCode", error.gatewayCode)
                    }.toString()
                )
                throw error
            }
            fallbackUsed = true
            val previous = entry.releases().previous(DataKind.NOWCAST, latest)
            call(previous.date, previous.time)
        }
        assertTrue(items.any { it.text("category") == "T1H" && it.number("obsrValue") != null })
        val summary = buildJsonObject {
            put("operation", "getUltraSrtNcst")
            put("items", items.size)
            put("baseDate", items.first().text("baseDate"))
            put("baseTime", items.first().text("baseTime"))
            put("compatibilityFallback", fallbackUsed)
            put("bodyBytes", entry.traffic().byteCount())
        }
        context.cacheDir.resolve("forecast-smoke.json").writeText(summary.toString())
    }

    @Test
    fun realShortUltraAndCacheContract() = runBlocking {
        assumeTrue(BuildConfig.KMA_SERVICE_KEY.isNotBlank() && !BuildConfig.KMA_SERVICE_KEY.startsWith("<"))
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val region = Region("test_seoul", "서울", 60, 127, 37.57142, 126.9658, "108", "서울", 0.0)
        entry.forecasts().refresh(region)
        val short = requireNotNull(entry.cache().read(region.key, DataKind.SHORT))
        val ultra = requireNotNull(entry.cache().read(region.key, DataKind.ULTRA))
        val data = entry.cache().json.decodeFromString<ForecastData>(short.payload)
        val now = entry.clock().instant().toEpochMilli()
        assertTrue(data.hours.count { it.at > now } >= 48)
        assertTrue(entry.cache().json.decodeFromString<ForecastData>(ultra.payload).hours.isNotEmpty())
        assertNotNull(entry.cache().read(region.key, DataKind.NOWCAST))
        val bytes = entry.traffic().byteCount()
        entry.forecasts().refresh(region)
        assertEquals(bytes, entry.traffic().byteCount())
        val state = withTimeout(5000) { entry.forecasts().observe(region).first { it.now.data != null } }
        assertNotNull(state.hourly.data)
        assertTrue(!state.now.isStale)
        val meta = entry.cache().json.decodeFromString<SourceMeta>(short.meta)
        assertNotNull(meta.issuedAt)
    }
}

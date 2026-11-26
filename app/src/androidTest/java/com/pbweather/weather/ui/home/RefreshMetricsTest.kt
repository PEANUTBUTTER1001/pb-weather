package com.pbweather.weather.ui.home

import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.data.local.AssetImporter
import com.pbweather.weather.data.location.RegionResolver
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.HistoryWindow
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertNotNull
import org.junit.Test

class RefreshMetricsTest {
    @Test fun recordTenRealRefreshSamplesWithoutLoggingPayloadsKeysOrCoordinates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val cache = entry.cache()
        AssetImporter(context, cache.db).ensureLoaded()
        val dao = cache.dao
        val resolver = RegionResolver(dao.regions(), dao.asosStations(), dao.midRegions())
        val region = resolver.named(dao.search("서울특별시 종로구").first())
        val samples = mutableListOf<JsonObject>()
        val duration = mutableListOf<Long>()
        val bytes = mutableListOf<Long>()
        val uidBytes = mutableListOf<Long>()
        repeat(10) {
            cache.clearWeather()
            val before = entry.traffic().byteCount()
            val beforeRx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
            val beforeTx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
            val start = System.nanoTime()
            coroutineScope {
                listOf(
                    async { entry.forecasts().refresh(region) },
                    async { entry.air().refresh(region) },
                    async { entry.history().refresh(region, HistoryWindow.recent(entry.clock())) }
                ).awaitAll()
            }
            val elapsed = (System.nanoTime() - start) / 1000000L
            val transferred = entry.traffic().byteCount() - before
            val afterRx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
            val afterTx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid())
            val uidTransferred = if (beforeRx >= 0 && beforeTx >= 0 && afterRx >= beforeRx && afterTx >= beforeTx) {
                afterRx - beforeRx + afterTx - beforeTx
            } else {
                -1L
            }
            assertNotNull(entry.forecasts().observe(region).first().now.data)
            assertNotNull(entry.air().observe(region).first().data)
            duration += elapsed
            bytes += transferred
            if (uidTransferred >= 0) uidBytes += uidTransferred
            samples += JsonObject(
                mapOf(
                    "elapsedMs" to JsonPrimitive(elapsed),
                    "bodyBytes" to JsonPrimitive(transferred),
                    "uidNetworkBytes" to JsonPrimitive(uidTransferred)
                )
            )
        }
        val p90 = duration.sorted()[8]
        val maxBytes = bytes.max()
        val record = JsonObject(
            mapOf(
                "conditions" to
                    JsonPrimitive(
                        "Pixel_9_Pro API35 debug; virtual Wi-Fi; explicit weather-cache clear per sample; HTTP body bytes exclude headers/TLS"
                    ),
                "samples" to JsonArray(samples),
                "p90Ms" to JsonPrimitive(p90),
                "maximumBodyBytes" to JsonPrimitive(maxBytes),
                "targetP90Ms" to JsonPrimitive(3000),
                "targetBodyBytes" to JsonPrimitive(500000),
                "networkTargetMet" to JsonPrimitive(p90 <= 3000),
                "bodyTargetMet" to JsonPrimitive(maxBytes <= 500000),
                "maximumUidNetworkBytes" to JsonPrimitive(uidBytes.maxOrNull() ?: -1L),
                "uidNetworkTargetMet" to JsonPrimitive(uidBytes.size == 10 && uidBytes.max() <= 500000)
            )
        )
        context.getExternalFilesDir(null)!!.resolve("refresh-metrics.json").writeText(record.toString())
    }
}

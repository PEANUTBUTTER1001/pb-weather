package com.pbweather.weather.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.BuildConfig
import com.pbweather.weather.di.AirEntryPoint
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.AirRules
import com.pbweather.weather.domain.Region
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LiveAirContractTest {
    @Test fun realPublicStationCatalogAndHourlyAirContract() = runBlocking {
        assumeTrue(BuildConfig.AIRKOREA_SERVICE_KEY.isNotBlank() && !BuildConfig.AIRKOREA_SERVICE_KEY.startsWith("<"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val air = EntryPointAccessors.fromApplication(context, AirEntryPoint::class.java).airQuality()
        val region = Region("air_seoul", "서울", 60, 127, 37.57142, 126.9658, "108", "서울", 0.0)
        air.refresh(region)
        val state = air.observe(region).first()
        val data = requireNotNull(state.data) { "Air request failed: ${state.error}" }
        assertTrue(data.readings.isNotEmpty() && data.readings.size <= 24)
        assertTrue(data.readings.zipWithNext().all { (a, b) -> a.at < b.at })
        assertEquals(data.readings.last().at, state.meta?.observedAt)
        assertTrue(AirRules.latestCoverage(data) >= 1)
        val stations = entry.cache().dao.airStations()
        assertTrue(stations.size > 100)
        assertTrue(stations.all { it.latitude in 30.0..40.0 && it.longitude in 120.0..140.0 })
        val bytes = entry.traffic().byteCount()
        air.refresh(region)
        assertEquals(bytes, entry.traffic().byteCount())
    }
}

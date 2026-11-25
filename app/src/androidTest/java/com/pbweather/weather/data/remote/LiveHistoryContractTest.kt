package com.pbweather.weather.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.BuildConfig
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.di.HistoryEntryPoint
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.HistoryWindow
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
class LiveHistoryContractTest {
    @Test fun realAsosDailyHistoryAndImmutableCacheContract() = runBlocking {
        assumeTrue(BuildConfig.KMA_SERVICE_KEY.isNotBlank() && !BuildConfig.KMA_SERVICE_KEY.startsWith("<"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val history = EntryPointAccessors.fromApplication(context, HistoryEntryPoint::class.java).history()
        val region = Region("asos_seoul", "서울", 60, 127, 37.57142, 126.9658, "108", "서울", 0.0)
        val window = HistoryWindow.recent(entry.clock())
        // Yesterday can still be unpublished after midnight. Validate the nine already completed published days.
        val range = DateRange(window.start, window.end.minusDays(1))
        history.refresh(region, range)
        val state = history.observe(region, range).first()
        val rows = requireNotNull(state.data) { "ASOS request failed: ${state.error}" }
        assertEquals(9, rows.size)
        assertTrue(rows.all { it.stationId == "108" && it.date in range.start.toString()..range.end.toString() })
        assertTrue(rows.any { it.maximum != null && it.minimum != null })
        assertTrue(rows.all { it.meta.observedAt == null && it.meta.validFrom != null && it.meta.validTo != null })
        val bytes = entry.traffic().byteCount()
        history.refresh(region, range)
        assertEquals(bytes, entry.traffic().byteCount())
    }
}

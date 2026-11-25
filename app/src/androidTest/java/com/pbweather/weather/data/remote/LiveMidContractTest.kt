package com.pbweather.weather.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.BuildConfig
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.SourceMeta
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LiveMidContractTest {
    @Test fun realLandTemperatureAndElevenDateContract() = runBlocking {
        assumeTrue(BuildConfig.KMA_SERVICE_KEY.isNotBlank() && !BuildConfig.KMA_SERVICE_KEY.startsWith("<"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java)
        val region = Region("mid_seoul", "서울", 60, 127, 37.57142, 126.9658, "108", "서울", 0.0, "11B00000", "11B10101")
        entry.forecasts().refresh(region)
        val land = requireNotNull(entry.cache().read("mid_land_11B00000", DataKind.MID_LAND))
        val temperature = requireNotNull(entry.cache().read("mid_ta_11B10101", DataKind.MID_TA))
        val days = entry.cache().json.decodeFromString<List<DailyForecast>>(land.payload)
        assertTrue(days.any { it.morningWeather != null || it.wholeDayWeather != null })
        assertTrue(entry.cache().json.decodeFromString<List<DailyForecast>>(temperature.payload).any { it.minimum != null })
        val state = entry.forecasts().observe(region).first { it.weekly.data != null }
        val weekly = requireNotNull(state.weekly.data)
        assertEquals(11, weekly.size)
        val meta = entry.cache().json.decodeFromString<SourceMeta>(temperature.meta)
        val providedThrough = Instant.ofEpochMilli(requireNotNull(meta.issuedAt))
            .atZone(ReleaseTimeCalculator.KST).toLocalDate().plusDays(10)
        assertTrue(
            weekly.filter { LocalDate.parse(it.date) <= providedThrough }.all {
                it.minimum != null && it.maximum != null
            }
        )
        assertTrue(
            weekly.filter { LocalDate.parse(it.date) > providedThrough }.all {
                it.minimum == null && it.maximum == null && it.origins.isEmpty()
            }
        )
        val last = weekly.last { LocalDate.parse(it.date) <= providedThrough }
        assertNotNull(last.wholeDayWeather)
        assertNullPeriodsForWholeDay(last)
    }
    private fun assertNullPeriodsForWholeDay(day: DailyForecast) {
        assertEquals(null, day.morningWeather)
        assertEquals(null, day.afternoonWeather)
    }
}

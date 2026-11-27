package com.pbweather.weather.ui.navigation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import androidx.room.InvalidationTracker
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.data.local.CachePolicy
import com.pbweather.weather.data.local.CacheStore
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.local.WeatherDao
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.repository.SharedMinuteTicker
import com.pbweather.weather.data.repository.WidgetRepository
import com.pbweather.weather.domain.AirQualityData
import com.pbweather.weather.domain.AirQualitySource
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.FindRegions
import com.pbweather.weather.domain.ForecastSource
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.HistorySource
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.LocationSource
import com.pbweather.weather.domain.ObserveAirQuality
import com.pbweather.weather.domain.ObserveForecast
import com.pbweather.weather.domain.ObserveHistory
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import java.lang.reflect.Proxy
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private class MutableClock(private var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ReleaseTimeCalculator.KST
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        fun advanceMillis(millis: Long) {
            now = now.plusMillis(millis)
        }
    }

    private class MemoryDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
            transform(state.value).also { state.value = it }
        }
    }

    private class CountingForecastSource : ForecastSource {
        var observeCount = 0
        var refreshCount = 0
        override fun observe(region: Region): Flow<ForecastState> {
            observeCount++
            return flowOf(ForecastState())
        }
        override suspend fun refresh(region: Region) {
            refreshCount++
        }
    }

    private class CountingAirSource : AirQualitySource {
        var observeCount = 0
        var refreshCount = 0
        override fun observe(region: Region): Flow<Resource<AirQualityData>> {
            observeCount++
            return flowOf(Resource())
        }
        override suspend fun refresh(region: Region) {
            refreshCount++
        }
    }

    private class CountingHistorySource : HistorySource {
        var observeCount = 0
        var refreshCount = 0
        override fun observe(region: Region, range: DateRange): Flow<Resource<List<DailyObservation>>> {
            observeCount++
            return flowOf(Resource())
        }
        override suspend fun refresh(region: Region, range: DateRange) {
            refreshCount++
        }
    }

    private class StubLocationSource : LocationSource {
        override suspend fun search(query: String): List<Region> = emptyList()
        override suspend fun current(): Region = seoul
        override fun permissionGranted(): Boolean = false
        override fun serviceEnabled(): Boolean = false
    }

    private fun fakeCacheStore(clock: Clock): CacheStore {
        val dao = Proxy.newProxyInstance(
            WeatherDao::class.java.classLoader,
            arrayOf(WeatherDao::class.java)
        ) { _, method, _ ->
            if (method.name == "observeCacheBytes") flowOf(0L) else null
        } as WeatherDao
        val db = object : WeatherDatabase() {
            override fun weatherDao(): WeatherDao = dao
            override fun createInvalidationTracker(): InvalidationTracker = InvalidationTracker(this, emptyMap(), emptyMap())
            override fun clearAllTables() = Unit
        }
        return CacheStore(db, Json, clock, CachePolicy(clock))
    }

    @Test
    fun `setRange updates range without extra refresh or resubscription`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val clock = MutableClock(Instant.parse("2026-11-28T01:00:00Z"))
        val prefs = PreferencesRepository(MemoryDataStore(), Json)
        prefs.update { AppPreferences(selected = seoul) }
        val forecast = CountingForecastSource()
        val air = CountingAirSource()
        val history = CountingHistorySource()
        val locations = StubLocationSource()
        val vm = WeatherViewModel(
            preferences = prefs,
            forecast = ObserveForecast(forecast),
            air = ObserveAirQuality(air),
            history = ObserveHistory(history),
            findRegions = FindRegions(locations),
            locations = locations,
            clock = clock,
            widgets = WidgetRepository(prefs, forecast, clock),
            cache = fakeCacheStore(clock)
        )
        try {
            val collector = backgroundScope.launch { vm.ui.collect {} }
            runCurrent()

            assertEquals(1, forecast.refreshCount)
            assertEquals(1, air.refreshCount)
            assertEquals(1, history.refreshCount)
            assertEquals(1, forecast.observeCount)
            assertEquals(1, air.observeCount)
            assertEquals(1, history.observeCount)

            val recent = HistoryWindow.recent(clock)
            val sevenDays = DateRange(recent.end.minusDays(6), recent.end)
            vm.setRange(sevenDays)
            runCurrent()

            assertEquals(sevenDays, vm.ui.value.range)
            assertEquals(1, forecast.refreshCount)
            assertEquals(1, air.refreshCount)
            assertEquals(1, history.refreshCount)
            assertEquals(1, forecast.observeCount)
            assertEquals(1, air.observeCount)
            assertEquals(1, history.observeCount)

            vm.select(busan)
            runCurrent()

            assertEquals(2, forecast.refreshCount)
            assertEquals(2, air.refreshCount)
            assertEquals(2, history.refreshCount)
            assertEquals(2, forecast.observeCount)
            assertEquals(2, air.observeCount)
            assertEquals(2, history.observeCount)

            clock.advanceMillis(24 * 60 * 60 * 1000L)
            advanceTimeBy(60_000L)
            runCurrent()

            assertEquals(3, forecast.refreshCount)
            assertEquals(3, air.refreshCount)
            assertEquals(3, history.refreshCount)
            assertEquals(3, history.observeCount)

            collector.cancel()
        } finally {
            vm.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shared minute ticker drives followers and hands off leadership on cancel`() = runTest {
        val clock = MutableClock(Instant.parse("2026-11-28T01:00:00Z"))
        val ticker = SharedMinuteTicker(clock, intervalMs = 60_000L)
        val firstEmissions = mutableListOf<Long>()
        val secondEmissions = mutableListOf<Long>()

        val first = launch { ticker.flow.collect { firstEmissions += it } }
        val second = launch { ticker.flow.collect { secondEmissions += it } }
        runCurrent()

        assertTrue(ticker.isLeaderActive)
        assertEquals(listOf(clock.instant().toEpochMilli()), firstEmissions)
        assertEquals(listOf(clock.instant().toEpochMilli()), secondEmissions)

        clock.advanceMillis(60_000L)
        advanceTimeBy(60_000L)
        runCurrent()

        val secondTick = clock.instant().toEpochMilli()
        assertEquals(listOf(secondTick - 60_000L, secondTick), firstEmissions)
        assertEquals(listOf(secondTick - 60_000L, secondTick), secondEmissions)

        first.cancel()
        runCurrent()
        assertTrue(ticker.isLeaderActive)

        clock.advanceMillis(60_000L)
        advanceTimeBy(60_000L)
        runCurrent()

        assertEquals(clock.instant().toEpochMilli(), secondEmissions.last())

        second.cancel()
        runCurrent()
        assertFalse(ticker.isLeaderActive)
    }

    private companion object {
        val seoul = Region("11", "서울", 60, 127, 37.56, 126.98, "108", "서울", 1.0)
        val busan = Region("26", "부산", 98, 76, 35.18, 129.07, "159", "부산", 2.0)
    }
}

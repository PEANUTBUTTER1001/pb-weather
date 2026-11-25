package com.pbweather.weather.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.repository.WidgetRepository
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.ForecastSource
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.ObservedWeather
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.work.WidgetScheduler
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun primaryDeletionUndoAndFailurePreserveRegionAndOriginalSuccessTime() = runBlocking {
        val file = context.cacheDir.resolve("widget-test.preferences_pb")
        file.delete()
        val job = SupervisorJob()
        try {
            val prefs =
                PreferencesRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }, Json)
            val a = Region("a", "서울 A", 60, 127, 37.57, 126.98, "108", "서울", 1.0)
            val b = a.copy(id = "b", name = "서울 B", nx = 61)
            fun weather(region: Region, temperature: Double, error: DomainError? = null) = ForecastState(
                now = Resource(
                    ObservedWeather(temperature),
                    SourceMeta(Provider.KMA, DataKind.NOWCAST, region.key, 1234L),
                    error = error
                )
            )
            val data = MutableStateFlow(mapOf(a.key to weather(a, 3.0), b.key to weather(b, 12.0, DomainError.TIMEOUT)))
            val forecasts = object : ForecastSource {
                override fun observe(region: Region) = data.map { it.getValue(region.key) }
                override suspend fun refresh(region: Region) = Unit
            }
            val widgets = WidgetRepository(prefs, forecasts, Clock.systemUTC())
            prefs.addFavorite(a)
            prefs.addFavorite(b)
            prefs.setPrimary(b.id)
            prefs.select(a)
            widgets.refreshPrimary()
            val failed = widgets.observe().first()
            assertEquals(b.id, failed.region?.id)
            assertEquals(a.id, failed.preferences.selected?.id)
            assertTrue(failed.preferences.widgetError)
            assertEquals(1234L, failed.forecast.now.meta?.receivedAt)
            assertEquals(12.0, failed.forecast.now.data?.temperature)
            val deleted = prefs.removeFavorite(b.id)!!
            assertEquals(a.id, widgets.observe().first().region?.id)
            prefs.restore(deleted)
            assertEquals(b.id, widgets.observe().first().region?.id)
            prefs.removeFavorite(a.id)
            prefs.removeFavorite(b.id)
            assertEquals(null, widgets.observe().first().region)
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }

    @Test fun repeatedSchedulingKeepsOnePeriodicJob() {
        WidgetScheduler.ensure(context).result.get(10, TimeUnit.SECONDS)
        WidgetScheduler.ensure(context).result.get(10, TimeUnit.SECONDS)
        val jobs = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WidgetScheduler.PERIODIC_NAME).get(10, TimeUnit.SECONDS)
        assertEquals(1, jobs.count { it.state != WorkInfo.State.CANCELLED })
    }
}

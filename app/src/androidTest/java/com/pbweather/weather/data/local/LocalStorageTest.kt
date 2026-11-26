package com.pbweather.weather.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.data.repository.LoadedSnapshot
import com.pbweather.weather.data.repository.RequestCoordinator
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.SourceMeta
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun clearingDuringRequestDiscardsLateSnapshotsAndDailyRowsButAllowsFreshRequests() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val clock = Clock.fixed(Instant.parse("2026-11-25T03:00:00Z"), ZoneOffset.UTC)
            val cache = CacheStore(db, Json, clock, CachePolicy(clock))
            val coordinator = RequestCoordinator(cache)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val meta = SourceMeta(Provider.KMA, DataKind.HISTORY, "108", clock.instant().toEpochMilli())
            val pending = async {
                coordinator.refresh("late", DataKind.HISTORY) {
                    started.complete(Unit)
                    release.await()
                    cache.saveObservation(DailyObservation("108", "2026-11-24", maximum = 10.0, meta = meta))
                    LoadedSnapshot("{}", meta)
                }
            }
            started.await()
            cache.clearWeather()
            release.complete(Unit)
            pending.await()
            assertEquals(0L, cache.sizeBytes())
            coordinator.refresh("new", DataKind.HISTORY) { LoadedSnapshot("{}", meta) }
            assertNotNull(cache.read("new", DataKind.HISTORY))
        } finally {
            db.close()
        }
    }

    @Test
    fun cacheDeletionPreservesPublicAssets() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val at = Instant.parse("2026-11-24T05:00:00Z")
            val clock = Clock.fixed(at, ZoneOffset.UTC)
            AssetImporter(context, db).ensureLoaded()
            val cache = CacheStore(db, Json, clock, CachePolicy(clock))
            cache.write("60_127", DataKind.NOWCAST, "{}", SourceMeta(Provider.KMA, DataKind.NOWCAST, "60_127", at.toEpochMilli()))
            assertNotNull(cache.read("60_127", DataKind.NOWCAST))
            cache.clearWeather()
            assertNull(cache.read("60_127", DataKind.NOWCAST))
            assertEquals(3839, db.weatherDao().regionCount())
            assertEquals(95, db.weatherDao().asosStations().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun preferencesSurviveStoreRestartWithPrimaryAndPalette() = runBlocking {
        val file = context.cacheDir.resolve("prefs-restart-test.preferences_pb")
        file.delete()
        val firstJob = SupervisorJob()
        val first = PreferencesRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file },
            Json
        )
        val region = Region("1100000000", "서울특별시", 60, 127, 37.5635694, 126.9800083, "108", "서울", 1.0)
        first.addFavorite(region)
        first.update { it.copy(theme = "DARK", palette = "TEAL", font = "LARGE") }
        firstJob.cancelAndJoin()
        val nextJob = SupervisorJob()
        try {
            val next = PreferencesRepository(
                PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file },
                Json
            )
            val restored = next.preferences.first()
            assertEquals("TEAL", restored.palette)
            assertEquals("DARK", restored.theme)
            assertEquals("LARGE", restored.font)
            assertEquals(region.id, restored.primaryId)
            assertEquals(listOf(region), restored.favorites)
        } finally {
            nextJob.cancelAndJoin()
            file.delete()
        }
    }
}

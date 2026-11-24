package com.pbweather.weather.data.location

import android.location.Location
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.pbweather.weather.MainActivity
import com.pbweather.weather.data.local.AssetImporter
import com.pbweather.weather.data.local.WeatherDatabase
import com.pbweather.weather.data.repository.LocationRepository
import com.pbweather.weather.domain.LocationException
import com.pbweather.weather.domain.LocationProblem
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun offlineSearchUsesLiteralQueryAndResolvesOfficialMetadata() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            val importer = AssetImporter(context, db)
            importer.ensureLoaded()
            val repo = LocationRepository(db, importer, CoarseLocationProvider(context))
            assertTrue(repo.search("서").isEmpty())
            assertTrue(repo.search("x".repeat(31)).isEmpty())
            assertTrue(repo.search("%%").isEmpty())
            val coldElapsed = measureTimeMillis { repo.search("서울") }
            assertTrue("Initialized catalog search exceeds 300 ms: $coldElapsed", coldElapsed < 300)
            val elapsed = measureTimeMillis {
                val results = repo.search("종로")
                assertTrue(results.isNotEmpty())
                assertTrue(results.all { it.name.contains("종로") && it.asosStationId == "108" })
                assertTrue(results.all { it.midLandRegId == "11B00000" && it.midTaRegId == "11B10101" })
            }
            assertTrue("Warm literal search exceeds 300 ms: $elapsed", elapsed < 300)
            val regions = db.weatherDao().regions()
            val mapping = RegionResolver(regions, db.weatherDao().asosStations(), db.weatherDao().midRegions())
            for (keyword in listOf("부산", "강릉", "제주", "울릉", "목포", "광산")) {
                val row = requireNotNull(regions.firstOrNull { it.name.contains(keyword) })
                val result = mapping.named(row)
                assertNotNull("Missing temperature region: $keyword", result.midTaRegId)
                assertNotNull("Missing land region: $keyword", result.midLandRegId)
            }
        } finally {
            db.close()
        }
    }

    @Ignore("User deferred location instrumentation on 2026-11-25; last result OUTSIDE_COVERAGE")
    @Test
    fun deniedThenForegroundSimulatedCoarseFix() = runBlocking {
        val provider = CoarseLocationProvider(context)
        assertEquals(false, provider.permissionGranted())
        val resolver = RegionResolver(emptyList(), emptyList(), emptyList())
        val error = runCatching { provider.resolve(resolver) }.exceptionOrNull() as LocationException
        assertEquals(LocationProblem.PERMISSION_DENIED, error.problem)
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        assertTrue(provider.permissionGranted())
        val db = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        try {
            AssetImporter(context, db).ensureLoaded()
            val dao = db.weatherDao()
            val catalog = RegionResolver(dao.regions(), dao.asosStations(), dao.midRegions())
            val activity = ActivityScenario.launch(MainActivity::class.java)
            val resolved = try {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                provider.resolve(catalog)
            } finally {
                activity.close()
            }
            assertTrue(resolved.usingCurrentLocation)
            assertTrue(resolved.nx in 57..64 && resolved.ny in 123..131)
            val row = dao.regions().first { it.name == resolved.name }
            assertEquals(row.latitude, resolved.publicLatitude, 0.0)
            assertEquals(row.longitude, resolved.publicLongitude, 0.0)
        } finally {
            db.close()
        }
    }
}

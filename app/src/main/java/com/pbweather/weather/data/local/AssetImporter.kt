package com.pbweather.weather.data.local

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AssetImporter(private val context: Context, private val db: WeatherDatabase) {
    private val mutex = Mutex()
    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dao = db.weatherDao()
            if (dao.regionCount() != 0) return@withLock
            fun rows(name: String) = context.assets.open(name).bufferedReader().use { r ->
                r.readLines().drop(1).filter { it.isNotBlank() }.map { it.trimEnd().split(',') }
            }
            val regions = rows("region_grid.csv").map { r ->
                RegionGridEntity(
                    r[0], r[1], r[2], r[3], r.subList(1, 4).filter { it.isNotBlank() }.joinToString(" "),
                    r[4].toInt(), r[5].toInt(), r[6].toDouble(), r[7].toDouble()
                )
            }
            val asos = rows("asos_stations.csv").map { AsosStationEntity(it[0], it[1], it[2].toDouble(), it[3].toDouble()) }
            val mid = rows("mid_regions.csv").map { MidRegionEntity(it[0], it[1], it[2]) }
            db.withTransaction {
                dao.saveRegions(regions)
                dao.saveAsos(asos)
                dao.saveMid(mid)
            }
        }
    }
}

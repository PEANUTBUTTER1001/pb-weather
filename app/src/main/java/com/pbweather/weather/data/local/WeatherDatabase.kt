package com.pbweather.weather.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "snapshots", primaryKeys = ["locationKey", "kind"], indices = [Index("lastAccessedAt")])
data class WeatherSnapshotEntity(
    val locationKey: String,
    val kind: String,
    val payload: String,
    val meta: String,
    val fetchedAt: Long,
    val expiresAt: Long,
    val lastAccessedAt: Long,
    val byteCount: Long
)

@Entity(tableName = "observations", primaryKeys = ["stationId", "date"], indices = [Index("lastAccessedAt")])
data class DailyObservationEntity(
    val stationId: String,
    val date: String,
    val payload: String,
    val lastAccessedAt: Long,
    val byteCount: Long
)

@Entity(tableName = "regions", indices = [Index("name")])
data class RegionGridEntity(
    @PrimaryKey val code: String,
    val sido: String,
    val sigungu: String,
    val dong: String,
    val name: String,
    val nx: Int,
    val ny: Int,
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "asos_stations")
data class AsosStationEntity(@PrimaryKey val id: String, val name: String, val latitude: Double, val longitude: Double)

@Entity(tableName = "mid_regions")
data class MidRegionEntity(@PrimaryKey val code: String, val name: String, val kind: String)

@Entity(tableName = "air_stations")
data class AirStationEntity(
    @PrimaryKey val name: String,
    val latitude: Double,
    val longitude: Double,
    val fetchedAt: Long,
    val network: String
)

@Entity(tableName = "air_links")
data class AirLinkEntity(@PrimaryKey val regionKey: String, val stationName: String, val isFallback: Boolean, val distanceKm: Double)
data class CacheIndexRow(val store: String, val key1: String, val key2: String, val byteCount: Long, val lastAccessedAt: Long)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM snapshots WHERE locationKey = :key")
    fun observeSnapshots(key: String): Flow<List<WeatherSnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE locationKey = :key AND kind = :kind")
    suspend fun snapshot(key: String, kind: String): WeatherSnapshotEntity?

    @Upsert suspend fun saveSnapshot(entity: WeatherSnapshotEntity)

    @Query("UPDATE snapshots SET lastAccessedAt = :now WHERE locationKey = :key")
    suspend fun touchSnapshots(key: String, now: Long)

    @Query("SELECT * FROM observations WHERE stationId = :station AND date BETWEEN :start AND :end ORDER BY date")
    fun observeHistory(station: String, start: String, end: String): Flow<List<DailyObservationEntity>>

    @Query("SELECT * FROM observations WHERE stationId = :station AND date BETWEEN :start AND :end ORDER BY date")
    suspend fun history(station: String, start: String, end: String): List<DailyObservationEntity>

    @Upsert suspend fun saveObservation(entity: DailyObservationEntity)

    @Query("UPDATE observations SET lastAccessedAt = :now WHERE stationId = :station AND date BETWEEN :start AND :end")
    suspend fun touchHistory(station: String, start: String, end: String, now: Long)

    @Query("SELECT * FROM regions WHERE instr(name, :query) > 0 ORDER BY length(name), name LIMIT 60")
    suspend fun search(query: String): List<RegionGridEntity>

    @Query("SELECT * FROM regions")
    suspend fun regions(): List<RegionGridEntity>

    @Query("SELECT COUNT(*) FROM regions")
    suspend fun regionCount(): Int

    @Upsert suspend fun saveRegions(rows: List<RegionGridEntity>)

    @Upsert suspend fun saveAsos(rows: List<AsosStationEntity>)

    @Upsert suspend fun saveMid(rows: List<MidRegionEntity>)

    @Query("SELECT * FROM asos_stations")
    suspend fun asosStations(): List<AsosStationEntity>

    @Query("SELECT * FROM mid_regions")
    suspend fun midRegions(): List<MidRegionEntity>

    @Query("DELETE FROM air_stations")
    suspend fun clearAirStations()

    @Upsert suspend fun saveAirStations(rows: List<AirStationEntity>)

    @Query("SELECT * FROM air_stations")
    suspend fun airStations(): List<AirStationEntity>

    @Upsert suspend fun saveAirLink(row: AirLinkEntity)

    @Query("SELECT * FROM air_links WHERE regionKey = :key")
    fun observeAirLink(key: String): Flow<AirLinkEntity?>

    @Query("SELECT * FROM air_links WHERE regionKey = :key")
    suspend fun airLink(key: String): AirLinkEntity?

    @Query(
        """
        SELECT 'snapshot' AS store, locationKey AS key1, kind AS key2, byteCount, lastAccessedAt FROM snapshots
        UNION ALL
        SELECT 'history', stationId, date, byteCount, lastAccessedAt FROM observations
        """
    )
    suspend fun cacheIndex(): List<CacheIndexRow>

    @Query("DELETE FROM snapshots WHERE locationKey = :key AND kind = :kind")
    suspend fun deleteSnapshot(key: String, kind: String)

    @Query("DELETE FROM observations WHERE stationId = :station AND date = :date")
    suspend fun deleteObservation(station: String, date: String)

    @Query("SELECT COALESCE(SUM(byteCount), 0) FROM (SELECT byteCount FROM snapshots UNION ALL SELECT byteCount FROM observations)")
    fun observeCacheBytes(): Flow<Long>

    @Query("DELETE FROM snapshots")
    suspend fun clearSnapshots()

    @Query("DELETE FROM observations")
    suspend fun clearObservations()
}

@Database(
    entities = [
        WeatherSnapshotEntity::class, DailyObservationEntity::class, RegionGridEntity::class,
        AsosStationEntity::class, MidRegionEntity::class, AirStationEntity::class, AirLinkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}

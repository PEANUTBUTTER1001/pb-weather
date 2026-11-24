package com.pbweather.weather.data.location

import com.pbweather.weather.core.geo.KmaGridConverter
import com.pbweather.weather.core.geo.NearestFinder
import com.pbweather.weather.core.geo.StationPoint
import com.pbweather.weather.data.local.AsosStationEntity
import com.pbweather.weather.data.local.MidRegionEntity
import com.pbweather.weather.data.local.RegionGridEntity
import com.pbweather.weather.domain.LocationException
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.Region

class RegionResolver(
    private val regions: List<RegionGridEntity>,
    private val stations: List<AsosStationEntity>,
    private val mid: List<MidRegionEntity>
) {
    private data class MidPoint(val row: MidRegionEntity, val latitude: Double, val longitude: Double)
    private val stationPoints = stations.map { StationPoint(it.id, it.name, it.latitude, it.longitude) }
    private fun prefixes(sido: String): Set<String> = when {
        listOf("서울", "인천", "경기").any { sido.startsWith(it) } -> setOf("11B", "11A")
        sido.startsWith("충청북") -> setOf("11C1")
        listOf("충청남", "대전", "세종").any { sido.startsWith(it) } -> setOf("11C2")
        sido.startsWith("강원") -> setOf("11D")
        sido.startsWith("전북") -> setOf("11F1", "21F1")
        listOf("전라", "전남", "광주").any { sido.startsWith(it) } -> setOf("11F2", "21F2")
        listOf("경상북", "대구").any { sido.startsWith(it) } -> setOf("11H1", "11E")
        listOf("경상남", "부산", "울산").any { sido.startsWith(it) } -> setOf("11H2")
        sido.startsWith("제주") -> setOf("11G")
        else -> emptySet()
    }
    private val scopes = regions.map { it.sido }.distinct().associateWith(::prefixes)
    private val midPoints = mid.filter { it.kind == "C" }.mapNotNull { row ->
        val locality = row.name.removeSuffix("도")
        val public = regions.filter { region ->
            scopes[region.sido].orEmpty().any { row.code.startsWith(it) } && (
                region.sigungu.startsWith(row.name + "시") || region.sigungu.startsWith(row.name + "군") ||
                    region.dong.startsWith(locality) || region.sigungu.startsWith(locality + "군") ||
                    region.sido.startsWith(row.name)
                )
        }.minWithOrNull(compareBy<RegionGridEntity> { if (it.dong.isBlank()) 0 else 1 }.thenBy { it.name.length })
        // KMA 2610 merged Gwangju into Jeonnam. Use the official ASOS 156 reference for this legacy mid code.
        val station = if (row.code == "11F20501") stations.firstOrNull { it.id == "156" } else null
        if (public != null) {
            MidPoint(row, public.latitude, public.longitude)
        } else {
            station?.let { MidPoint(row, it.latitude, it.longitude) }
        }
    }
    fun named(row: RegionGridEntity): Region = resolve(row, row.latitude, row.longitude, false)

    /** Device coordinates are used only during this call; returned coordinates always come from the public catalog. */
    fun current(latitude: Double, longitude: Double): Region {
        if (!latitude.isFinite() || !longitude.isFinite()) throw LocationException(LocationProblem.OUTSIDE_COVERAGE)
        val nearest = regions.minByOrNull {
            NearestFinder.distanceKm(latitude, longitude, it.latitude, it.longitude)
        } ?: throw LocationException(LocationProblem.UNAVAILABLE)
        if (NearestFinder.distanceKm(latitude, longitude, nearest.latitude, nearest.longitude) > 80) {
            throw LocationException(LocationProblem.OUTSIDE_COVERAGE)
        }
        return resolve(nearest, latitude, longitude, true)
    }
    private fun resolve(row: RegionGridEntity, latitude: Double, longitude: Double, current: Boolean): Region {
        val nearest = NearestFinder.nearest(latitude, longitude, stationPoints).firstOrNull()
            ?: throw LocationException(LocationProblem.UNAVAILABLE)
        val point = midPoints.filter { p -> scopes[row.sido].orEmpty().any { p.row.code.startsWith(it) } }
            .minByOrNull { NearestFinder.distanceKm(latitude, longitude, it.latitude, it.longitude) }
        val temperature = point?.row?.code
        val land = temperature?.let { code ->
            mid.filter { it.kind == "A" && code.replaceFirst("21F", "11F").startsWith(it.code.trimEnd('0')) }
                .maxByOrNull { it.code.trimEnd('0').length }?.code
                ?: code.takeIf { it.startsWith("11A") || it.startsWith("11E") }
        }
        val grid = if (current) KmaGridConverter.convert(latitude, longitude) else null
        return Region(
            id = if (current) "current_${grid!!.nx}_${grid.ny}" else row.code,
            name = row.name,
            nx = grid?.nx ?: row.nx,
            ny = grid?.ny ?: row.ny,
            publicLatitude = row.latitude,
            publicLongitude = row.longitude,
            asosStationId = nearest.station.id,
            asosStationName = nearest.station.name,
            asosDistanceKm = nearest.distanceKm,
            midLandRegId = land,
            midTaRegId = temperature,
            usingCurrentLocation = current
        )
    }
}

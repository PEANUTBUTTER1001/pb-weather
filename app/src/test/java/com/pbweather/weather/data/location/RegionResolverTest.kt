package com.pbweather.weather.data.location

import com.pbweather.weather.data.local.AsosStationEntity
import com.pbweather.weather.data.local.MidRegionEntity
import com.pbweather.weather.data.local.RegionGridEntity
import com.pbweather.weather.domain.LocationException
import com.pbweather.weather.domain.LocationProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionResolverTest {
    private val row = RegionGridEntity("a", "서울특별시", "종로구", "", "서울특별시 종로구", 60, 127, 37.57, 126.98)
    private val stations = listOf(AsosStationEntity("108", "서울", 37.57, 126.98))
    private val codes = listOf(MidRegionEntity("11B00000", "서울.인천.경기", "A"), MidRegionEntity("11B10101", "서울", "C"))

    @Test fun `transient position returns only public reference coordinates`() {
        val result = RegionResolver(listOf(row), stations, codes).current(37.575123, 126.985456)
        assertEquals(row.latitude, result.publicLatitude, 0.0)
        assertEquals(row.longitude, result.publicLongitude, 0.0)
        assertNotEquals(37.575123, result.publicLatitude, 0.0)
        assertTrue(result.usingCurrentLocation)
        assertEquals("11B10101", result.midTaRegId)
        assertEquals("11B00000", result.midLandRegId)
    }

    @Test fun `outside coverage and invalid position produce explicit failure`() {
        val resolver = RegionResolver(listOf(row), stations, codes)
        listOf(0.0 to 0.0, Double.NaN to 126.0).forEach { (lat, lon) ->
            val error = runCatching { resolver.current(lat, lon) }.exceptionOrNull() as LocationException
            assertEquals(LocationProblem.OUTSIDE_COVERAGE, error.problem)
        }
    }

    @Test fun `same city name stays inside its province code group`() {
        val south = row.copy(code = "south", sido = "전남광주통합특별시", sigungu = "광산구", name = "광주", latitude = 35.16)
        val north = row.copy(code = "north", sido = "경기도", sigungu = "광주시", name = "광주", latitude = 37.4)
        val mid = listOf(
            MidRegionEntity("11B20702", "광주", "C"),
            MidRegionEntity("11F20501", "광주", "C"),
            MidRegionEntity("11B00000", "서울.인천.경기", "A"),
            MidRegionEntity("11F20000", "전라남도", "A")
        )
        val resolver = RegionResolver(listOf(south, north), stations + AsosStationEntity("156", "광주", 35.17, 126.89), mid)
        assertEquals("11F20501", resolver.named(south).midTaRegId)
        assertEquals("11B20702", resolver.named(north).midTaRegId)
    }
}

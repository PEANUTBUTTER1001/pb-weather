package com.pbweather.weather.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    @Test
    fun `official grid comparison preserves the known source discrepancy`() {
        val stream = requireNotNull(javaClass.getResourceAsStream("/fixtures/grid_samples.csv"))
        val lines = stream.bufferedReader().use { it.readLines().drop(1) }
        assertEquals(10, lines.size)
        var discrepancies = 0
        for (line in lines) {
            val r = line.split(',')
            val registered = GridPoint(r[6].toInt(), r[7].toInt())
            val converted = KmaGridConverter.convert(r[4].toDouble(), r[5].toDouble())
            if (r[0] == "2872033000") {
                // Preserve the published 2610 source inconsistency; TEST-008 is not 10/10 PASS.
                assertEquals(GridPoint(21, 135), registered)
                assertEquals(GridPoint(21, 136), converted)
                discrepancies++
            } else {
                assertEquals(r[0], registered, converted)
            }
        }
        assertEquals(1, discrepancies)
    }

    @Test
    fun `nearest station selection is ordered and distance symmetric`() {
        val a = StationPoint("A", "A", 37.5, 127.0)
        val b = StationPoint("B", "B", 35.1, 129.0)
        val result = NearestFinder.nearest(37.5, 127.0, listOf(b, a), 2)
        assertEquals("A", result.first().station.id)
        assertEquals(0.0, result.first().distanceKm, 0.000001)
        assertEquals(
            NearestFinder.distanceKm(37.5, 127.0, 35.1, 129.0),
            NearestFinder.distanceKm(35.1, 129.0, 37.5, 127.0),
            0.000001
        )
        assertTrue(result.last().distanceKm > 200)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid coordinates cannot enter conversion`() {
        KmaGridConverter.convert(Double.NaN, 127.0)
    }
}

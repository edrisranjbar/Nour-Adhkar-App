package com.example

import com.example.prayer.qiblaBearing
import com.example.prayer.qiblaTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaDirectionTest {
    @Test fun turnCrossesNorthByShortestRoute() {
        assertEquals(2f, qiblaTurn(1.0, 359.0), 0.001f)
        assertEquals(-2f, qiblaTurn(359.0, 1.0), 0.001f)
        assertEquals(0f, qiblaTurn(230.0, 590.0), 0.001f)
    }
    @Test fun directionsPointTowardMecca() {
        assertTrue(qiblaBearing(35.6892, 51.3890) in 215.0..220.0)
        assertTrue(qiblaBearing(51.5074, -0.1278) in 118.0..120.0)
        assertEquals(180.0, qiblaBearing(40.0, 39.826206), 0.01)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidCoordinates() { qiblaBearing(Double.NaN, 0.0) }
}

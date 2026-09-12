package com.example.prayer

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Qibla

fun qiblaBearing(latitude: Double, longitude: Double): Double {
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    return Qibla(Coordinates(latitude, longitude)).direction
}

/** Signed shortest turn, clockwise positive, including across north. */
fun qiblaTurn(bearing: Double, heading: Double): Float =
    (((bearing - heading) % 360 + 540) % 360 - 180).toFloat()

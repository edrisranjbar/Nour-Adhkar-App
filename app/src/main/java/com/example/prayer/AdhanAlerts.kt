package com.example.prayer

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

enum class AdhanPrayer(val label: String, val timeIndex: Int) {
    FAJR("صبح", 0), DHUHR("ظهر", 2), ASR("عصر", 3), MAGHRIB("مغرب", 4), ISHA("عشا", 5)
}

fun nextAdhanTime(settings: PrayerSettings, prayer: AdhanPrayer, now: Long): Long? {
    if (!settings.isValid()) return null
    val day = Calendar.getInstance(TimeZone.getTimeZone(settings.zone)).apply { timeInMillis = now }
    repeat(2) {
        // The library retains incidental Calendar milliseconds; use stable second precision.
        val time = settings.times(Date(day.timeInMillis))[prayer.timeIndex].second?.time?.let { it / 1000 * 1000 }
        if (time != null && time > now) return time
        day.add(Calendar.DAY_OF_YEAR, 1)
    }
    return null
}

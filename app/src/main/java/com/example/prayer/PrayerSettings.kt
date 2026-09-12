package com.example.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

val prayerMethods = linkedMapOf(
    "MUSLIM_WORLD_LEAGUE" to "رابطة العالم الاسلامی",
    "KARACHI" to "دانشگاه علوم اسلامی کراچی",
    "EGYPTIAN" to "سازمان نقشه‌برداری مصر",
    "KUWAIT" to "وزارت اوقاف کویت",
    "UMM_AL_QURA" to "ام‌القری، مکه",
    "NORTH_AMERICA" to "آمریکای شمالی (ISNA)",
    "QATAR" to "قطر",
    "DUBAI" to "دبی (مدل محاسباتی Adhan)",
    "SINGAPORE" to "سنگاپور",
    "MOON_SIGHTING_COMMITTEE" to "کمیته رؤیت هلال"
)

data class PrayerSettings(
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val zone: String = "Asia/Tehran",
    val method: String = "MUSLIM_WORLD_LEAGUE",
    val hanafi: Boolean = false,
    val automaticLocation: Boolean = true
) {
    fun isValid() = location.isNotBlank() && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        zone in TimeZone.getAvailableIDs() && method in prayerMethods

    fun times(now: Date): List<Pair<String, Date?>> {
        require(isValid())
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(zone)).apply { time = now }
        val parameters = CalculationMethod.valueOf(method).parameters.apply {
            madhab = if (hanafi) Madhab.HANAFI else Madhab.SHAFI
        }
        val times = PrayerTimes(Coordinates(latitude, longitude), DateComponents(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)), parameters)
        return listOf("صبح" to times.fajr, "طلوع" to times.sunrise, "ظهر" to times.dhuhr,
            "عصر" to times.asr, "مغرب" to times.maghrib, "عشاء" to times.isha)
    }
}

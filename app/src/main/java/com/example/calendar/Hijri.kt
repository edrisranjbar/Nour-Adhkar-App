package com.example.calendar

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone

/** A lunar Hijri date; [month] is 1 (محرم) to 12 (ذی‌الحجه). */
data class HijriDate(val year: Int, val month: Int, val day: Int)

/**
 * Hijri dates from Android's built-in ICU Umm al-Qura calendar (available from API 24, so no
 * extra library or network). Umm al-Qura is a calculated calendar; the start of a month by local
 * moon sighting can differ by a day or two, which the user corrects with [MAX_OFFSET] days of
 * offset in the calendar screen. The app makes no claim about which day is official.
 */
object Hijri {
    const val MAX_OFFSET = 2
    private const val MILLIS_PER_DAY = 86_400_000L

    val monthNames = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    private val calendar by lazy {
        IslamicCalendar(IcuTimeZone.GMT_ZONE).apply {
            calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        }
    }

    /** The Hijri date of [jdn]; [offsetDays] (±[MAX_OFFSET]) shifts it to match local sighting. */
    fun fromJdn(jdn: Int, offsetDays: Int = 0): HijriDate = synchronized(calendar) {
        // Noon UTC keeps the instant inside the intended civil day.
        calendar.timeInMillis = Gregorian.epochDay(jdn + offsetDays) * MILLIS_PER_DAY + MILLIS_PER_DAY / 2
        HijriDate(
            year = calendar.get(IslamicCalendar.YEAR),
            month = calendar.get(IslamicCalendar.MONTH) + 1,
            day = calendar.get(IslamicCalendar.DAY_OF_MONTH)
        )
    }
}

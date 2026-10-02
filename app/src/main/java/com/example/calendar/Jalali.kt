package com.example.calendar

import java.util.Calendar
import java.util.TimeZone

/** A Solar Hijri (Jalali/Shamsi) date; [month] is 1 (فروردین) to 12 (اسفند). */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * Jalali ↔ Julian Day Number conversion using the published "jalaali" break-year algorithm
 * (Borkowski's arithmetic, as in the jalaali-js library). It matches the official Iranian
 * calendar for years 1178–1633, which covers every date the app can show.
 *
 * Days are Julian Day Numbers (JDN): plain integers, so a month grid needs no time zones or
 * daylight-saving handling. Use [Gregorian] to move between JDN and wall-clock dates.
 */
object Jalali {
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private class YearInfo(val leap: Int, val gregorianYear: Int, val march: Int)

    private fun yearInfo(jy: Int): YearInfo {
        require(jy >= breaks.first() && jy < breaks.last()) { "Jalali year out of range: $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += tdiv(jump, 33) * 8 + tdiv(tmod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += tdiv(n, 33) * 8 + tdiv(tmod(n, 33) + 3, 4)
        if (tmod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = tdiv(gy, 4) - tdiv((tdiv(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + tdiv(jump + 4, 33) * 33
        var leap = tmod(tmod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return YearInfo(leap, gy, march)
    }

    fun isLeapYear(year: Int): Boolean = yearInfo(year).leap == 0

    fun monthLength(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        isLeapYear(year) -> 30
        else -> 29
    }

    fun toJdn(year: Int, month: Int, day: Int): Int {
        val info = yearInfo(year)
        return Gregorian.toJdn(info.gregorianYear, 3, info.march) +
            (month - 1) * 31 - tdiv(month, 7) * (month - 7) + day - 1
    }

    fun toJdn(date: JalaliDate): Int = toJdn(date.year, date.month, date.day)

    fun fromJdn(jdn: Int): JalaliDate {
        val gy = Gregorian.fromJdn(jdn).year
        var jy = gy - 621
        val info = yearInfo(jy)
        var k = jdn - Gregorian.toJdn(gy, 3, info.march)
        if (k >= 0) {
            if (k <= 185) return JalaliDate(jy, 1 + tdiv(k, 31), tmod(k, 31) + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (info.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + tdiv(k, 30), tmod(k, 30) + 1)
    }

    /** The month after [year]/[month] shifted by [delta] months (negative goes back). */
    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) + delta
        return Math.floorDiv(index, 12) to Math.floorMod(index, 12) + 1
    }
}

/** A proleptic Gregorian date; [month] is 1–12. */
data class GregorianDate(val year: Int, val month: Int, val day: Int)

object Gregorian {
    private const val JDN_UNIX_EPOCH = 2_440_588

    val monthNames = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    fun toJdn(year: Int, month: Int, day: Int): Int {
        var d = tdiv((year + tdiv(month - 8, 6) + 100100) * 1461, 4) +
            tdiv(153 * tmod(month + 9, 12) + 2, 5) + day - 34840408
        d = d - tdiv(tdiv(year + 100100 + tdiv(month - 8, 6), 100) * 3, 4) + 752
        return d
    }

    fun fromJdn(jdn: Int): GregorianDate {
        var j = 4 * jdn + 139361631
        j += tdiv(tdiv(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = tdiv(tmod(j, 1461), 4) * 5 + 308
        val day = tdiv(tmod(i, 153), 5) + 1
        val month = tmod(tdiv(i, 153), 12) + 1
        val year = tdiv(j, 1461) - 100100 + tdiv(8 - month, 6)
        return GregorianDate(year, month, day)
    }

    /** The local calendar day containing [millis] in [zone]. */
    fun jdnOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return toJdn(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    /** Local midnight of [jdn] in [zone]; this is the day key used by activity and checklist data. */
    fun startOfDayMillis(jdn: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val date = fromJdn(jdn)
        return Calendar.getInstance(zone).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
        }.timeInMillis
    }

    /** Days since 1970-01-01, as used by java.time and ICU. */
    fun epochDay(jdn: Int): Long = (jdn - JDN_UNIX_EPOCH).toLong()
}

/** Weekdays in the Iranian order, Saturday first. */
object Weekday {
    val names = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val initials = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    const val FRIDAY = 6

    /** 0 = Saturday … 6 = Friday. */
    fun indexOf(jdn: Int): Int = Math.floorMod(jdn + 2, 7)
}

// The jalaali algorithm is defined with truncating division, so these differ from floorDiv/floorMod.
private fun tdiv(a: Int, b: Int): Int = a / b
private fun tmod(a: Int, b: Int): Int = a - (a / b) * b

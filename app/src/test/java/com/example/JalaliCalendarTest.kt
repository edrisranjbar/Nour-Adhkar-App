package com.example

import com.example.calendar.Gregorian
import com.example.calendar.GregorianDate
import com.example.calendar.Jalali
import com.example.calendar.JalaliDate
import com.example.calendar.Weekday
import com.example.ui.util.formatInboxDate
import com.example.ui.util.toPersianDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliCalendarTest {
    private fun jdn(year: Int, month: Int, day: Int) = Gregorian.toJdn(year, month, day)

    @Test
    fun `Nowruz falls on the known Gregorian dates`() {
        assertEquals(JalaliDate(1403, 1, 1), Jalali.fromJdn(jdn(2024, 3, 20)))
        assertEquals(JalaliDate(1404, 1, 1), Jalali.fromJdn(jdn(2025, 3, 21)))
        assertEquals(JalaliDate(1405, 1, 1), Jalali.fromJdn(jdn(2026, 3, 21)))
        assertEquals(jdn(2026, 3, 21), Jalali.toJdn(1405, 1, 1))
    }

    @Test
    fun `leap years give Esfand thirty days`() {
        assertTrue(Jalali.isLeapYear(1399))
        assertTrue(Jalali.isLeapYear(1403))
        assertTrue(Jalali.isLeapYear(1408))
        assertFalse(Jalali.isLeapYear(1404))
        assertFalse(Jalali.isLeapYear(1405))
        assertEquals(30, Jalali.monthLength(1403, 12))
        assertEquals(29, Jalali.monthLength(1404, 12))
        assertEquals(JalaliDate(1403, 12, 30), Jalali.fromJdn(jdn(2025, 3, 20)))
    }

    @Test
    fun `month lengths follow the 31-30-29 pattern`() {
        (1..6).forEach { assertEquals(31, Jalali.monthLength(1405, it)) }
        (7..11).forEach { assertEquals(30, Jalali.monthLength(1405, it)) }
        val yearLength = (1..12).sumOf { Jalali.monthLength(1405, it) }
        assertEquals(Jalali.toJdn(1406, 1, 1) - Jalali.toJdn(1405, 1, 1), yearLength)
    }

    @Test
    fun `round trips and agrees with the app's existing converter for every day 2000 to 2040`() {
        var day = jdn(2000, 1, 1)
        val end = jdn(2040, 12, 31)
        while (day <= end) {
            val jalali = Jalali.fromJdn(day)
            assertEquals(day, Jalali.toJdn(jalali))
            val g = Gregorian.fromJdn(day)
            val iso = "%04d-%02d-%02d".format(g.year, g.month, g.day)
            val expected = "${jalali.day} ${Jalali.monthNames[jalali.month - 1]} ${jalali.year}".toPersianDigits()
            assertEquals(iso, expected, formatInboxDate(iso))
            day++
        }
    }

    @Test
    fun `gregorian conversion and weekdays are consistent`() {
        assertEquals(2_440_588, jdn(1970, 1, 1))
        assertEquals(GregorianDate(2026, 10, 2), Gregorian.fromJdn(jdn(2026, 10, 2)))
        assertEquals(GregorianDate(2024, 2, 29), Gregorian.fromJdn(jdn(2024, 2, 29)))
        // 1970-01-01 was a Thursday; 2026-03-20 was a Friday; 2026-03-21 a Saturday.
        assertEquals(5, Weekday.indexOf(jdn(1970, 1, 1)))
        assertEquals(Weekday.FRIDAY, Weekday.indexOf(jdn(2026, 3, 20)))
        assertEquals(0, Weekday.indexOf(jdn(2026, 3, 21)))
    }

    @Test
    fun `shifting months crosses year boundaries`() {
        assertEquals(1406 to 1, Jalali.shiftMonth(1405, 12, 1))
        assertEquals(1404 to 12, Jalali.shiftMonth(1405, 1, -1))
        assertEquals(1405 to 7, Jalali.shiftMonth(1405, 7, 0))
        assertEquals(1403 to 7, Jalali.shiftMonth(1405, 7, -24))
    }
}

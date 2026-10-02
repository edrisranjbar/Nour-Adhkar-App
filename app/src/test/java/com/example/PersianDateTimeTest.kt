package com.example

import com.example.ui.util.formatPersianDateTime
import com.example.ui.util.formatInboxDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class PersianDateTimeTest {
    @Test
    fun `formats inbox dates with a Persian month and Persian digits`() {
        assertEquals("۲۶ شهریور ۱۴۰۴", formatInboxDate("2025-09-17T12:30:00.000000Z"))
        assertEquals("۲۶ شهریور ۱۴۰۴", formatInboxDate("2025-09-17"))
        assertEquals("۲۶ شهریور ۱۴۰۴", formatInboxDate("2025-09-17T23:30:00Z"))
        assertEquals("۱ فروردین ۱۴۰۵", formatInboxDate("2026-03-21 08:05:00"))
    }

    @Test
    fun `does not invent an inbox date for missing or invalid values`() {
        listOf("", "null", "2025-02-30", "2025-13-01").forEach {
            assertEquals("تاریخ نامشخص", formatInboxDate(it))
        }
    }

    private val tehranTimeZone = TimeZone.getTimeZone("Asia/Tehran")

    @Test
    fun `formats a Shahrivar date with Persian digits`() {
        val timestamp = timestampOf(2026, Calendar.AUGUST, 29, 14, 29)

        assertEquals(
            "۷ شهریور ۱۴۰۵ - ۱۴:۲۹",
            formatPersianDateTime(timestamp, tehranTimeZone)
        )
    }

    @Test
    fun `formats Nowruz as the first day of the Persian year`() {
        val timestamp = timestampOf(2026, Calendar.MARCH, 21, 8, 5)

        assertEquals(
            "۱ فروردین ۱۴۰۵ - ۰۸:۰۵",
            formatPersianDateTime(timestamp, tehranTimeZone)
        )
    }

    private fun timestampOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(tehranTimeZone).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}

package com.example

import com.example.ui.util.formatPersianDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class PersianDateTimeTest {
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

package com.example

import com.example.prayer.PrayerSettings
import com.example.widget.PrayerWidgetSchedule
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerWidgetScheduleTest {
    private val settings = PrayerSettings("تهران", 35.6892, 51.3890)
    private fun utc(text: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(text)!!

    @Test fun missingLocationDoesNotInventTimes() {
        assertNull(PrayerWidgetSchedule.calculate(PrayerSettings(), Date()))
    }

    @Test fun sunriseIsNotNextPrayer() {
        val today = settings.times(utc("2026-09-20 08:00"))
        val afterFajr = Date(today[0].second!!.time + 60_000)
        val result = PrayerWidgetSchedule.calculate(settings, afterFajr)!!
        assertEquals("ظهر", result.next!!.first)
        assertFalse(result.tomorrow)
        assertTrue(result.refreshAt > afterFajr.time)
    }

    @Test fun afterIshaShowsTomorrowButKeepsTodaysTimetable() {
        val day = utc("2026-09-20 08:00")
        val today = settings.times(day)
        val result = PrayerWidgetSchedule.calculate(settings, Date(today.last().second!!.time + 60_000))!!
        assertEquals("صبح", result.next!!.first)
        assertTrue(result.tomorrow)
        assertEquals(today.map { it.second!!.time / 60_000 }, result.today.map { it.second!!.time / 60_000 })
        assertEquals(utc("2026-09-20 20:30").time, result.refreshAt)
        assertEquals(settings.times(utc("2026-09-21 08:00"))[0].second!!.time / 60_000,
            result.next.second!!.time / 60_000)
    }

    @Test fun midnightUsesSavedZoneRatherThanDeviceZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val before = PrayerWidgetSchedule.calculate(settings, utc("2026-09-20 20:29"))!!
            val after = PrayerWidgetSchedule.calculate(settings, utc("2026-09-20 20:31"))!!
            assertTrue(before.tomorrow)
            assertFalse(after.tomorrow)
            assertNotEquals(before.today[0].second!!.time / 60_000, after.today[0].second!!.time / 60_000)
        } finally { TimeZone.setDefault(original) }
    }
}

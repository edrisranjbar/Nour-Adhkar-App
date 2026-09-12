package com.example

import com.example.prayer.PrayerSettings
import com.example.prayer.prayerMethods
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PrayerSettingsTest {
    @Test fun everyAvailableMethodCalculatesBothAsrOptions() {
        val expected = setOf("MUSLIM_WORLD_LEAGUE", "KARACHI", "EGYPTIAN", "KUWAIT",
            "UMM_AL_QURA", "NORTH_AMERICA", "QATAR", "DUBAI", "SINGAPORE", "MOON_SIGHTING_COMMITTEE")
        assertEquals(expected, prayerMethods.keys)
        for (method in expected) {
            for (hanafi in listOf(false, true)) {
                val selected = settings.copy(method = method, hanafi = hanafi)
                assertTrue(method, selected.isValid())
                val times = selected.times(utc("2026-09-04 12:00"))
                assertEquals(6, times.size)
                assertTrue(method, times.all { it.second != null })
                assertTrue(method, times.zipWithNext().all { (a, b) -> a.second!!.before(b.second) })
            }
        }
        assertFalse(settings.copy(method = "UNKNOWN").isValid())
    }

    private val settings = PrayerSettings("تهران", 35.6892, 51.3890)
    private fun utc(value: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(value)!!

    @Test fun rejectsMissingLocationInvalidCoordinatesAndUnknownZone() {
        assertFalse(PrayerSettings().isValid())
        assertFalse(settings.copy(latitude = Double.NaN).isValid())
        assertFalse(settings.copy(longitude = 181.0).isValid())
        assertFalse(settings.copy(zone = "Tehran").isValid())
        assertTrue(settings.isValid())
    }

    @Test fun prayersAreOrderedAndHanafiAsrIsLater() {
        val day = utc("2026-09-04 12:00")
        val times = settings.times(day).map { it.second!! }
        assertTrue(times.zipWithNext().all { (a, b) -> a.before(b) })
        assertTrue(settings.copy(hanafi = true).times(day)[3].second!!.after(times[3]))
        assertNotEquals(settings.copy(method = "KARACHI").times(day).last().second, times.last())
    }

    @Test fun calculationDateUsesLocationTimezoneAcrossUtcMidnight() {
        // Adhan returns minute-rounded times but retains Calendar milliseconds.
        // Compare the timetable's displayed precision, not incidental milliseconds.
        fun minutes(value: String) = settings.times(utc(value)).map { it.second!!.time / 60_000 }
        assertEquals(minutes("2026-09-03 22:00"), minutes("2026-09-04 12:00"))
        assertNotEquals(minutes("2026-09-03 18:00"), minutes("2026-09-04 12:00"))
    }
}

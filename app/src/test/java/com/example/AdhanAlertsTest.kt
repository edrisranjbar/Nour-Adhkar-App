package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import com.example.prayer.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdhanAlertsTest {
    @Test fun schedulerCreatesOnlyEnabledAlarmsAndCancelsUncheckedPrayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", 0).edit().clear().commit()
        val prefs = PreferenceRepository(context)
        prefs.setPrayerSettings(PrayerSettings("مکه", 21.4225, 39.8262, "Asia/Riyadh"))
        prefs.setAdhanSound(AdhanSound("adhan_one"))
        val alarms = org.robolectric.Shadows.shadowOf(context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        prefs.setAdhanPrayer(AdhanPrayer.FAJR, true)
        prefs.setAdhanPrayer(AdhanPrayer.ISHA, true)
        val scheduler = AdhanScheduler(context)
        scheduler.reschedule()
        assertEquals(2, alarms.scheduledAlarms.size)
        scheduler.reschedule()
        assertEquals(2, alarms.scheduledAlarms.size)
        prefs.setAdhanPrayer(AdhanPrayer.FAJR, false)
        scheduler.reschedule()
        assertEquals(1, alarms.scheduledAlarms.size)
        prefs.setAdhanPrayer(AdhanPrayer.ISHA, false)
        scheduler.reschedule()
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test fun independentPrayerChoicesPersistWithoutChangingReminders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", 0).edit().clear().commit()
        val prefs = PreferenceRepository(context)
        assertTrue(prefs.getAdhanPrayers().isEmpty())
        prefs.setMorningNotificationTime("06:45")
        prefs.setAdhanPrayer(AdhanPrayer.FAJR, true)
        prefs.setAdhanPrayer(AdhanPrayer.ISHA, true)
        prefs.setAdhanPrayer(AdhanPrayer.FAJR, false)
        assertEquals(setOf(AdhanPrayer.ISHA), PreferenceRepository(context).getAdhanPrayers())
        assertEquals("06:45", prefs.getMorningNotificationTime())
    }

    @Test fun nextOccurrenceUsesSavedZoneAndRollsPastPrayersToTomorrow() {
        val settings = PrayerSettings("مکه", 21.4225, 39.8262, "Asia/Riyadh")
        val now = Instant.parse("2026-09-05T20:00:00Z").toEpochMilli()
        val tomorrow = Calendar.getInstance(TimeZone.getTimeZone(settings.zone)).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        AdhanPrayer.entries.forEach { prayer ->
            assertEquals(settings.times(tomorrow.time)[prayer.timeIndex].second?.time?.let { it / 1000 * 1000 },
                nextAdhanTime(settings, prayer, now))
        }
        assertFalse(AdhanPrayer.entries.any { it.timeIndex == 1 })
    }

    @Test fun futurePrayerUsesTodayAndInvalidLocationHasNoAlarm() {
        val settings = PrayerSettings("تهران", 35.6892, 51.389, "Asia/Tehran")
        val now = Instant.parse("2026-09-05T00:00:00Z").toEpochMilli()
        assertEquals(settings.times(Date(now))[2].second?.time?.let { it / 1000 * 1000 }, nextAdhanTime(settings, AdhanPrayer.DHUHR, now))
        assertNull(nextAdhanTime(PrayerSettings(), AdhanPrayer.FAJR, now))
    }
}

package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import com.example.prayer.AdhanPrayer
import com.example.prayer.PostPrayerReminderScheduler
import com.example.prayer.PostPrayerReminders
import com.example.prayer.PrayerSettings
import com.example.prayer.nextAdhanTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PostPrayerReminderTest {
    private val tehran = PrayerSettings("تهران", 35.6892, 51.3890, "Asia/Tehran")
    private val now = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli()
    private val minute = 60_000L

    @Test
    fun reminderIsTenMinutesAfterPrayerStarts() {
        assertEquals(40 * minute, PostPrayerReminders.delayMillis(AdhanPrayer.FAJR))
        assertEquals(40 * minute, PostPrayerReminders.delayMillis(AdhanPrayer.DHUHR))
        assertEquals(40 * minute, PostPrayerReminders.delayMillis(AdhanPrayer.ASR))
        assertEquals(15 * minute, PostPrayerReminders.delayMillis(AdhanPrayer.MAGHRIB))
        assertEquals(40 * minute, PostPrayerReminders.delayMillis(AdhanPrayer.ISHA))

        AdhanPrayer.entries.forEach { prayer ->
            val adhan = nextAdhanTime(tehran, prayer, now)!!
            val reminder = PostPrayerReminders.nextReminderTime(tehran, prayer, adhan - minute)!!
            assertEquals(prayer.name, PostPrayerReminders.delayMillis(prayer), reminder - adhan)
        }
    }

    @Test
    fun reminderStaysUpcomingAfterTheAdhanUntilItFires() {
        val adhan = nextAdhanTime(tehran, AdhanPrayer.ASR, now)!!
        val during = PostPrayerReminders.nextReminderTime(tehran, AdhanPrayer.ASR, adhan + 35 * minute)!!
        assertEquals(adhan + 40 * minute, during)

        val after = PostPrayerReminders.nextReminderTime(tehran, AdhanPrayer.ASR, adhan + 41 * minute)!!
        assertTrue("next reminder is tomorrow's", after - adhan in (23 * 60 * minute)..(25 * 60 * minute))
    }

    @Test
    fun nothingIsScheduledUntilGloballyEnabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", 0).edit().clear().commit()
        PreferenceRepository(context).setPrayerSettings(tehran)
        val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)

        PostPrayerReminderScheduler(context).reschedule()

        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun globalSwitchSchedulesAllPrayersAndCancelsAllWhenDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", 0).edit().clear().commit()
        val prefs = PreferenceRepository(context)
        prefs.setPrayerSettings(tehran)
        val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val scheduler = PostPrayerReminderScheduler(context)

        prefs.setPostPrayerReminderEnabled(true)
        scheduler.reschedule()
        assertEquals(5, alarms.scheduledAlarms.size)
        scheduler.reschedule()
        assertEquals(5, alarms.scheduledAlarms.size)

        prefs.setPostPrayerReminderEnabled(false)
        scheduler.reschedule()
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun existingPerPrayerOptInBecomesGlobalAndCanBeDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", 0).edit().clear()
            .putStringSet("post_prayer_reminder_prayers", setOf(AdhanPrayer.MAGHRIB.name)).commit()
        val prefs = PreferenceRepository(context)
        assertEquals(AdhanPrayer.entries.toSet(), prefs.getPostPrayerReminderPrayers())
        prefs.setPostPrayerReminderEnabled(false)
        assertTrue(prefs.getPostPrayerReminderPrayers().isEmpty())
    }
}

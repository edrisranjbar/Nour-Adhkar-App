package com.example.prayer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.notifications.AdhkarNotificationManager
import com.example.ui.language.text

/**
 * Timing for the "adhkar after prayer" reminder. It fires well after the adhan so the prayer is
 * finished: a gentle reminder 10 minutes after each selected prayer.
 */
object PostPrayerReminders {
    const val DEFAULT_DELAY_MINUTES = 10
    const val MAGHRIB_DELAY_MINUTES = 10
    /** An alarm may be delivered late (Doze, vendor battery managers); later than this it is dropped. */
    const val MAX_LATE_MILLIS = 30 * 60_000L

    fun delayMillis(prayer: AdhanPrayer): Long =
        (if (prayer == AdhanPrayer.MAGHRIB) MAGHRIB_DELAY_MINUTES else DEFAULT_DELAY_MINUTES) * 60_000L

    /** The next reminder strictly after [now], i.e. the next adhan whose reminder is still ahead. */
    fun nextReminderTime(settings: PrayerSettings, prayer: AdhanPrayer, now: Long): Long? {
        val delay = delayMillis(prayer)
        return nextAdhanTime(settings, prayer, now - delay)?.plus(delay)
    }
}

/**
 * Schedules one inexact alarm per selected prayer. It needs no exact-alarm permission: a reminder
 * that arrives a minute late is fine. Nothing is scheduled until the user selects a prayer.
 */
class PostPrayerReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun reschedule() {
        val prefs = PreferenceRepository(context)
        val enabled = prefs.getPostPrayerReminderPrayers()
        val settings = prefs.getPrayerSettings()
        val ready = settings.isValid() && NotificationManagerCompat.from(context).areNotificationsEnabled()
        val now = System.currentTimeMillis()
        AdhanPrayer.entries.forEach { prayer ->
            val pending = PendingIntent.getBroadcast(
                context, REQUEST_BASE + prayer.ordinal,
                Intent(context, PostPrayerReminderReceiver::class.java).setAction(ACTION)
                    .putExtra(EXTRA_PRAYER, prayer.name),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarms.cancel(pending)
            if (ready && prayer in enabled) {
                PostPrayerReminders.nextReminderTime(settings, prayer, now)?.let { time ->
                    try { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
                    catch (_: Exception) { /* Alarm restrictions must never crash the app. */ }
                }
            }
        }
    }

    companion object {
        const val ACTION = "ir.adhkar.app.POST_PRAYER_REMINDER"
        const val EXTRA_PRAYER = "prayer"
        const val CHANNEL_ID = "post_prayer_reminders"
        private const val REQUEST_BASE = 5200
    }
}

class PostPrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PostPrayerReminderScheduler.ACTION) return
        val prayer = AdhanPrayer.entries.firstOrNull {
            it.name == intent.getStringExtra(PostPrayerReminderScheduler.EXTRA_PRAYER)
        } ?: return
        val prefs = PreferenceRepository(context)
        val settings = prefs.getPrayerSettings()
        val now = System.currentTimeMillis()
        // Validate again so a canceled or stale broadcast cannot notify for a disabled prayer.
        val scheduled = PostPrayerReminders.nextReminderTime(
            settings, prayer, now - PostPrayerReminders.MAX_LATE_MILLIS - 1
        )
        if (prayer in prefs.getPostPrayerReminderPrayers() && scheduled != null &&
            now - scheduled in 0..PostPrayerReminders.MAX_LATE_MILLIS &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            show(context, prayer, prefs)
        }
        PostPrayerReminderScheduler(context).reschedule()
    }

    private fun show(context: Context, prayer: AdhanPrayer, prefs: PreferenceRepository) {
        val language = prefs.getAppLanguage()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    PostPrayerReminderScheduler.CHANNEL_ID,
                    language.text("اذکار نور - اذکار پس از نماز"),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = language.text("یادآوری اذکار مأثور پس از پایان نماز") }
            )
        }
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_BASE + prayer.ordinal,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AdhkarNotificationManager.EXTRA_OPEN_CATEGORY, "after_salah")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = language.text("اذکار پس از نماز") + " " + language.text(prayer.label)
        val text = language.text("نمازتان قبول باشد. چند لحظه برای اذکار پس از نماز.")
        val notification = NotificationCompat.Builder(context, PostPrayerReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(open)
            .addAction(0, language.text("شروع"), open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_BASE + prayer.ordinal, notification)
    }

    private companion object {
        const val NOTIFICATION_BASE = 1100
    }
}

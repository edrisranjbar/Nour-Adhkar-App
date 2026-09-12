package com.example.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.repository.PreferenceRepository

class AdhanScheduler(private val context: Context) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun hasAlarmPermission() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    fun reschedule() {
        val prefs = PreferenceRepository(context)
        val enabled = prefs.getAdhanPrayers()
        val ready = hasAlarmPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            prefs.getAdhanSound().isSelected && prefs.getPrayerSettings().isValid()
        val now = System.currentTimeMillis()
        AdhanPrayer.entries.forEach { prayer ->
            val pending = PendingIntent.getBroadcast(context, 5000 + prayer.ordinal,
                Intent(context, AdhanAlarmReceiver::class.java).setAction(ACTION)
                    .putExtra("prayer", prayer.name), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarms.cancel(pending)
            if (ready && prayer in enabled) {
                nextAdhanTime(prefs.getPrayerSettings(), prayer, now)?.let { time ->
                    // Permission may be revoked between checking and scheduling.
                    try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
                    catch (_: SecurityException) { }
                }
            }
        }
    }

    companion object { const val ACTION = "ir.adhkar.app.ADHAN_ALERT" }
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AdhanScheduler.ACTION) return
        val prayer = AdhanPrayer.entries.firstOrNull { it.name == intent.getStringExtra("prayer") } ?: return
        val prefs = PreferenceRepository(context)
        // Validate again so a canceled or stale broadcast cannot play a disabled prayer.
        val now = System.currentTimeMillis()
        val scheduled = nextAdhanTime(prefs.getPrayerSettings(), prayer, now - 10 * 60_000L - 1)
        if (prayer in prefs.getAdhanPrayers() && prefs.getAdhanSound().isSelected &&
            scheduled != null && now - scheduled in 0..(10 * 60_000L) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                ContextCompat.startForegroundService(context,
                    Intent(context, AdhanAlertService::class.java).putExtra("prayer", prayer.name))
            } catch (_: IllegalStateException) { /* System restrictions must not crash the receiver. */ }
              catch (_: SecurityException) { }
        }
        AdhanScheduler(context).reschedule()
    }
}

class AdhanRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            AdhanScheduler(context).reschedule()
        }
    }
}

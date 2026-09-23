package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.repository.PreferenceRepository
import com.example.quran.QuranKhatmRepository
import java.util.Calendar

class AdhkarNotificationManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = PreferenceRepository(context)
    private val khatmRepository = QuranKhatmRepository(context)

    fun scheduleReminders() {
        try {
            cancelAllReminders()
            if (!prefs.isNotificationsEnabled()) return
            scheduleNext("morning")
            scheduleNext("evening")
            if (prefs.isFridayKahfReminderEnabled()) scheduleNext("friday_kahf")
            val khatmGoal = khatmRepository.getGoal()
            if (khatmGoal?.reminderEnabled == true && !khatmGoal.paused && !khatmGoal.isComplete) {
                scheduleNext("quran_khatm")
            }
        } catch (_: Exception) {
            // Alarm restrictions must never crash app startup.
        }
    }

    fun scheduleNext(type: String) {
        try {
            if (!prefs.isNotificationsEnabled()) return
            val (time, days, requestCode) = when (type) {
                "morning" -> Triple(prefs.getMorningNotificationTime(), ALL_DAYS, REQUEST_MORNING)
                "evening" -> Triple(prefs.getEveningNotificationTime(), ALL_DAYS, REQUEST_EVENING)
                "friday_kahf" -> {
                    if (!prefs.isFridayKahfReminderEnabled()) return
                    Triple(prefs.getFridayKahfReminderTime(), setOf(Calendar.FRIDAY), REQUEST_FRIDAY_KAHF)
                }
                "quran_khatm" -> {
                    val goal = khatmRepository.getGoal() ?: return
                    if (!goal.reminderEnabled || goal.paused || goal.isComplete) return
                    Triple(goal.reminderTime, ALL_DAYS, REQUEST_QURAN_KHATM)
                }
                else -> return
            }
            if (days.isEmpty()) {
                cancelReminder(requestCode)
                return
            }
            val pendingIntent = reminderPendingIntent(type, requestCode)
            alarmManager.cancel(pendingIntent)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(time, days), pendingIntent)
        } catch (_: Exception) {
            // Safe fallback for vendor-specific alarm restrictions.
        }
    }

    fun scheduleSnooze(type: String, delayMinutes: Int = 60) {
        try {
            val requestCode = when (type) {
                "morning" -> REQUEST_SNOOZE_MORNING
                "evening" -> REQUEST_SNOOZE_EVENING
                "friday_kahf" -> REQUEST_SNOOZE_FRIDAY
                "quran_khatm" -> REQUEST_SNOOZE_QURAN_KHATM
                else -> return
            }
            val pendingIntent = reminderPendingIntent(type, requestCode)
            alarmManager.cancel(pendingIntent)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMinutes * 60_000L,
                pendingIntent
            )
        } catch (_: Exception) {
            // Snoozing is optional; failure must not crash the receiver.
        }
    }

    fun cancelAllReminders() {
        listOf(101, 102, 103, 104, 201, 202, 203, 204).forEach(::cancelReminder)
    }

    private fun nextTrigger(time: String, allowedDays: Set<Int>): Long {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val now = Calendar.getInstance()
        repeat(8) { dayOffset ->
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in allowedDays && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        return now.timeInMillis + AlarmManager.INTERVAL_DAY
    }

    private fun reminderPendingIntent(type: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelReminder(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_SHOW_REMINDER }
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let(alarmManager::cancel)
    }

    fun triggerTestNotification() {
        context.sendBroadcast(Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, "test")
        })
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "ir.adhkar.app.action.SHOW_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "ir.adhkar.app.action.SNOOZE_REMINDER"
        const val EXTRA_REMINDER_TYPE = "REMINDER_TYPE"
        const val EXTRA_OPEN_CATEGORY = "OPEN_CATEGORY"
        const val EXTRA_OPEN_QURAN_PAGE = "OPEN_QURAN_PAGE"
        private const val REQUEST_MORNING = 101
        private const val REQUEST_EVENING = 102
        private const val REQUEST_FRIDAY_KAHF = 103
        private const val REQUEST_QURAN_KHATM = 104
        private const val REQUEST_SNOOZE_MORNING = 201
        private const val REQUEST_SNOOZE_EVENING = 202
        private const val REQUEST_SNOOZE_FRIDAY = 203
        private const val REQUEST_SNOOZE_QURAN_KHATM = 204
        private val ALL_DAYS = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
    }
}

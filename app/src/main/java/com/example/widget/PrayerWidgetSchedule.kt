package com.example.widget

import com.example.prayer.PrayerSettings
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

internal data class PrayerWidgetSchedule(
    val today: List<Pair<String, Date?>>,
    val next: Pair<String, Date?>?,
    val tomorrow: Boolean,
    val refreshAt: Long
) {
    companion object {
        fun calculate(settings: PrayerSettings, now: Date): PrayerWidgetSchedule? {
            if (!settings.isValid()) return null
            return runCatching {
                val midnight = Calendar.getInstance(TimeZone.getTimeZone(settings.zone)).apply {
                    time = now
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                val today = settings.times(now)
                fun List<Pair<String, Date?>>.upcoming() = firstOrNull {
                    it.first != "طلوع" && (it.second?.time?.div(60_000) ?: Long.MIN_VALUE) > now.time / 60_000
                }
                val todayNext = today.upcoming()
                val next = todayNext ?: settings.times(midnight).upcoming()
                PrayerWidgetSchedule(today, next, todayNext == null,
                    minOf(midnight.time, next?.second?.time?.let { it / 60_000 * 60_000 } ?: midnight.time))
            }.getOrNull()
        }
    }
}

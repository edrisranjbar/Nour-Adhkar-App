package com.example.qaza

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** The prayers that can be owed. Witr is optional because traditions count it differently. */
enum class QazaPrayer(val id: String, val title: String, val optional: Boolean = false) {
    FAJR("fajr", "صبح"),
    DHUHR("dhuhr", "ظهر"),
    ASR("asr", "عصر"),
    MAGHRIB("maghrib", "مغرب"),
    ISHA("isha", "عشاء"),
    WITR("witr", "وتر", optional = true);

    companion object {
        fun fromId(id: String): QazaPrayer? = values().firstOrNull { it.id == id }
    }
}

/** Sanity limits that keep every number editable but bounded. */
object QazaLimits {
    const val MAX_PER_PRAYER = 99_999
    const val MAX_FAST_COUNT = 9_999
    const val MAX_DAILY_SETS = 20
    const val HISTORY_LIMIT = 200
}

/** A stretch of time the user did not pray, entered by age or as a length of time. */
data class QazaPeriod(val years: Int = 0, val months: Int = 0, val days: Int = 0)

/**
 * Pure helpers for the setup estimate and the finish date. The estimate is only a suggestion the
 * user edits before saving; it carries no religious ruling.
 */
object QazaCalculator {
    const val DAYS_PER_YEAR = 365
    const val DAYS_PER_MONTH = 30

    fun totalDays(period: QazaPeriod): Int {
        val total = period.years.coerceAtLeast(0).toLong() * DAYS_PER_YEAR +
            period.months.coerceAtLeast(0).toLong() * DAYS_PER_MONTH +
            period.days.coerceAtLeast(0).toLong()
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /**
     * One prayer per day for the period, minus days the person was not required to pray.
     * Witr is included only when asked for.
     */
    fun estimate(period: QazaPeriod, exemptDays: Int, includeWitr: Boolean): Map<QazaPrayer, Int> {
        val days = (totalDays(period) - exemptDays.coerceAtLeast(0)).coerceIn(0, QazaLimits.MAX_PER_PRAYER)
        return QazaPrayer.values().filter { includeWitr || !it.optional }.associateWith { days }
    }

    /**
     * The day everything would be made up at [perDay] a day, or null when there is no goal.
     * With nothing remaining the answer is today.
     */
    fun completionDate(
        remaining: Int,
        perDay: Int,
        today: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Date? {
        if (perDay <= 0) return null
        if (remaining <= 0) return Date(today)
        val days = (remaining + perDay - 1) / perDay
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.time
    }
}

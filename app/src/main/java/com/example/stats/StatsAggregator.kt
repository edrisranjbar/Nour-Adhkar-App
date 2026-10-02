package com.example.stats

import com.example.calendar.Jalali

/**
 * The user's own activity, per local calendar day (Julian Day Numbers, see [com.example.calendar.Gregorian]).
 * Built from data the app already keeps: tasbih sessions, daily checklist days and active-day keys.
 */
data class StatsInput(
    val todayJdn: Int,
    val tasbihByDay: Map<Int, Int>,
    val checklistByDay: Map<Int, Int>,
    val activeDays: Set<Int>
)

enum class StatsMetric { ACTIVE_DAYS, TASBIH, CHECKLIST }

data class DayPoint(val jdn: Int, val tasbih: Int, val checklist: Int, val active: Boolean)

data class MonthPoint(
    val year: Int,
    val month: Int,
    val length: Int,
    val activeDays: Int,
    val tasbih: Int,
    val checklist: Int
) {
    fun value(metric: StatsMetric): Int = when (metric) {
        StatsMetric.ACTIVE_DAYS -> activeDays
        StatsMetric.TASBIH -> tasbih
        StatsMetric.CHECKLIST -> checklist
    }
}

data class CumulativePoint(val jdn: Int, val total: Int)

data class StatsSummary(val activeThisMonth: Int, val totalTasbih: Int, val totalActiveDays: Int, val firstDay: Int?)

/** Pure aggregation for the statistics page; no Android types, so it is unit-tested directly. */
object StatsAggregator {

    /** [days] consecutive days ending at [endJdn], oldest first. */
    fun daily(input: StatsInput, endJdn: Int, days: Int = 30): List<DayPoint> =
        (endJdn - days + 1..endJdn).map { jdn ->
            DayPoint(
                jdn = jdn,
                tasbih = input.tasbihByDay[jdn] ?: 0,
                checklist = input.checklistByDay[jdn] ?: 0,
                active = jdn in input.activeDays
            )
        }

    /** The last [months] Jalali months ending with the current one, oldest first. */
    fun monthly(input: StatsInput, months: Int = 12): List<MonthPoint> {
        val today = Jalali.fromJdn(input.todayJdn)
        return (months - 1 downTo 0).map { back ->
            val (year, month) = Jalali.shiftMonth(today.year, today.month, -back)
            val first = Jalali.toJdn(year, month, 1)
            val length = Jalali.monthLength(year, month)
            val range = first until first + length
            MonthPoint(
                year = year,
                month = month,
                length = length,
                activeDays = range.count { it in input.activeDays },
                tasbih = range.sumOf { input.tasbihByDay[it] ?: 0 },
                checklist = range.sumOf { input.checklistByDay[it] ?: 0 }
            )
        }
    }

    /** First day with any recorded activity, or null when there is none. */
    fun firstDay(input: StatsInput): Int? =
        listOfNotNull(
            input.tasbihByDay.filterValues { it > 0 }.keys.minOrNull(),
            input.checklistByDay.filterValues { it > 0 }.keys.minOrNull(),
            input.activeDays.minOrNull()
        ).minOrNull()

    /**
     * Running total of [metric] for every day from max(first activity, today - [rangeDays] + 1) to
     * today, oldest first. With a range, the total still counts everything before the range starts,
     * so the curve shows the same lifetime total at today whatever range is chosen.
     */
    fun cumulative(input: StatsInput, metric: StatsMetric, rangeDays: Int? = null): List<CumulativePoint> {
        val first = firstDay(input) ?: return emptyList()
        val start = if (rangeDays == null) first else maxOf(first, input.todayJdn - rangeDays + 1)
        fun valueOn(jdn: Int): Int = when (metric) {
            StatsMetric.ACTIVE_DAYS -> if (jdn in input.activeDays) 1 else 0
            StatsMetric.TASBIH -> input.tasbihByDay[jdn] ?: 0
            StatsMetric.CHECKLIST -> input.checklistByDay[jdn] ?: 0
        }
        var total = (first until start).sumOf(::valueOn)
        return (start..input.todayJdn).map { jdn ->
            total += valueOn(jdn)
            CumulativePoint(jdn, total)
        }
    }

    fun summary(input: StatsInput): StatsSummary {
        val today = Jalali.fromJdn(input.todayJdn)
        val monthStart = Jalali.toJdn(today.year, today.month, 1)
        return StatsSummary(
            activeThisMonth = (monthStart..input.todayJdn).count { it in input.activeDays },
            totalTasbih = input.tasbihByDay.values.sum(),
            totalActiveDays = input.activeDays.count { it <= input.todayJdn },
            firstDay = firstDay(input)
        )
    }
}

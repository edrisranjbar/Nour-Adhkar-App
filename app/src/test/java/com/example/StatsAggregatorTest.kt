package com.example

import com.example.calendar.Jalali
import com.example.stats.StatsAggregator
import com.example.stats.StatsInput
import com.example.stats.StatsMetric
import com.example.ui.components.niceMax
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsAggregatorTest {
    // Today is 10 Mehr 1405.
    private val today = Jalali.toJdn(1405, 7, 10)

    private fun input(
        tasbih: Map<Int, Int> = emptyMap(),
        checklist: Map<Int, Int> = emptyMap(),
        active: Set<Int> = emptySet()
    ) = StatsInput(today, tasbih, checklist, active)

    @Test
    fun `daily window ends today, oldest first, and fills gaps with zeros`() {
        val days = StatsAggregator.daily(input(tasbih = mapOf(today to 33, today - 2 to 100), active = setOf(today)), today, 30)
        assertEquals(30, days.size)
        assertEquals(today - 29, days.first().jdn)
        assertEquals(today, days.last().jdn)
        assertEquals(33, days.last().tasbih)
        assertTrue(days.last().active)
        assertEquals(0, days[days.size - 2].tasbih)
        assertEquals(100, days[days.size - 3].tasbih)
    }

    @Test
    fun `monthly uses Jalali month lengths and counts active days per month`() {
        val mehrFirst = Jalali.toJdn(1405, 7, 1)
        val shahrivarLast = Jalali.toJdn(1405, 6, 31)
        val esfand1404 = Jalali.toJdn(1404, 12, 29)
        val months = StatsAggregator.monthly(
            input(active = setOf(mehrFirst, mehrFirst + 1, shahrivarLast, esfand1404), tasbih = mapOf(shahrivarLast to 50))
        )
        assertEquals(12, months.size)
        val mehr = months.last()
        assertEquals(1405 to 7, mehr.year to mehr.month)
        assertEquals(30, mehr.length)
        assertEquals(2, mehr.activeDays)
        val shahrivar = months[months.size - 2]
        assertEquals(31, shahrivar.length)
        assertEquals(1, shahrivar.activeDays)
        assertEquals(50, shahrivar.value(StatsMetric.TASBIH))
        // 1404 is not a leap year: Esfand has 29 days, and its last day counts.
        val esfand = months.first { it.year == 1404 && it.month == 12 }
        assertEquals(29, esfand.length)
        assertEquals(1, esfand.activeDays)
    }

    @Test
    fun `cumulative totals rise and a range keeps the lifetime total at today`() {
        val data = input(tasbih = mapOf(today - 200 to 10, today - 5 to 20, today to 30))
        val all = StatsAggregator.cumulative(data, StatsMetric.TASBIH)
        assertEquals(today - 200, all.first().jdn)
        assertEquals(10, all.first().total)
        assertEquals(60, all.last().total)
        assertTrue(all.zipWithNext().all { (a, b) -> b.total >= a.total })

        val lastMonth = StatsAggregator.cumulative(data, StatsMetric.TASBIH, rangeDays = 30)
        assertEquals(30, lastMonth.size)
        assertEquals(10, lastMonth.first().total) // the day-200 tasbih is carried in
        assertEquals(60, lastMonth.last().total)
    }

    @Test
    fun `summary and empty data`() {
        val empty = input()
        assertNull(StatsAggregator.firstDay(empty))
        assertTrue(StatsAggregator.cumulative(empty, StatsMetric.ACTIVE_DAYS).isEmpty())
        val summary = StatsAggregator.summary(input(tasbih = mapOf(today to 7, today - 40 to 3), active = setOf(today, today - 1, today - 40)))
        assertEquals(2, summary.activeThisMonth)
        assertEquals(10, summary.totalTasbih)
        assertEquals(3, summary.totalActiveDays)
        assertEquals(today - 40, summary.firstDay)
    }

    @Test
    fun `axis maximum rounds up to a readable number`() {
        assertEquals(1f, niceMax(0f))
        assertEquals(10f, niceMax(7f))
        assertEquals(250f, niceMax(201f))
        assertEquals(500f, niceMax(330f))
        assertEquals(1000f, niceMax(1000f))
    }
}

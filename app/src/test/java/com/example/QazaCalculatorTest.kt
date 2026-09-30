package com.example

import com.example.qaza.QazaCalculator
import com.example.qaza.QazaLimits
import com.example.qaza.QazaPeriod
import com.example.qaza.QazaPrayer
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QazaCalculatorTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val today = 1_700_000_000_000L
    private val oneDay = 24L * 60 * 60 * 1000

    @Test fun estimateCountsOnePrayerPerDayForTheFiveRequiredPrayers() {
        val estimate = QazaCalculator.estimate(QazaPeriod(years = 2), exemptDays = 0, includeWitr = false)
        assertEquals(5, estimate.size)
        assertFalse(estimate.containsKey(QazaPrayer.WITR))
        assertTrue(estimate.values.all { it == 2 * 365 })
    }

    @Test fun estimateIncludesWitrOnlyWhenAsked() {
        val estimate = QazaCalculator.estimate(QazaPeriod(days = 10), exemptDays = 0, includeWitr = true)
        assertEquals(6, estimate.size)
        assertEquals(10, estimate.getValue(QazaPrayer.WITR))
    }

    @Test fun exemptDaysAreSubtractedAndNeverGoBelowZero() {
        val period = QazaPeriod(months = 2)
        assertEquals(40, QazaCalculator.estimate(period, exemptDays = 20, includeWitr = false).getValue(QazaPrayer.FAJR))
        assertEquals(0, QazaCalculator.estimate(period, exemptDays = 500, includeWitr = false).getValue(QazaPrayer.ISHA))
        assertEquals(60, QazaCalculator.estimate(period, exemptDays = -5, includeWitr = false).getValue(QazaPrayer.ASR))
    }

    @Test fun totalDaysCombinesYearsMonthsAndDays() {
        assertEquals(365 + 60 + 7, QazaCalculator.totalDays(QazaPeriod(years = 1, months = 2, days = 7)))
        assertEquals(0, QazaCalculator.totalDays(QazaPeriod(years = -3, months = -1, days = -9)))
    }

    @Test fun periodBetweenAgesUsesTheDifferenceInYears() {
        assertEquals(QazaPeriod(years = 7), QazaCalculator.periodBetweenAges(12, 19))
        assertEquals(QazaPeriod(years = 0), QazaCalculator.periodBetweenAges(20, 15))
    }

    @Test fun hugePeriodsAreCappedPerPrayer() {
        val estimate = QazaCalculator.estimate(QazaPeriod(years = 500), exemptDays = 0, includeWitr = false)
        assertTrue(estimate.values.all { it == QazaLimits.MAX_PER_PRAYER })
    }

    @Test fun completionDateRoundsUpToWholeDays() {
        val date = QazaCalculator.completionDate(remaining = 10, perDay = 3, today = today, timeZone = utc)
        assertEquals(today + 4 * oneDay, date!!.time)
    }

    @Test fun completionDateIsTodayWhenNothingRemains() {
        assertEquals(today, QazaCalculator.completionDate(0, 2, today, utc)!!.time)
    }

    @Test fun completionDateIsNullWithoutADailyGoal() {
        assertNull(QazaCalculator.completionDate(10, 0, today, utc))
        assertNull(QazaCalculator.completionDate(10, -1, today, utc))
    }
}

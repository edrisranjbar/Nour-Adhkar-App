package com.example

import com.example.streak.StreakEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class StreakEngineTest {
    // Day 9 is a Saturday (first day of a week); day 15 is the following Friday.
    private val saturday = 9L

    private fun compute(today: Long, vararg activeDays: Long) =
        StreakEngine.compute(today) { it in activeDays.toSet() }

    private fun days(from: Long, to: Long) = (from..to).toList().toLongArray()

    @Test
    fun weeksRunSaturdayThroughFriday() {
        assertEquals(StreakEngine.weekOf(saturday), StreakEngine.weekOf(saturday + 6))
        assertEquals(StreakEngine.weekOf(saturday) + 1, StreakEngine.weekOf(saturday + 7))
        assertEquals(StreakEngine.weekOf(saturday) - 1, StreakEngine.weekOf(saturday - 1))
    }

    @Test
    fun dayNumberUsesTheLocalCalendarDay() {
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        // 2026-03-01T22:00:00Z is already 01:30 on March 2 in Tehran (UTC+3:30).
        val millis = 1_772_402_400_000L
        val utcDay = StreakEngine.dayNumber(millis, TimeZone.getTimeZone("UTC"))
        assertEquals(utcDay + 1, StreakEngine.dayNumber(millis, tehran))
    }

    @Test
    fun plainStreakCountsConsecutiveActiveDays() {
        val state = compute(20, *days(17, 20))
        assertEquals(4, state.count)
        assertTrue(state.todayActive)
        assertTrue(state.frozenDays.isEmpty())
        assertNull(state.pendingFreezeDay)
    }

    @Test
    fun streakEndingYesterdayStaysAliveWhileTodayIsPending() {
        val state = compute(20, *days(17, 19))
        assertEquals(3, state.count)
        assertFalse(state.todayActive)
        assertNull(state.pendingFreezeDay)
    }

    @Test
    fun oneMissedDayIsCoveredAndAddsNothingToTheCount() {
        // Active Sat..Mon, missed Tue, active Wed.
        val state = compute(saturday + 4, *days(saturday, saturday + 2), saturday + 4)
        assertEquals(4, state.count)
        assertEquals(setOf(saturday + 3), state.frozenDays)
    }

    @Test
    fun missedYesterdayIsPendingUntilTheUserActsToday() {
        val state = compute(saturday + 4, *days(saturday, saturday + 2))
        assertEquals(3, state.count)
        assertFalse(state.todayActive)
        assertEquals(saturday + 3, state.pendingFreezeDay)
        assertTrue(state.frozenDays.isEmpty())
    }

    @Test
    fun pendingFreezeBecomesRealOnceTodayIsActive() {
        val pending = compute(saturday + 4, *days(saturday, saturday + 2))
        val acted = compute(saturday + 4, *days(saturday, saturday + 2), saturday + 4)
        assertEquals(saturday + 3, pending.pendingFreezeDay)
        assertEquals(setOf(saturday + 3), acted.frozenDays)
        assertNull(acted.pendingFreezeDay)
        assertEquals(pending.count + 1, acted.count)
    }

    @Test
    fun twoMissedDaysInARowBreakTheStreak() {
        val state = compute(saturday + 5, *days(saturday, saturday + 2), saturday + 5)
        assertEquals(1, state.count)
        assertTrue(state.frozenDays.isEmpty())
    }

    @Test
    fun missedYesterdayAfterTwoMissedDaysIsNotPending() {
        val state = compute(saturday + 5, *days(saturday, saturday + 2))
        assertEquals(0, state.count)
        assertNull(state.pendingFreezeDay)
    }

    @Test
    fun aStreakMustExistBeforeADayIsCovered() {
        // Only two active days before the gap.
        val state = compute(saturday + 3, saturday, saturday + 1, saturday + 3)
        assertEquals(1, state.count)
        assertTrue(state.frozenDays.isEmpty())
    }

    @Test
    fun aSingleGapInAWeekIsCovered() {
        // Sat..Mon active, Tue missed, Wed..Fri active.
        val state = compute(saturday + 6, *days(saturday, saturday + 2), *days(saturday + 4, saturday + 6))
        assertEquals(setOf(saturday + 3), state.frozenDays)
        assertEquals(6, state.count)
    }

    @Test
    fun aSecondGapInTheSameWeekBreaksTheStreak() {
        // Week A is days 9..15. Days 5..8 are active, so day 9 (the week's first day) is covered;
        // a second gap on day 13 in the same week is not.
        val state = compute(14, 5, 6, 7, 8, 10, 11, 12, 14)
        assertEquals(1, state.count)
        assertTrue(state.frozenDays.isEmpty())
    }

    @Test
    fun aNewWeekGetsANewFreeze() {
        // Gap on day 13 (Wed of week A) covered; gap on day 20 (Wed of week B) covered too.
        val active = (9L..12L) + (14L..19L) + (21L..22L)
        val state = StreakEngine.compute(22) { it in active.toSet() }
        assertEquals(setOf(13L, 20L), state.frozenDays)
        assertEquals(active.size, state.count)
    }

    @Test
    fun emptyHistoryHasNoStreak() {
        val state = compute(20)
        assertEquals(0, state.count)
        assertFalse(state.todayActive)
        assertEquals(state, com.example.streak.StreakState.Empty)
    }
}

package com.example

import com.example.ui.components.WeekDayState
import com.example.ui.components.riveState
import com.example.ui.screens.DayActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakWeekStateTest {
    private fun day(
        active: Boolean = false, today: Boolean = false, frozen: Boolean = false, pending: Boolean = false
    ) = DayActivity("ش", active, today, 0L, frozen, pending)

    @Test
    fun pastDaysMapToDoneFrozenPendingOrMissed() {
        assertEquals(WeekDayState.DONE, day(active = true).riveState(todayFilled = false))
        assertEquals(WeekDayState.FROZEN, day(frozen = true).riveState(todayFilled = false))
        assertEquals(WeekDayState.PENDING, day(pending = true).riveState(todayFilled = false))
        assertEquals(WeekDayState.MISSED, day().riveState(todayFilled = false))
    }

    @Test
    fun todayIsDoneOnlyAfterItIsActiveAndTheFillIsDue() {
        assertEquals(WeekDayState.TODAY_EMPTY, day(today = true, active = true).riveState(todayFilled = false))
        assertEquals(WeekDayState.TODAY_DONE, day(today = true, active = true).riveState(todayFilled = true))
        // Opened from Home before any activity: the fill never happens.
        assertEquals(WeekDayState.TODAY_EMPTY, day(today = true).riveState(todayFilled = true))
    }

    @Test
    fun stateNumbersMatchTheRiveFileContract() {
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5),
            listOf(
                WeekDayState.MISSED, WeekDayState.DONE, WeekDayState.FROZEN,
                WeekDayState.PENDING, WeekDayState.TODAY_EMPTY, WeekDayState.TODAY_DONE
            )
        )
    }
}

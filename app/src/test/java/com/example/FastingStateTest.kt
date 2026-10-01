package com.example

import com.example.qaza.FastingState
import com.example.qaza.QazaLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FastingStateTest {
    @Test fun startsEmpty() {
        val state = FastingState()
        assertFalse(state.hasData)
        assertEquals(0, state.remaining)
    }

    @Test fun settingTheTotalSetsWhatIsOwed() {
        val state = FastingState().setTotal(8)
        assertTrue(state.hasData)
        assertEquals(8, state.owed)
        assertEquals(8, state.remaining)
        assertEquals(0, state.madeUp)
    }

    @Test fun settingTheTotalAgainReplacesItInsteadOfAdding() {
        assertEquals(9, FastingState().setTotal(5).setTotal(9).owed)
    }

    @Test fun raisingTheTotalKeepsTheProgressAlreadyMade() {
        val state = FastingState().setTotal(10).markMadeUp(1).markMadeUp(2).setTotal(20)
        assertEquals(20, state.owed)
        assertEquals(2, state.madeUp)
        assertEquals(18, state.remaining)
    }

    @Test fun loweringTheTotalBelowTheProgressTakesTheExtraDaysBack() {
        var state = FastingState().setTotal(10)
        (1L..4L).forEach { state = state.markMadeUp(it) }
        state = state.setTotal(2)
        assertEquals(2, state.owed)
        assertEquals(2, state.madeUp)
        assertEquals(0, state.remaining)
        assertEquals(listOf(1L, 2L), state.recentDates)
    }

    @Test fun aTotalOfZeroClearsEverything() {
        val state = FastingState().setTotal(5).markMadeUp(1).setTotal(0)
        assertFalse(state.hasData)
        assertEquals(0, state.madeUp)
        assertTrue(state.recentDates.isEmpty())
    }

    @Test fun markingMadeUpReducesRemainingAndRecordsTheDate() {
        val state = FastingState().setTotal(3).markMadeUp(100).markMadeUp(200)
        assertEquals(1, state.remaining)
        assertEquals(2, state.madeUp)
        assertEquals(listOf(100L, 200L), state.recentDates)
    }

    @Test fun markingMadeUpNeverGoesBelowZero() {
        val done = FastingState().setTotal(1).markMadeUp(1)
        assertEquals(0, done.remaining)
        assertSame(done, done.markMadeUp(2))
        val untouched = FastingState()
        assertEquals(untouched, untouched.markMadeUp(5))
    }

    @Test fun undoTakesBackTheLatestDayOnly() {
        var state = FastingState().setTotal(3).markMadeUp(10).markMadeUp(20)
        state = state.undoMadeUp()
        assertEquals(2, state.remaining)
        assertEquals(listOf(10L), state.recentDates)
        state = state.undoMadeUp()
        assertEquals(3, state.remaining)
        assertTrue(state.recentDates.isEmpty())
        assertSame(state, state.undoMadeUp())
    }

    @Test fun theTotalIsNeverNegativeAndIsCapped() {
        assertEquals(0, FastingState().setTotal(3).setTotal(-5).owed)
        assertEquals(QazaLimits.MAX_FASTS, FastingState().setTotal(10_000_000).owed)
        assertEquals(QazaLimits.MAX_FASTS, FastingState().setTotal(Int.MAX_VALUE).remaining)
    }

    @Test fun onlyTheLatestDatesAreKept() {
        var state = FastingState().setTotal(QazaLimits.RECENT_DATES + 10)
        repeat(QazaLimits.RECENT_DATES + 5) { state = state.markMadeUp(it.toLong()) }
        assertEquals(QazaLimits.RECENT_DATES, state.recentDates.size)
        assertEquals((QazaLimits.RECENT_DATES + 4).toLong(), state.recentDates.last())
        assertEquals(QazaLimits.RECENT_DATES + 5, state.madeUp)
    }
}

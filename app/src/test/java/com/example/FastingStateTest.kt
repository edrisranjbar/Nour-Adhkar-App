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

    @Test fun addingMissedFastsIncreasesWhatIsOwed() {
        val state = FastingState().addMissed(5).addMissed(3)
        assertTrue(state.hasData)
        assertEquals(8, state.owed)
        assertEquals(8, state.remaining)
        assertEquals(0, state.madeUp)
    }

    @Test fun addingZeroOrNegativeChangesNothing() {
        val state = FastingState().addMissed(4)
        assertSame(state, state.addMissed(0))
        assertSame(state, state.addMissed(-3))
    }

    @Test fun markingMadeUpReducesRemainingAndRecordsTheDate() {
        val state = FastingState().addMissed(3).markMadeUp(100).markMadeUp(200)
        assertEquals(1, state.remaining)
        assertEquals(2, state.madeUp)
        assertEquals(listOf(100L, 200L), state.recentDates)
    }

    @Test fun markingMadeUpNeverGoesBelowZero() {
        val done = FastingState().addMissed(1).markMadeUp(1)
        assertEquals(0, done.remaining)
        assertSame(done, done.markMadeUp(2))
        val untouched = FastingState()
        assertEquals(untouched, untouched.markMadeUp(5))
    }

    @Test fun undoTakesBackTheLatestDayOnly() {
        var state = FastingState().addMissed(3).markMadeUp(10).markMadeUp(20)
        state = state.undoMadeUp()
        assertEquals(2, state.remaining)
        assertEquals(listOf(10L), state.recentDates)
        state = state.undoMadeUp()
        assertEquals(3, state.remaining)
        assertTrue(state.recentDates.isEmpty())
        assertSame(state, state.undoMadeUp())
    }

    @Test fun settingRemainingKeepsTheProgressAlreadyMade() {
        var state = FastingState().addMissed(10).markMadeUp(1).markMadeUp(2)
        state = state.setRemaining(20)
        assertEquals(20, state.remaining)
        assertEquals(2, state.madeUp)
        assertEquals(22, state.owed)
        state = state.setRemaining(0)
        assertEquals(0, state.remaining)
        assertEquals(2, state.owed)
    }

    @Test fun remainingIsNeverNegativeAndIsCapped() {
        assertEquals(0, FastingState().addMissed(3).setRemaining(-5).remaining)
        assertEquals(QazaLimits.MAX_FASTS, FastingState().setRemaining(10_000_000).remaining)
        assertEquals(QazaLimits.MAX_FASTS, FastingState().addMissed(Int.MAX_VALUE).remaining)
        assertEquals(QazaLimits.MAX_FASTS, FastingState().addMissed(5).addMissed(Int.MAX_VALUE).remaining)
    }

    @Test fun onlyTheLatestDatesAreKept() {
        var state = FastingState().addMissed(QazaLimits.RECENT_DATES + 10)
        repeat(QazaLimits.RECENT_DATES + 5) { state = state.markMadeUp(it.toLong()) }
        assertEquals(QazaLimits.RECENT_DATES, state.recentDates.size)
        assertEquals((QazaLimits.RECENT_DATES + 4).toLong(), state.recentDates.last())
        assertEquals(QazaLimits.RECENT_DATES + 5, state.madeUp)
    }
}

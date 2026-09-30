package com.example

import com.example.qaza.FastEntry
import com.example.qaza.QazaActionType
import com.example.qaza.QazaLimits
import com.example.qaza.QazaPrayer
import com.example.qaza.QazaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QazaStateTest {
    @Test fun addMissedIncreasesWhatIsOwed() {
        val state = QazaState().addMissed(QazaPrayer.FAJR, 1, now = 1).addMissed(QazaPrayer.FAJR, 4, now = 2)
        assertEquals(5, state.debt(QazaPrayer.FAJR).remaining)
        assertEquals(5, state.debt(QazaPrayer.FAJR).owed)
        assertEquals(listOf(QazaActionType.MISSED, QazaActionType.MISSED), state.history.map { it.type })
    }

    @Test fun madeUpReducesRemainingButNeverBelowZero() {
        var state = QazaState().setRemaining(QazaPrayer.ASR, 2, now = 1)
        state = state.madeUp(QazaPrayer.ASR, 1, now = 2)
        assertEquals(1, state.debt(QazaPrayer.ASR).remaining)
        state = state.madeUp(QazaPrayer.ASR, 5, now = 3)
        assertEquals(0, state.debt(QazaPrayer.ASR).remaining)
        val unchanged = state.madeUp(QazaPrayer.ASR, 1, now = 4)
        assertSame(state, unchanged)
        assertEquals(2, state.debt(QazaPrayer.ASR).madeUp)
    }

    @Test fun madeUpWithNothingOwedIsIgnored() {
        val state = QazaState()
        assertSame(state, state.madeUp(QazaPrayer.DHUHR, 1, now = 1))
        assertTrue(state.history.isEmpty())
    }

    @Test fun settingRemainingKeepsProgressAlreadyMade() {
        var state = QazaState().setRemaining(QazaPrayer.ISHA, 10, now = 1).madeUp(QazaPrayer.ISHA, 4, now = 2)
        state = state.setRemaining(QazaPrayer.ISHA, 20, now = 3)
        assertEquals(20, state.debt(QazaPrayer.ISHA).remaining)
        assertEquals(4, state.debt(QazaPrayer.ISHA).madeUp)
        assertEquals(24, state.debt(QazaPrayer.ISHA).owed)
    }

    @Test fun countersAreCappedAndNeverNegative() {
        val big = QazaState().setRemaining(QazaPrayer.FAJR, 10_000_000, now = 1)
        assertEquals(QazaLimits.MAX_PER_PRAYER, big.debt(QazaPrayer.FAJR).remaining)
        val negative = QazaState().setRemaining(QazaPrayer.FAJR, -4, now = 1)
        assertEquals(0, negative.debt(QazaPrayer.FAJR).remaining)
        assertTrue(negative.history.isEmpty())
    }

    @Test fun undoRestoresTheLastActionExactly() {
        var state = QazaState().setRemaining(QazaPrayer.MAGHRIB, 5, now = 1).madeUp(QazaPrayer.MAGHRIB, 2, now = 2)
        state = state.undoLast()
        assertEquals(5, state.debt(QazaPrayer.MAGHRIB).remaining)
        assertEquals(0, state.debt(QazaPrayer.MAGHRIB).madeUp)
        assertEquals(1, state.history.size)
        state = state.undoLast().undoLast()
        assertEquals(0, state.debt(QazaPrayer.MAGHRIB).remaining)
        assertTrue(state.history.isEmpty())
    }

    @Test fun undoOfASetupRestoresEveryPrayer() {
        val start = QazaState().setRemaining(QazaPrayer.FAJR, 3, now = 1)
        val estimate = QazaPrayer.values().filterNot { it.optional }.associateWith { 100 }
        val afterSetup = start.applyEstimate(estimate, now = 2)
        assertEquals(100, afterSetup.debt(QazaPrayer.DHUHR).remaining)
        assertEquals(QazaActionType.SETUP, afterSetup.history.last().type)
        val undone = afterSetup.undoLast()
        assertEquals(3, undone.debt(QazaPrayer.FAJR).remaining)
        assertEquals(0, undone.debt(QazaPrayer.DHUHR).remaining)
    }

    @Test fun setupWithWitrTurnsWitrOn() {
        val state = QazaState().applyEstimate(mapOf(QazaPrayer.WITR to 30, QazaPrayer.FAJR to 30), now = 1)
        assertTrue(state.witrEnabled)
        assertEquals(60, state.totalRemaining)
    }

    @Test fun historyIsCappedKeepingTheNewestActions() {
        var state = QazaState()
        repeat(QazaLimits.HISTORY_LIMIT + 25) { state = state.addMissed(QazaPrayer.FAJR, 1, now = it.toLong()) }
        assertEquals(QazaLimits.HISTORY_LIMIT, state.history.size)
        assertEquals((QazaLimits.HISTORY_LIMIT + 24).toLong(), state.history.last().time)
        assertEquals(25L, state.history.first().time)
    }

    @Test fun witrIsCountedOnlyWhenEnabled() {
        var state = QazaState().setRemaining(QazaPrayer.FAJR, 10, now = 1).setRemaining(QazaPrayer.WITR, 7, now = 2)
        assertEquals(10, state.totalRemaining)
        state = state.withWitrEnabled(true)
        assertEquals(17, state.totalRemaining)
        assertTrue(QazaPrayer.WITR in state.activePrayers)
    }

    @Test fun dailySetsAreBoundedAndDriveTheFinishEstimate() {
        assertEquals(1, QazaState().withDailySets(0).dailySets)
        assertEquals(QazaLimits.MAX_DAILY_SETS, QazaState().withDailySets(500).dailySets)
        val state = QazaState().setRemaining(QazaPrayer.FAJR, 10, now = 1).withDailySets(5)
        val finish = state.estimatedFinish(today = 0L)
        assertEquals(2L, (finish!!.time - 0L + 12 * 3600_000L) / (24L * 3600_000L))
    }

    @Test fun fastsCanBeAddedMarkedEditedAndDeleted() {
        var state = QazaState().addFast(5, " رمضان ۱۴۰۴ ", " سفر ")
        val entry = state.fasts.single()
        assertEquals("رمضان ۱۴۰۴", entry.label)
        assertEquals("سفر", entry.reason)
        state = state.markFastDay(entry.id, now = 100).markFastDay(entry.id, now = 200)
        assertEquals(3, state.fastsRemaining)
        assertEquals(2, state.fastsMadeUp)
        assertEquals(listOf(100L, 200L), state.fasts.single().madeUpDates)
        state = state.unmarkFastDay(entry.id)
        assertEquals(listOf(100L), state.fasts.single().madeUpDates)
        state = state.editFast(entry.id, count = 8, label = "رمضان", reason = "")
        assertEquals(7, state.fastsRemaining)
        state = state.deleteFast(entry.id)
        assertTrue(state.fasts.isEmpty())
    }

    @Test fun fastCountsAreNeverNegativeOrBelowDaysAlreadyMadeUp() {
        assertTrue(QazaState().addFast(0, "x", "").fasts.isEmpty())
        assertTrue(QazaState().addFast(-2, "x", "").fasts.isEmpty())
        var state = QazaState().addFast(3, "a", "")
        val id = state.fasts.single().id
        state = state.markFastDay(id, 1).markFastDay(id, 2).markFastDay(id, 3)
        val extra = state.markFastDay(id, 4)
        assertEquals(3, extra.fasts.single().madeUpDates.size)
        assertEquals(0, extra.fastsRemaining)
        val shrunk = state.editFast(id, count = 1, label = "a", reason = "")
        assertEquals(3, shrunk.fasts.single().count)
    }

    @Test fun fastIdsAreUniqueAfterDeletes() {
        var state = QazaState().addFast(1, "a", "").addFast(1, "b", "")
        val firstId = state.fasts.first().id
        state = state.deleteFast(firstId).addFast(1, "c", "")
        assertEquals(state.fasts.size, state.fasts.map { it.id }.distinct().size)
    }

    @Test fun emptyStateHasNoData() {
        val state = QazaState()
        assertFalse(state.hasPrayerData)
        assertEquals(0, state.totalRemaining)
        assertNull(state.history.lastOrNull())
        assertTrue(FastEntry(1, 2, "", "", listOf(1, 2, 3)).remaining == 0)
    }
}

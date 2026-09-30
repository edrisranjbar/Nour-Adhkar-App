package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.QazaRepository
import com.example.qaza.QazaLimits
import com.example.qaza.QazaPrayer
import com.example.qaza.QazaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QazaRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clear() {
        context.getSharedPreferences(QazaRepository.FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun startsEmpty() {
        val state = QazaRepository(context).load()
        assertFalse(state.hasPrayerData)
        assertEquals(0, state.totalRemaining)
        assertTrue(state.fasts.isEmpty())
        assertFalse(state.witrEnabled)
        assertEquals(1, state.dailySets)
    }

    @Test fun countersSettingsHistoryAndFastsSurviveRecreation() {
        val repository = QazaRepository(context)
        repository.update { it.setRemaining(QazaPrayer.FAJR, 30, now = 10) }
        repository.update { it.madeUp(QazaPrayer.FAJR, 4, now = 20) }
        repository.update { it.addMissed(QazaPrayer.ISHA, 2, now = 30).withWitrEnabled(true).withDailySets(3) }
        repository.update { it.addFast(6, "رمضان ۱۴۰۴", "سفر") }
        repository.update { state -> state.markFastDay(state.fasts.single().id, now = 40) }

        val restored = QazaRepository(context).load()
        assertEquals(26, restored.debt(QazaPrayer.FAJR).remaining)
        assertEquals(4, restored.debt(QazaPrayer.FAJR).madeUp)
        assertEquals(2, restored.debt(QazaPrayer.ISHA).remaining)
        assertTrue(restored.witrEnabled)
        assertEquals(3, restored.dailySets)
        assertEquals(3, restored.history.size)
        assertEquals(listOf(10L, 20L, 30L), restored.history.map { it.time })
        assertEquals("رمضان ۱۴۰۴", restored.fasts.single().label)
        assertEquals("سفر", restored.fasts.single().reason)
        assertEquals(listOf(40L), restored.fasts.single().madeUpDates)
        assertEquals(5, restored.fastsRemaining)
    }

    @Test fun undoWorksAfterTheAppIsRestarted() {
        QazaRepository(context).update { it.setRemaining(QazaPrayer.ASR, 9, now = 1).madeUp(QazaPrayer.ASR, 3, now = 2) }
        val undone = QazaRepository(context).update { it.undoLast() }
        assertEquals(9, undone.debt(QazaPrayer.ASR).remaining)
        assertEquals(9, QazaRepository(context).load().debt(QazaPrayer.ASR).remaining)
    }

    @Test fun serializationRoundTripsTheWholeState() {
        val state = QazaState()
            .applyEstimate(mapOf(QazaPrayer.FAJR to 12, QazaPrayer.WITR to 5), now = 5)
            .madeUp(QazaPrayer.FAJR, 2, now = 6)
            .addFast(3, "", "")
        assertEquals(state, QazaRepository.parse(QazaRepository.serialize(state)))
    }

    @Test fun garbageOrEmptyStoredDataGivesAnEmptyState() {
        assertFalse(QazaRepository.parse(null).hasPrayerData)
        assertFalse(QazaRepository.parse("").hasPrayerData)
        assertFalse(QazaRepository.parse("not json at all {").hasPrayerData)
        assertFalse(QazaRepository.parse("[1,2,3]").hasPrayerData)
    }

    @Test fun missingExtraAndInvalidFieldsAreTolerated() {
        val json = """
            {
              "v": 99, "future_field": {"x": 1},
              "prayers": {
                "fajr": {"o": 10, "m": 3, "extra": true},
                "dhuhr": {"o": 5},
                "asr": "broken",
                "zuhr_typo": {"o": 7, "m": 0},
                "isha": {"o": -4, "m": 9}
              },
              "history": [
                {"t": "MADE_UP", "at": 50, "c": [{"p": "fajr", "bo": 0, "bm": 0, "ao": 10, "am": 3}, {"p": "nope"}]},
                {"t": "UNKNOWN_TYPE", "at": 1, "c": []},
                "not an object",
                {"t": "MISSED", "at": 60}
              ],
              "fasts": [
                {"id": 4, "n": 2, "d": [100, 200]},
                {"n": 0},
                {"id": 9, "n": 100000, "l": "بزرگ"}
              ],
              "sets": 500
            }
        """.trimIndent()

        val state = QazaRepository.parse(json)
        assertEquals(7, state.debt(QazaPrayer.FAJR).remaining)
        assertEquals(5, state.debt(QazaPrayer.DHUHR).remaining)
        assertEquals(0, state.debt(QazaPrayer.ASR).remaining)
        assertEquals(0, state.debt(QazaPrayer.ISHA).owed)
        assertEquals(1, state.history.size)
        assertEquals(50L, state.history.single().time)
        assertEquals(2, state.fasts.size)
        assertEquals(listOf(100L, 200L), state.fasts.first().madeUpDates)
        assertEquals(QazaLimits.MAX_FAST_COUNT, state.fasts.last().count)
        assertEquals(QazaLimits.MAX_DAILY_SETS, state.dailySets)
    }

    @Test fun storedHistoryBeyondTheLimitIsTrimmed() {
        var state = QazaState()
        repeat(QazaLimits.HISTORY_LIMIT) { state = state.addMissed(QazaPrayer.DHUHR, 1, now = it.toLong()) }
        val trimmed = QazaRepository.parse(QazaRepository.serialize(state))
        assertEquals(QazaLimits.HISTORY_LIMIT, trimmed.history.size)
    }
}

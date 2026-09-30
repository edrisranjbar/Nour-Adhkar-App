package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.QazaRepository
import com.example.qaza.FastingState
import com.example.qaza.QazaLimits
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
        assertFalse(state.hasData)
        assertEquals(0, state.remaining)
    }

    @Test fun countersAndDatesSurviveRecreation() {
        val repository = QazaRepository(context)
        repository.update { it.addMissed(12) }
        repository.update { it.markMadeUp(100) }
        repository.update { it.markMadeUp(200) }

        val restored = QazaRepository(context).load()
        assertEquals(12, restored.owed)
        assertEquals(10, restored.remaining)
        assertEquals(2, restored.madeUp)
        assertEquals(listOf(100L, 200L), restored.recentDates)
    }

    @Test fun undoWorksAfterTheAppIsRestarted() {
        QazaRepository(context).update { it.addMissed(5).markMadeUp(1).markMadeUp(2) }
        val undone = QazaRepository(context).update { it.undoMadeUp() }
        assertEquals(4, undone.remaining)
        assertEquals(4, QazaRepository(context).load().remaining)
    }

    @Test fun serializationRoundTripsTheWholeState() {
        val state = FastingState().addMissed(9).markMadeUp(5).markMadeUp(6)
        assertEquals(state, QazaRepository.parse(QazaRepository.serialize(state)))
    }

    @Test fun garbageOrEmptyStoredDataGivesAnEmptyState() {
        assertFalse(QazaRepository.parse(null).hasData)
        assertFalse(QazaRepository.parse("").hasData)
        assertFalse(QazaRepository.parse("not json at all {").hasData)
        assertFalse(QazaRepository.parse("[1,2,3]").hasData)
    }

    @Test fun missingExtraAndInvalidFieldsAreTolerated() {
        val state = QazaRepository.parse("""{"v": 99, "future": {"x": 1}, "owed": 8, "madeUp": 50, "dates": [1, "x", 3]}""")
        assertEquals(8, state.owed)
        // More made up than owed is clamped so the counters stay consistent.
        assertEquals(8, state.madeUp)
        assertEquals(0, state.remaining)
        assertEquals(3, state.recentDates.size)

        val partial = QazaRepository.parse("""{"owed": 4}""")
        assertEquals(4, partial.remaining)
        assertTrue(partial.recentDates.isEmpty())

        val negative = QazaRepository.parse("""{"owed": -3, "madeUp": -2}""")
        assertFalse(negative.hasData)
    }

    @Test fun storedDatesBeyondTheLimitAreTrimmed() {
        val dates = (1..(QazaLimits.RECENT_DATES + 20)).joinToString(",")
        val state = QazaRepository.parse("""{"owed": 100, "madeUp": 10, "dates": [$dates]}""")
        assertEquals(QazaLimits.RECENT_DATES, state.recentDates.size)
        assertEquals((QazaLimits.RECENT_DATES + 20).toLong(), state.recentDates.last())
    }

    @Test fun oldPrayerTrackerDataIsIgnoredSafely() {
        val old = """{"v":1,"witr":true,"sets":3,"prayers":{"fajr":{"o":10,"m":2}},"history":[],"fasts":[{"id":1,"n":5,"d":[1,2]}]}"""
        val state = QazaRepository.parse(old)
        assertFalse(state.hasData)
    }
}

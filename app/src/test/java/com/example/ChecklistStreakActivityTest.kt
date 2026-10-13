package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyChecklistData
import com.example.data.repository.PreferenceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChecklistStreakActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = PreferenceRepository(context)
    private val items = DailyChecklistData.items.map { it.id }
    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun checkingAnItemMakesTodayActive() {
        prefs.setChecklistItemWithActivity(today, items[0], true)
        assertTrue(today in prefs.getActivityDayKeys())
    }

    @Test fun uncheckingTheOnlyItemClearsToday() {
        prefs.setChecklistItemWithActivity(today, items[0], true)
        prefs.setChecklistItemWithActivity(today, items[0], false)
        assertFalse(today in prefs.getActivityDayKeys())
    }

    @Test fun uncheckingOneOfTwoItemsKeepsToday() {
        prefs.setChecklistItemWithActivity(today, items[0], true)
        prefs.setChecklistItemWithActivity(today, items[1], true)
        prefs.setChecklistItemWithActivity(today, items[0], false)
        assertTrue(today in prefs.getActivityDayKeys())
        prefs.setChecklistItemWithActivity(today, items[1], false)
        assertFalse(today in prefs.getActivityDayKeys())
    }

    @Test fun otherActivityKeepsTodayAfterTheChecklistIsEmptied() {
        prefs.setChecklistItemWithActivity(today, items[0], true)
        prefs.markActivityToday() // e.g. a dhikr or a made-up fast
        prefs.setChecklistItemWithActivity(today, items[0], false)
        assertTrue(today in prefs.getActivityDayKeys())
    }

    @Test fun activityThatWasAlreadyThereBeforeTheChecklistIsKept() {
        prefs.markActivityToday()
        prefs.setChecklistItemWithActivity(today, items[0], true)
        prefs.setChecklistItemWithActivity(today, items[0], false)
        assertEquals(true, today in prefs.getActivityDayKeys())
    }
}

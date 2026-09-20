package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyChecklistData
import com.example.data.repository.PreferenceRepository
import com.example.widget.ChecklistWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChecklistWidgetTest {

    @Test
    fun toggleItemViaWidgetUpdatesPreferencesAndActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceRepository(context)
        val provider = ChecklistWidgetProvider()

        val testItem = DailyChecklistData.items.first().id
        val initialCompleted = prefs.getDailyChecklistCompletedIds()
        assertFalse("Item should initially not be completed", testItem in initialCompleted)

        // Simulate click intent from widget
        val clickIntent = Intent(context, ChecklistWidgetProvider::class.java).apply {
            action = ChecklistWidgetProvider.ACTION_TOGGLE_ITEM
            putExtra(ChecklistWidgetProvider.EXTRA_ITEM_ID, testItem)
        }
        provider.onReceive(context, clickIntent)

        val updatedCompleted = prefs.getDailyChecklistCompletedIds()
        assertTrue("Item should be completed after widget click", testItem in updatedCompleted)
        assertTrue("Activity today should be marked", prefs.getActivityDayKeys().isNotEmpty())

        // Simulate second click to uncheck
        provider.onReceive(context, clickIntent)
        val afterUncheckCompleted = prefs.getDailyChecklistCompletedIds()
        assertFalse("Item should be unchecked after second widget click", testItem in afterUncheckCompleted)
    }

    @Test
    fun nullItemIdInIntentDoesNotCrashOrModifyPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceRepository(context)
        val provider = ChecklistWidgetProvider()

        val before = prefs.getDailyChecklistCompletedIds()
        val emptyIntent = Intent(context, ChecklistWidgetProvider::class.java).apply {
            action = ChecklistWidgetProvider.ACTION_TOGGLE_ITEM
        }
        provider.onReceive(context, emptyIntent)
        val after = prefs.getDailyChecklistCompletedIds()
        assertEquals(before, after)
    }
}

package com.example

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.appwidget.AppWidgetManager
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyChecklistData
import com.example.data.repository.PreferenceRepository
import com.example.widget.ChecklistWidgetProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChecklistWidgetInteractionTest {
    @Test fun collectionFillInIntentChecksAndUnchecksTheSelectedTask() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val itemId = DailyChecklistData.items.first().id
        val prefs = PreferenceRepository(context)
        val widgetManager = Shadows.shadowOf(context.getSystemService(AppWidgetManager::class.java))
        val widgetId = widgetManager.createWidget(ChecklistWidgetProvider::class.java, R.layout.widget_daily_checklist)
        val template = ChecklistWidgetProvider().toggleTemplate(context, 123)
        assertFalse(template.isImmutable)

        fun tap() {
            template.send(context, 0, Intent().putExtra(ChecklistWidgetProvider.EXTRA_ITEM_ID, itemId))
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
        assertFalse(itemId in prefs.getDailyChecklistCompletedIds())
        tap()
        assertTrue(itemId in prefs.getDailyChecklistCompletedIds())
        assertTrue(widgetManager.getViewFor(widgetId).findViewById<TextView>(R.id.widget_progress_text)
            .text.contains("۱"))
        tap()
        assertFalse(itemId in prefs.getDailyChecklistCompletedIds())
    }
}

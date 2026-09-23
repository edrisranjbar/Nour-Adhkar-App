package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import com.example.prayer.PrayerSettings
import com.example.ui.language.AppLanguage
import com.example.widget.PrayerTimesWidgetProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerTimesWidgetProviderTest {
    @Test fun widgetRendersSavedTimesAndReactsToSettingsAndLanguage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = Shadows.shadowOf(context.getSystemService(AppWidgetManager::class.java))
        val id = manager.createWidget(PrayerTimesWidgetProvider::class.java, R.layout.widget_prayer_times)
        val prefs = PreferenceRepository(context)
        assertTrue(manager.getViewFor(id).findViewById<TextView>(R.id.prayer_widget_next)
            .text.contains("تعیین موقعیت"))

        prefs.setPrayerSettings(PrayerSettings("تهران", 35.6892, 51.3890))
        val rendered = manager.getViewFor(id)
        assertEquals("تهران", rendered.findViewById<TextView>(R.id.prayer_widget_city).text.toString())
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row1).childCount)
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row2).childCount)
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row3).childCount)

        prefs.setAppLanguage(AppLanguage.ARABIC)
        assertEquals("مواقيت الصلاة", manager.getViewFor(id)
            .findViewById<TextView>(R.id.prayer_widget_title).text.toString())
    }
}

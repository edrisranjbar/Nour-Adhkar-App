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
    // SDK 28 renders one layout chosen from the reported size (none here, so the 2x2 grid);
    // Android 12+ hands the launcher responsive layouts instead.
    @Config(sdk = [28])
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
        // A renamed saved place shows its own name instead of the city.
        val places = prefs.getPrayerPlaces()
        prefs.setPrayerPlaces(places.update(places.active!!.copy(name = "خانه")))
        assertEquals("خانه", manager.getViewFor(id).findViewById<TextView>(R.id.prayer_widget_city).text.toString())
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row1).childCount)
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row2).childCount)
        assertEquals(2, rendered.findViewById<LinearLayout>(R.id.prayer_widget_row3).childCount)

        prefs.setAppLanguage(AppLanguage.ARABIC)
        assertEquals("مواقيت الصلاة", manager.getViewFor(id)
            .findViewById<TextView>(R.id.prayer_widget_title).text.toString())
    }

    @Test fun responsiveWidgetStillShowsTheNextPrayerOrSetupHint() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = Shadows.shadowOf(context.getSystemService(AppWidgetManager::class.java))
        val id = manager.createWidget(PrayerTimesWidgetProvider::class.java, R.layout.widget_prayer_times)
        // Whichever responsive layout Robolectric applies, every shape has the next-prayer line.
        assertTrue(manager.getViewFor(id).findViewById<TextView>(R.id.prayer_widget_next)
            .text.contains("تعیین موقعیت"))
    }

    @Test fun sizeSelectsShapeAndTextScale() {
        fun describe(width: Int, height: Int) = PrayerTimesWidgetProvider.describeSize(width, height)
        assertEquals("GRID 1.0", describe(0, 0))          // size not reported yet
        assertEquals("COMPACT 1.0", describe(300, 60))    // one row tall
        assertEquals("GRID 1.0", describe(150, 150))      // 2x2
        assertEquals("GRID 1.25", describe(160, 240))     // 2x3
        assertEquals("WIDE 1.0", describe(300, 140))      // 4x2
        assertEquals("WIDE 1.25", describe(320, 230))     // 4x3
    }
}

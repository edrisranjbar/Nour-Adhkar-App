package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HomeShortcuts
import com.example.data.repository.PreferenceRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeShortcutsPreferenceTest {
    @Test fun customSelectionAndIntentionalEmptySelectionSurviveReopening() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("home_shortcuts").putString("prayer_location", "preserved").commit()
        val repository = PreferenceRepository(context)
        assertEquals(HomeShortcuts.defaults, repository.getHomeShortcuts())
        repository.setHomeShortcuts(listOf("qaza", "collection:sleep", "quran_audio"))
        assertEquals(listOf("qaza", "collection:sleep", "quran_audio"), PreferenceRepository(context).getHomeShortcuts())
        repository.setHomeShortcuts(emptyList())
        assertTrue(PreferenceRepository(context).getHomeShortcuts().isEmpty())
        assertEquals("preserved", prefs.getString("prayer_location", null))
    }

    @Test fun legacyUnknownIdsAndDuplicatesAreFilteredWithoutChangingOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("home_shortcuts", "[\"calendar\",\"removed-page\",\"calendar\",\"quran\"]").commit()
        assertEquals(listOf("calendar", "quran"), PreferenceRepository(context).getHomeShortcuts())
        prefs.edit().putString("home_shortcuts", "not-json").commit()
        assertEquals(HomeShortcuts.defaults, PreferenceRepository(context).getHomeShortcuts())
        assertEquals(HomeShortcuts.all.size, HomeShortcuts.all.map { it.id }.toSet().size)
    }
}

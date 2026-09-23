package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingPreferencesTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPreferences() {
        context.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun onboardingStartsIncompleteAndPersistsCompletion() {
        val preferences = PreferenceRepository(context)
        assertFalse(preferences.isOnboardingComplete())

        preferences.setOnboardingComplete(true)

        assertTrue(PreferenceRepository(context).isOnboardingComplete())
    }
}

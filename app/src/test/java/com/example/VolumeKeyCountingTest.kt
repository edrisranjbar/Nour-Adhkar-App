package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VolumeCountButton
import com.example.data.repository.PreferenceRepository
import com.example.ui.viewmodel.AdhkarViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VolumeKeyCountingTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferenceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("adhkar_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = PreferenceRepository(context)
    }

    @Test
    fun defaultPreferencesAreDisabledWithBothButtonsOption() {
        assertFalse("Volume key counting should be disabled by default", prefs.isVolumeKeyCountingEnabled())
        assertEquals(VolumeCountButton.BOTH, prefs.getVolumeCountButton())
    }

    @Test
    fun preferencesPersistCorrectly() {
        prefs.setVolumeKeyCountingEnabled(true)
        assertTrue(prefs.isVolumeKeyCountingEnabled())

        prefs.setVolumeCountButton(VolumeCountButton.UP)
        assertEquals(VolumeCountButton.UP, prefs.getVolumeCountButton())

        prefs.setVolumeCountButton(VolumeCountButton.DOWN)
        assertEquals(VolumeCountButton.DOWN, prefs.getVolumeCountButton())

        prefs.setVolumeCountButton(VolumeCountButton.BOTH)
        assertEquals(VolumeCountButton.BOTH, prefs.getVolumeCountButton())
    }

    @Test
    fun volumeCountButtonFromIdHandlesAllCases() {
        assertEquals(VolumeCountButton.BOTH, VolumeCountButton.fromId("both"))
        assertEquals(VolumeCountButton.BOTH, VolumeCountButton.fromId("BOTH"))
        assertEquals(VolumeCountButton.UP, VolumeCountButton.fromId("up"))
        assertEquals(VolumeCountButton.DOWN, VolumeCountButton.fromId("down"))
        assertEquals(VolumeCountButton.BOTH, VolumeCountButton.fromId("unknown"))
        assertEquals(VolumeCountButton.BOTH, VolumeCountButton.fromId(null))
    }

    @Test
    fun viewModelInterceptionRespectsEnabledStateAndTab() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AdhkarViewModel(app)

        // By default, disabled
        assertFalse(viewModel.volumeKeyCountingEnabled.value)
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = true))
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = false))
        assertFalse(viewModel.onVolumeKeyPressed(isVolumeUp = true))

        // Enable feature
        viewModel.setVolumeKeyCountingEnabled(true)
        assertTrue(viewModel.volumeKeyCountingEnabled.value)

        // When tab is home (default), should NOT intercept
        viewModel.selectTab("home")
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = true))
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = false))

        // When tab is tasbih, should intercept
        viewModel.selectTab("tasbih")
        assertTrue(viewModel.shouldInterceptVolumeKey(isVolumeUp = true))
        assertTrue(viewModel.shouldInterceptVolumeKey(isVolumeUp = false))

        // Counting works
        viewModel.resetTasbih()
        assertEquals(0, viewModel.tasbihCount.value)

        val handledUp = viewModel.onVolumeKeyPressed(isVolumeUp = true)
        assertTrue(handledUp)
        assertEquals(1, viewModel.tasbihCount.value)

        val handledDown = viewModel.onVolumeKeyPressed(isVolumeUp = false)
        assertTrue(handledDown)
        assertEquals(2, viewModel.tasbihCount.value)

        // Select UP only
        viewModel.setVolumeCountButton(VolumeCountButton.UP)
        assertTrue(viewModel.shouldInterceptVolumeKey(isVolumeUp = true))
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = false))

        val handledUpOnly = viewModel.onVolumeKeyPressed(isVolumeUp = true)
        assertTrue(handledUpOnly)
        assertEquals(3, viewModel.tasbihCount.value)

        val handledDownIgnored = viewModel.onVolumeKeyPressed(isVolumeUp = false)
        assertFalse(handledDownIgnored)
        assertEquals(3, viewModel.tasbihCount.value) // unchanged

        // Select DOWN only
        viewModel.setVolumeCountButton(VolumeCountButton.DOWN)
        assertFalse(viewModel.shouldInterceptVolumeKey(isVolumeUp = true))
        assertTrue(viewModel.shouldInterceptVolumeKey(isVolumeUp = false))

        val handledUpIgnored = viewModel.onVolumeKeyPressed(isVolumeUp = true)
        assertFalse(handledUpIgnored)
        assertEquals(3, viewModel.tasbihCount.value) // unchanged

        val handledDownOnly = viewModel.onVolumeKeyPressed(isVolumeUp = false)
        assertTrue(handledDownOnly)
        assertEquals(4, viewModel.tasbihCount.value)
    }
}

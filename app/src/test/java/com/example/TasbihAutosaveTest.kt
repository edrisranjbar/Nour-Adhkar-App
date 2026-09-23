package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class TasbihAutosaveTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferenceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = PreferenceRepository(context)
    }

    @Test
    fun defaultAutosaveIsDisabled() {
        assertFalse("Tasbih autosave should be disabled by default", prefs.isTasbihAutosaveEnabled())
    }

    @Test
    fun autosavePreferencePersistsCorrectly() {
        prefs.setTasbihAutosaveEnabled(true)
        assertTrue(prefs.isTasbihAutosaveEnabled())

        prefs.setTasbihAutosaveEnabled(false)
        assertFalse(prefs.isTasbihAutosaveEnabled())
    }

    @Test
    fun tasbihCountsPersistenceAndClearing() {
        prefs.setTasbihCount("سبحان الله", 15)
        prefs.setTasbihCount("الحمد لله", 25)

        assertEquals(15, prefs.getTasbihCount("سبحان الله"))
        assertEquals(25, prefs.getTasbihCount("الحمد لله"))
        assertEquals(0, prefs.getTasbihCount("الله أكبر"))

        val all = prefs.getAllTasbihCounts()
        assertEquals(2, all.size)
        assertEquals(15, all["سبحان الله"])
        assertEquals(25, all["الحمد لله"])

        prefs.setTasbihCount("سبحان الله", 0)
        assertEquals(0, prefs.getTasbihCount("سبحان الله"))
        assertEquals(1, prefs.getAllTasbihCounts().size)

        prefs.clearAllTasbihCounts()
        assertTrue(prefs.getAllTasbihCounts().isEmpty())
    }

    @Test
    fun whenAutosaveDisabledCounterResetsWhenSwitchingDhikrs() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AdhkarViewModel(app)

        assertFalse(viewModel.tasbihAutosaveEnabled.value)

        // Increment Subhanallah 10 times
        repeat(10) { viewModel.incrementTasbih() }
        assertEquals(10, viewModel.tasbihCount.value)

        // Switch to Alhamdulillah -> counter resets to 0
        viewModel.selectTasbihDhikr("الحمد لله")
        assertEquals(0, viewModel.tasbihCount.value)

        // Switch back to Subhanallah -> counter is 0
        viewModel.selectTasbihDhikr("سبحان الله")
        assertEquals(0, viewModel.tasbihCount.value)
    }

    @Test
    fun whenAutosaveEnabledCounterIsPreservedWhenSwitchingDhikrs() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AdhkarViewModel(app)

        viewModel.setTasbihAutosaveEnabled(true)
        assertTrue(viewModel.tasbihAutosaveEnabled.value)

        // Count Subhanallah 10 times
        repeat(10) { viewModel.incrementTasbih() }
        assertEquals(10, viewModel.tasbihCount.value)

        // Switch to Alhamdulillah
        viewModel.selectTasbihDhikr("الحمد لله")
        assertEquals(0, viewModel.tasbihCount.value)

        // Count Alhamdulillah 5 times
        repeat(5) { viewModel.incrementTasbih() }
        assertEquals(5, viewModel.tasbihCount.value)

        // Verify tasbihCounts state flow has both
        val counts = viewModel.tasbihCounts.value
        assertEquals(10, counts["سبحان الله"])
        assertEquals(5, counts["الحمد لله"])

        // Switch back to Subhanallah -> should restore 10 (not 0!)
        viewModel.selectTasbihDhikr("سبحان الله")
        assertEquals(10, viewModel.tasbihCount.value)

        // Switch back to Alhamdulillah -> should restore 5 (not 0!)
        viewModel.selectTasbihDhikr("الحمد لله")
        assertEquals(5, viewModel.tasbihCount.value)
    }

    @Test
    fun resetTasbihClearsCountForCurrentDhikrOnly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AdhkarViewModel(app)

        viewModel.setTasbihAutosaveEnabled(true)

        // Count Subhanallah 10
        repeat(10) { viewModel.incrementTasbih() }
        // Switch to Alhamdulillah and count 5
        viewModel.selectTasbihDhikr("الحمد لله")
        repeat(5) { viewModel.incrementTasbih() }

        // Reset Alhamdulillah
        viewModel.resetTasbih()
        assertEquals(0, viewModel.tasbihCount.value)
        assertNull(viewModel.tasbihCounts.value["الحمد لله"])

        // Switch back to Subhanallah -> still 10
        viewModel.selectTasbihDhikr("سبحان الله")
        assertEquals(10, viewModel.tasbihCount.value)
        assertEquals(10, viewModel.tasbihCounts.value["سبحان الله"])
    }
}

package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.AdhkarViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TasbihCounterStateTest {

    private val application: Application
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPreferences() {
        application.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun tasbihCountsArePreservedWhenSwitchingBetweenDhikrs() {
        val viewModel = AdhkarViewModel(application)

        // 1. Select SubhanAllah and tap 30 times
        viewModel.selectTasbihDhikr("سبحان الله")
        repeat(30) {
            viewModel.incrementTasbih()
        }
        assertEquals(30, viewModel.tasbihCount.value)
        assertEquals(30, viewModel.tasbihCounts.value["سبحان الله"])

        // 2. Switch to Alhamdulillah -> count should be 0 (not overwritten by SubhanAllah's 30)
        viewModel.selectTasbihDhikr("الحمد لله")
        assertEquals("الحمد لله", viewModel.selectedTasbihDhikr.value)
        assertEquals(0, viewModel.tasbihCount.value)

        // 3. Tap Alhamdulillah 15 times
        repeat(15) {
            viewModel.incrementTasbih()
        }
        assertEquals(15, viewModel.tasbihCount.value)
        assertEquals(15, viewModel.tasbihCounts.value["الحمد لله"])

        // 4. Return to SubhanAllah -> count must still be 30!
        viewModel.selectTasbihDhikr("سبحان الله")
        assertEquals("سبحان الله", viewModel.selectedTasbihDhikr.value)
        assertEquals(30, viewModel.tasbihCount.value)

        // 5. Increment SubhanAllah once more -> goes to 31
        viewModel.incrementTasbih()
        assertEquals(31, viewModel.tasbihCount.value)

        // 6. Switch back to Alhamdulillah -> still 15
        viewModel.selectTasbihDhikr("الحمد لله")
        assertEquals(15, viewModel.tasbihCount.value)

        // 7. Reset Alhamdulillah -> becomes 0
        viewModel.resetTasbih()
        assertEquals(0, viewModel.tasbihCount.value)

        // 8. SubhanAllah count is unaffected and remains 31
        viewModel.selectTasbihDhikr("سبحان الله")
        assertEquals(31, viewModel.tasbihCount.value)
    }

    @Test
    fun tasbihCountsPersistAcrossRecreation() {
        val vm1 = AdhkarViewModel(application)
        vm1.selectTasbihDhikr("سبحان الله")
        repeat(33) { vm1.incrementTasbih() }

        vm1.selectTasbihDhikr("الله أكبر")
        repeat(10) { vm1.incrementTasbih() }

        // Recreate viewModel from persisted preferences
        val vm2 = AdhkarViewModel(application)
        assertEquals("الله أكبر", vm2.selectedTasbihDhikr.value)
        assertEquals(10, vm2.tasbihCount.value)

        vm2.selectTasbihDhikr("سبحان الله")
        assertEquals(33, vm2.tasbihCount.value)
    }
}

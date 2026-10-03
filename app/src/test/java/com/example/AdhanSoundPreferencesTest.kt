package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import com.example.prayer.AdhanSound
import com.example.prayer.adhanRecordings
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdhanSoundPreferencesTest {
    @Test fun selectionSurvivesRepositoryRecreationAndCanBeClearedWithoutChangingReminders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceRepository(context)
        prefs.setMorningNotificationTime("06:45")
        adhanRecordings.forEach { recording ->
            val selected = AdhanSound(recording.id)
            prefs.setAdhanSound(selected)
            assertEquals(selected, PreferenceRepository(context).getAdhanSound())
            assertTrue(PreferenceRepository(context).getAdhanSound().isSelected)
        }
        val recreated = PreferenceRepository(context)
        recreated.setAdhanSound(AdhanSound())
        assertFalse(PreferenceRepository(context).getAdhanSound().isSelected)
        assertEquals("06:45", recreated.getMorningNotificationTime())
    }

    @Test fun sixDistinctBundledRecordingsMatchTheirSources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedTitles = listOf("علی بن احمد ملا (مکه)", "اذان مسجدالنبی (مدینه)", "مشاری راشد العفاسی",
            "عبدالباسط عبدالصمد", "ناصر القطامی", "یاسر الدوسری")
        val expectedHashes = listOf("a16fb14b22397c22fb809187e81d3b3ef9a02f29",
            "1b0602ae0af66e77d65512297eccd37ecc9e1899", "f36d1d6dbcb69692e0269aa60d3c915610e986e0",
            "1412837b8d0d75323c8f36308a66b649df62799b", "3bb36bbc8595a228fc949ed7e53eea1c6cb50e3a",
            "3e5c6cd42cd635343f1ed0fc2182f87dfa9c2e32")
        assertEquals(6, adhanRecordings.size)
        assertEquals(6, adhanRecordings.map { it.id }.toSet().size)
        assertEquals(expectedTitles, adhanRecordings.map { it.title })
        adhanRecordings.forEachIndexed { index, recording ->
            val bytes = context.resources.openRawResource(recording.resource).use { it.readBytes() }
            assertTrue(bytes.size > 300_000)
            val hash = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(expectedHashes[index], hash)
        }
    }

    @Test fun invalidSelectionDoesNotOverwriteSavedChoice() {
        val prefs = PreferenceRepository(ApplicationProvider.getApplicationContext<Context>())
        prefs.setAdhanSound(AdhanSound("adhan_three"))
        assertThrows(IllegalArgumentException::class.java) { prefs.setAdhanSound(AdhanSound("missing")) }
        assertEquals("adhan_three", prefs.getAdhanSound().id)
    }
}

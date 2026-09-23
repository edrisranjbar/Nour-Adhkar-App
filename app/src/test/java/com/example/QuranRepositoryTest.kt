package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PreferenceRepository
import com.example.quran.QuranRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranRepositoryTest {
    @Test
    fun bundledQuranHasEveryMadaniPageAndAllVerses() {
        val corpus = QuranRepository.load(ApplicationProvider.getApplicationContext())

        assertEquals(QuranRepository.PAGE_COUNT, corpus.pages.size)
        assertEquals(QuranRepository.VERSE_COUNT, corpus.verses.size)
        assertEquals(114, corpus.surahs.size)
        assertTrue(corpus.pages.all { it.verses.isNotEmpty() })
        assertEquals("1:1", corpus.pages.first().verses.first().id)
        assertEquals("114:6", corpus.pages.last().verses.last().id)
        assertEquals(1, corpus.surahs.first().firstPage)
        assertEquals(604, corpus.surahs.last().firstPage)
        assertEquals(7, corpus.surahs.first().verseCount)
        assertTrue(corpus.verses.first { it.id == "2:1" }.bismillah?.isNotBlank() == true)
    }

    @Test
    fun readerPreferencesKeepPageColorHighlightsAndNotes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceRepository(context)
        prefs.setQuranLastReadPage(604)
        prefs.setQuranReaderColor("sage")
        prefs.setQuranHighlight("2:255", "gold")
        prefs.setQuranNote("2:255", "یادداشت آزمون")

        val recreated = PreferenceRepository(context)
        assertEquals(604, recreated.getQuranLastReadPage())
        assertEquals("sage", recreated.getQuranReaderColor())
        assertEquals("gold", recreated.getQuranHighlights()["2:255"])
        assertEquals("یادداشت آزمون", recreated.getQuranNotes()["2:255"])

        recreated.setQuranHighlight("2:255", null)
        recreated.setQuranNote("2:255", "")
        assertTrue(PreferenceRepository(context).getQuranHighlights()["2:255"] == null)
        assertTrue(PreferenceRepository(context).getQuranNotes()["2:255"] == null)
    }
}

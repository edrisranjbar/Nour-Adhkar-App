package com.example

import com.example.quran.QuranVerse
import com.example.share.ayahShareCard
import com.example.share.ayahShareText
import com.example.store.StoreConfig
import com.example.ui.language.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AyahShareTest {
    private val verse = QuranVerse(
        surahNumber = 112, surahName = "الإخلاص", verseNumber = 1,
        text = "قُلْ هُوَ اللَّهُ أَحَدٌ", bismillah = null, pageNumber = 604,
        displayText = "font-encoded"
    )

    @Test
    fun `text share uses the unicode verse, the reference and the optional translation`() {
        val withTranslation = ayahShareText(verse, "بگو: او خدای یگانه است.", "ترجمه فولادوند", AppLanguage.FARSI)
        assertTrue(withTranslation.startsWith("﴿قُلْ هُوَ اللَّهُ أَحَدٌ﴾"))
        assertTrue("سوره الإخلاص، آیه ۱" in withTranslation)
        assertTrue("بگو: او خدای یگانه است." in withTranslation)
        assertTrue("(ترجمه فولادوند)" in withTranslation)
        assertFalse("font-encoded" in withTranslation)

        val arabicOnly = ayahShareText(verse, null, "ترجمه فولادوند", AppLanguage.FARSI)
        assertFalse("ترجمه فولادوند" in arabicOnly)
        assertTrue(StoreConfig.WEB_URL in arabicOnly)
    }

    @Test
    fun `image card puts the verse in the Quranic face and the translation in the body`() {
        val card = ayahShareCard(verse, "بگو: او خدای یگانه است.", AppLanguage.FARSI)
        assertEquals("سوره الإخلاص، آیه ۱", card.eyebrow)
        assertEquals("﴿قُلْ هُوَ اللَّهُ أَحَدٌ﴾", card.headline)
        assertTrue(card.headlineIsArabic)
        assertEquals("بگو: او خدای یگانه است.", card.body)
        assertNull(ayahShareCard(verse, "  ", AppLanguage.FARSI).body)
    }
}

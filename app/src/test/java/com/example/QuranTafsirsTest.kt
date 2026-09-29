package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.quran.QuranRepository
import com.example.quran.QuranTafsir
import com.example.quran.QuranTafsirs
import com.example.ui.language.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranTafsirsTest {
    @Test
    fun persianReadersSeePersianTafsirsFirstAndArabicReadersOnlyArabic() {
        val persian = QuranTafsir.optionsFor(AppLanguage.FARSI)
        assertEquals(QuranTafsir.entries.size, persian.size)
        assertEquals(AppLanguage.FARSI, persian.first().textLanguage)
        assertTrue(QuranTafsir.optionsFor(AppLanguage.ARABIC).all { it.textLanguage == AppLanguage.ARABIC })
        assertEquals(QuranTafsir.ARABIC_SAADI, QuranTafsir.forLanguage(AppLanguage.ARABIC, "fa_saadi"))
        assertEquals(QuranTafsir.ARABIC_IBN_KATHIR, QuranTafsir.forLanguage(AppLanguage.FARSI, "ar_ibn_kathir"))
    }

    @Test
    fun parserResolvesSharedPassagesAndMissingVerses() {
        val surah = QuranTafsirs.parse("1|first\\nsecond\n2|=1\n3|=1\n5|fifth\n".byteInputStream())

        val grouped = surah.passage(2)!!
        assertEquals("first\nsecond", grouped.text)
        assertEquals(1, grouped.fromAyah)
        assertEquals(3, grouped.toAyah)
        assertFalse(grouped.previousOnly)

        val missing = surah.passage(4)!!
        assertTrue(missing.previousOnly)
        assertEquals(1, missing.fromAyah)

        assertEquals(5, surah.passage(5)!!.fromAyah)
        assertNull(QuranTafsirs.parse("3|third\n".byteInputStream()).passage(1))
    }

    @Test
    fun everyBundledTafsirHasEverySurahAndAnswersEveryVerse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val corpus = QuranRepository.load(context)
        QuranTafsir.entries.forEach { tafsir ->
            corpus.surahs.forEach { surah ->
                context.assets.open("quran/tafsir/${tafsir.id}/${surah.number}.txt").use { input ->
                    assertTrue("${tafsir.id} ${surah.number}", QuranTafsirs.parse(input).passage(surah.verseCount) != null)
                }
            }
            val passage = QuranTafsirs.passage(context, tafsir, 2, 255)
            assertNotNull(tafsir.id, passage)
            assertTrue(tafsir.id, passage!!.text.isNotBlank())
        }
    }
}

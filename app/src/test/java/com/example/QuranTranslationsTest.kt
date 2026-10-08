package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.quran.QuranRepository
import com.example.quran.QuranTranslation
import com.example.quran.QuranTranslations
import com.example.ui.language.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranTranslationsTest {
    @Test
    fun appLanguageSelectsItsSunniTranslation() {
        assertEquals(QuranTranslation.PERSIAN_ISLAMHOUSE, QuranTranslation.forLanguage(AppLanguage.FARSI))
        assertEquals(QuranTranslation.ARABIC_MUYASSAR, QuranTranslation.forLanguage(AppLanguage.ARABIC))
        assertEquals(QuranTranslation.URDU_JUNAGARHI, QuranTranslation.forLanguage(AppLanguage.URDU))
        assertEquals(QuranTranslation.PERSIAN_ISLAMHOUSE, QuranTranslation.forLanguage(AppLanguage.DARI))
        assertEquals(QuranTranslation.URDU_JUNAGARHI, QuranTranslation.forLanguage(AppLanguage.URDU, "fa_khorramdel"))
    }

    @Test
    fun savedChoiceAppliesOnlyToItsLanguage() {
        assertEquals(
            QuranTranslation.PERSIAN_KHORRAMDEL,
            QuranTranslation.forLanguage(AppLanguage.FARSI, "fa_khorramdel")
        )
        assertEquals(QuranTranslation.ARABIC_MUYASSAR, QuranTranslation.forLanguage(AppLanguage.ARABIC, "fa_khorramdel"))
        assertEquals(QuranTranslation.PERSIAN_ISLAMHOUSE, QuranTranslation.forLanguage(AppLanguage.FARSI, "removed_id"))
    }

    @Test
    fun everyTranslationCoversEveryVerse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val verseIds = QuranRepository.load(context).verses.map { it.id }.toSet()
        QuranTranslation.entries.forEach { translation ->
            val texts = QuranTranslations.load(context, translation)
            assertEquals(translation.name, verseIds, texts.keys)
            assertTrue(translation.name, texts.values.all { it.isNotBlank() })
        }
    }

    @Test
    fun parserSkipsAttributionCommentsAndKeepsPipesInText() {
        val parsed = QuranTranslations.parse("# credit\n1|1|a|b\n\n114|6|c\n".byteInputStream())
        assertEquals(mapOf("1:1" to "a|b", "114:6" to "c"), parsed)
    }
}

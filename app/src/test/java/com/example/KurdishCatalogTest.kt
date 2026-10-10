package com.example

import com.example.quran.QuranTafsir
import com.example.ui.language.AppLanguage
import com.example.ui.language.ArabicCatalog
import com.example.ui.language.KurdishCatalog
import com.example.ui.language.reference
import com.example.ui.language.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KurdishCatalogTest {
    @Test fun everyArabicCatalogKeyHasAKurdishEntry() {
        val missing = ArabicCatalog.keys - KurdishCatalog.keys
        assertTrue("Missing Kurdish entries: $missing", missing.isEmpty())
    }

    @Test fun templatesKeepTheirPlaceholders() {
        val source = java.io.File("src/main/java/com/example/ui/language/KurdishCatalog.kt").readText()
        val placeholder = Regex("""\{[0-9]\}""")
        source.lines().filter { it.contains('|') && placeholder.containsMatchIn(it) }.forEach { line ->
            val (key, target) = line.split('|', limit = 2)
            assertEquals(line, placeholder.findAll(key).map { it.value }.sorted().toList(),
                placeholder.findAll(target).map { it.value }.sorted().toList())
        }
    }

    @Test fun translatesLiteralsAndTemplatesWithPersianDigits() {
        assertEquals("ڕێکخستنەکان", AppLanguage.KURDISH.text("تنظیمات"))
        assertEquals("نوێژی داهاتوو", AppLanguage.KURDISH.text("نماز بعدی"))
        assertEquals("۲ کاتژمێر و ۱۵ خولەک", AppLanguage.KURDISH.text("۲ ساعت و ۱۵ دقیقه"))
        assertEquals("۵ لە ۳۰", AppLanguage.KURDISH.text("5 از 30"))
        assertEquals("۱۲ ڕۆژی لەسەریەک", AppLanguage.KURDISH.text("۱۲ روز متوالی"))
        assertEquals("تەفسیری ئایەتەکانی ۶ تا ۷", AppLanguage.KURDISH.text("تفسیر آیه‌های ۶ تا ۷"))
    }

    @Test fun citationsUseSoraniSpellingButLeaveLongNotesAlone() {
        assertEquals("سەحیحی موسلیم، فەرموودەی ۷۱۳", AppLanguage.KURDISH.reference("صحیح مسلم، حدیث ۷۱۳"))
        assertEquals("سوورەتی بەقەرە، ئایەتی ۲۸۶", AppLanguage.KURDISH.reference("سوره بقره، آیه ۲۸۶"))
        val note = "محبوب‌ترین سخنان نزد خداوند که موازین حسنات را سنگین می‌کند (صحیح مسلم)."
        assertEquals(note, AppLanguage.KURDISH.reference(note))
    }

    @Test fun kurdishHidesPersianContentAndUsesArabicTafsirs() {
        assertFalse(AppLanguage.KURDISH.usesPersianContent)
        assertFalse(AppLanguage.KURDISH.showPersianTranslation)
        assertEquals(AppLanguage.KURDISH, AppLanguage.fromCode("ckb"))
        assertEquals(QuranTafsir.optionsFor(AppLanguage.ARABIC), QuranTafsir.optionsFor(AppLanguage.KURDISH))
    }
}

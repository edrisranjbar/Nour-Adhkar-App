package com.example

import com.example.quran.QuranTafsir
import com.example.ui.language.AppLanguage
import com.example.ui.language.ArabicCatalog
import com.example.ui.language.UrduCatalog
import com.example.ui.language.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrduCatalogTest {
    @Test fun everyArabicCatalogKeyHasAnUrduEntry() {
        val missing = ArabicCatalog.keys - UrduCatalog.keys
        assertTrue("Missing Urdu entries: $missing", missing.isEmpty())
    }

    @Test fun templatesKeepTheirPlaceholders() {
        val source = java.io.File("src/main/java/com/example/ui/language/UrduCatalog.kt").readText()
        val placeholder = Regex("""\{[0-9]\}""")
        source.lines().filter { it.contains('|') && placeholder.containsMatchIn(it) }.forEach { line ->
            val (key, target) = line.split('|', limit = 2)
            assertEquals(line, placeholder.findAll(key).map { it.value }.sorted().toList(),
                placeholder.findAll(target).map { it.value }.sorted().toList())
        }
    }

    @Test fun translatesLiteralsAndTemplatesWithUrduOrderAndDigits() {
        assertEquals("ترتیبات", AppLanguage.URDU.text("تنظیمات"))
        assertEquals("اگلی نماز", AppLanguage.URDU.text("نماز بعدی"))
        assertEquals("۲ گھنٹے ۱۵ منٹ", AppLanguage.URDU.text("۲ ساعت و ۱۵ دقیقه"))
        assertEquals("۳۰ میں سے ۵", AppLanguage.URDU.text("5 از 30"))
        assertEquals("۱۲ دن مسلسل", AppLanguage.URDU.text("۱۲ روز متوالی"))
        assertEquals("فون کو ۴۵° دائیں گھمائیں", AppLanguage.URDU.text("گوشی را ۴۵° به راست بچرخانید"))
        // Nested phrase from the Quran reader: the range inside the tafsir label is localized too.
        assertEquals("آیات ۶ تا ۷ کی تفسیر", AppLanguage.URDU.text("تفسیر آیه‌های ۶ تا ۷"))
    }

    @Test fun urduHidesPersianContentAndUsesArabicTafsirs() {
        assertFalse(AppLanguage.URDU.usesPersianContent)
        assertFalse(AppLanguage.URDU.showPersianTranslation)
        assertEquals(AppLanguage.URDU, AppLanguage.fromCode("ur"))
        assertEquals(QuranTafsir.optionsFor(AppLanguage.ARABIC), QuranTafsir.optionsFor(AppLanguage.URDU))
    }

    @Test fun catalogPatternsAvoidSyntaxAndroidIcuRejects() {
        // JVM regex accepts forms (e.g. `\pL`, a bare `}`) that crashed Android's ICU on device.
        val source = java.io.File("src/main/java/com/example/ui/language/PhraseCatalog.kt").readText()
        assertFalse(source.contains("\\\\p"))
        assertTrue(source.contains("""Regex("\\{[0-9]+\\}")"""))
    }
}

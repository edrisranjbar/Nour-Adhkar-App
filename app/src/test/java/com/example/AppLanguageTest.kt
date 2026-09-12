package com.example

import com.example.ui.language.*
import com.example.data.model.DhikrItem
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @Test fun formattedArabicTextKeepsArgumentsAndLocalizesDigits() {
        assertEquals("٢٢ ذكر", AppLanguage.ARABIC.text("۲۲ ذکر"))
        assertEquals("بعد ٢ ساعة و١٥ دقيقة", AppLanguage.ARABIC.text("۲ ساعت و ۱۵ دقیقه دیگر"))
        assertEquals("الصلاة القادمة • غدًا", AppLanguage.ARABIC.text("نماز بعدی • فردا"))
        assertEquals("أدر الهاتف ٤٥° إلى اليمين", AppLanguage.ARABIC.text("گوشی را ۴۵° به راست بچرخانید"))
    }
    @Test fun farsiIsTheDefaultAndKeepsItsText() {
        assertEquals(AppLanguage.FARSI, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.FARSI, AppLanguage.fromCode("bad"))
        assertEquals("۲۲ ذکر", AppLanguage.FARSI.text("۲۲ ذکر"))
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
    }
    @Test fun arabicSharingOmitsPersianWithoutChangingOriginalDhikr() {
        val item = DhikrItem(1, "سُبْحَانَ اللَّهِ", "خدا پاک و منزه است", 33, "صحیح مسلم، حدیث ۲۷۲۱")
        val arabic = item.shareText(AppLanguage.ARABIC)
        assertTrue(arabic.startsWith(item.arabicText))
        assertFalse(arabic.contains(item.persianTranslation))
        assertTrue(arabic.contains("صحيح مسلم، حديث ٢٧٢١"))
        assertTrue(item.shareText(AppLanguage.FARSI).contains(item.persianTranslation))
        assertEquals("سُبْحَانَ اللَّهِ", AppLanguage.ARABIC.text(item.arabicText))
    }
}

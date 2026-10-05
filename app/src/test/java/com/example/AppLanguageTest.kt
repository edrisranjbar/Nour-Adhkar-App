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
    @Test fun dariUsesAfghanMonthNamesAndWording() {
        assertEquals(AppLanguage.DARI, AppLanguage.fromCode("prs"))
        assertEquals("حمل", AppLanguage.DARI.text("فروردین"))
        assertEquals("مناسبت‌های حوت", AppLanguage.DARI.text("مناسبت‌های اسفند"))
        assertEquals("شنبه ۵ میزان ۱۴۰۵", AppLanguage.DARI.text("شنبه ۵ مهر ۱۴۰۵"))
        assertEquals("جدی ۱۴۰۴", AppLanguage.DARI.text("دی ۱۴۰۴"))
        assertEquals("سرطان", AppLanguage.DARI.text("تیر"))
        assertEquals("می", AppLanguage.DARI.text("مه"))
        assertEquals("موبایل را ۴۵° به راست بچرخانید", AppLanguage.DARI.text("گوشی را ۴۵° به راست بچرخانید"))
        assertEquals("موبایل‌تان", AppLanguage.DARI.text("گوشی‌تان"))
        assertEquals("۲۲ ذکر", AppLanguage.DARI.text("۲۲ ذکر"))
    }
    @Test fun dariLeavesOrdinaryWordsAndTranslatedTextAlone() {
        // تیر/مهر/دی are also ordinary words; «مه» (fog) is not a month inside a sentence.
        assertEquals("با مهر و محبت", AppLanguage.DARI.text("با مهر و محبت"))
        assertEquals("خداوند مهربان است", AppLanguage.DARI.text("خداوند مهربان است"))
        assertEquals("مه غلیظ", AppLanguage.DARI.text("مه غلیظ"))
        val once = AppLanguage.DARI.text("مناسبت‌های اردیبهشت، ۱۰ دی ۱۴۰۴، گوشی")
        assertEquals(once, AppLanguage.DARI.text(once))
    }
    @Test fun dariReadsPersianQuranContent() {
        assertEquals(
            com.example.quran.QuranTranslation.optionsFor(AppLanguage.FARSI),
            com.example.quran.QuranTranslation.optionsFor(AppLanguage.DARI)
        )
        assertEquals(
            com.example.quran.QuranTafsir.optionsFor(AppLanguage.FARSI),
            com.example.quran.QuranTafsir.optionsFor(AppLanguage.DARI)
        )
        assertTrue(AppLanguage.DARI.showPersianTranslation)
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

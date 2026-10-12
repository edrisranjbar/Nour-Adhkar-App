package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.quran.QuranSurah
import com.example.quran.TafsirPassage
import com.example.quran.QuranVerse
import com.example.ui.language.AppLanguage
import com.example.ui.util.toPersianDigits

internal class QuranLabels(private val language: AppLanguage) {
    private val arabic get() = language == AppLanguage.ARABIC
    /** Arabic wording is written here; other languages localize the Persian source (Dari, Urdu). */
    private fun pick(arabicText: String, persian: String) = if (arabic) arabicText else language.text(persian)

    val search get() = pick("البحث في القرآن", "جست‌وجو در قرآن")
    val readerTitle get() = pick("القرآن الكريم", "قرآن کریم")
    val backToHome get() = pick("العودة إلى الرئيسية", "بازگشت به خانه")
    val clearSearch get() = pick("مسح البحث", "پاک کردن جست‌وجو")
    val pageColor get() = pick("لون الصفحة", "رنگ صفحه")
    val goToSurah get() = pick("الانتقال إلى سورة", "رفتن به سوره")
    val goToPage get() = pick("الانتقال إلى صفحة", "رفتن به صفحه")
    val chooseSurah get() = pick("اختر سورة", "انتخاب سوره")
    val searchSurah get() = pick("ابحث باسم السورة أو رقمها", "جست‌وجوی نام یا شماره سوره")
    val noSurahResults get() = pick("لم يتم العثور على سورة", "سوره‌ای پیدا نشد")
    val current get() = pick("الحالية", "فعلی")
    val pageRange get() = pick("رقم الصفحة (١–٦٠٤)", "شماره صفحه (۱ تا ۶۰۴)")
    val go get() = pick("انتقال", "برو")
    val more get() = pick("المزيد", "بیشتر")
    val noResults get() = pick("لا توجد نتائج", "نتیجه‌ای پیدا نشد")
    val page get() = pick("الصفحة", "صفحه")
    val surah get() = pick("سورة", "سوره")
    val verse get() = pick("آية", "آیه")
    val highlight get() = pick("تمييز الآية", "هایلایت آیه")
    val removeHighlight get() = pick("إزالة التمييز", "حذف هایلایت")
    // Arabic readers get al-Muyassar's plain meaning here, so "المعنى" rather than "الترجمة".
    val translation get() = pick("المعنى", "ترجمه")
    val translationPicker get() = pick("ترجمة الآيات", "ترجمهٔ آیات")
    val tafsir get() = pick("التفسير", "تفسیر")
    val tafsirUnavailable get() = pick("لا يوجد تفسير لهذه الآية في هذا الكتاب.", "این تفسیر برای این آیه متنی ندارد.")

    private fun ayahRange(passage: TafsirPassage): String {
        val from = passage.fromAyah.toPersianDigits()
        val to = passage.toAyah.toPersianDigits()
        return when {
            // Raw Persian here: the callers' pick() localizes the whole phrase, range included.
            passage.fromAyah == passage.toAyah -> if (arabic) "الآية $from" else "آیهٔ $from"
            arabic -> "الآيات $from–$to"
            else -> "آیه‌های $from تا $to"
        }
    }

    fun tafsirOfRange(passage: TafsirPassage): String =
        pick("تفسير ${ayahRange(passage)}", "تفسیر ${ayahRange(passage)}")

    fun tafsirOfPrevious(passage: TafsirPassage): String = pick(
        "لا يفرد هذا التفسير هذه الآية بكلام؛ هذا تفسير ${ayahRange(passage)}:",
        "این تفسیر برای این آیه متن جداگانه‌ای ندارد؛ تفسیر ${ayahRange(passage)}:"
    )

    val translationUnavailable get() = pick("المعنى غير متاح حاليًا.", "ترجمهٔ این آیه در دسترس نیست.")
    val addNote get() = pick("إضافة ملاحظة", "افزودن یادداشت")
    val shareVerse get() = pick("مشاركة الآية", "اشتراک‌گذاری")
    val editNote get() = pick("ویرایش یادداشت", "ویرایش یادداشت")
    val writeNote get() = pick("اكتب ملاحظتك", "یادداشت خود را بنویسید")
    val playFromHere get() = pick("التلاوة من هنا", "پخش از این آیه")
    val playPage get() = pick("تلاوة آية بآية من هذه الصفحة", "پخش آیه‌به‌آیه از این صفحه")
    val stopRecitation get() = pick("إيقاف التلاوة", "توقف تلاوت")
    val verseByVerseReciters get() = pick("التلاوة آية بآية", "قاریان پخش آیه‌به‌آیه")
    val previousVerse get() = pick("الآية السابقة", "آیهٔ قبل")
    val nextVerse get() = pick("الآية التالية", "آیهٔ بعد")
    val pause get() = pick("إيقاف مؤقت", "توقف موقت")
    val resume get() = pick("متابعة", "ادامه")
    val bismillah get() = pick("البسملة", "بسم‌الله")
    val save get() = pick("حفظ", "ذخیره")
    val cancel get() = pick("إلغاء", "لغو")

    fun noteFor(verse: QuranVerse): String =
        "${surah} ${verse.surahName} · ${verse.verseNumber.toPersianDigits()}"

    fun surahMeta(surah: QuranSurah): String = pick(
        "الصفحة ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آية",
        "صفحه ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آیه"
    )

    fun colorName(color: QuranReaderColor): String = when (color) {
        QuranReaderColor.Paper -> pick("ورقي", "کاغذی")
        QuranReaderColor.Sepia -> pick("بني فاتح", "سپیا")
        QuranReaderColor.Sage -> pick("سبز ملایم", "سبز ملایم")
        QuranReaderColor.Night -> pick("ليلي", "شب")
    }
}

package com.example.ui.language

import com.example.data.model.DhikrItem
import com.example.share.ShareCardSpec
import com.example.share.appShareFooter

/** Source metadata only; never applied to the original Quran or adhkar text. */
fun AppLanguage.reference(source: String): String {
    if (this == AppLanguage.URDU) return urduReference(source)
    if (this == AppLanguage.KURDISH) return kurdishReference(source)
    if (this != AppLanguage.ARABIC) return source
    var result = source
    val words = linkedMapOf(
        "هنگام غلت‌زدن و بی‌قراری در شب" to "عند التقلب والأرق ليلًا",
        "هنگام بازگشت از سفر" to "عند الرجوع من السفر",
        "هنگام پوشیدن لباس نو" to "عند لبس الثوب الجديد",
        "هنگام پوشیدن لباس" to "عند لبس الثوب", "هنگام ورود" to "عند الدخول",
        "صحیح" to "صحيح", "بخاری" to "البخاري", "ترمذی" to "الترمذي",
        "ابوداوود" to "أبي داود", "نسائی" to "النسائي", "ابن ماجه" to "ابن ماجه",
        "حدیث" to "حديث", "سوره" to "سورة", "آیات" to "الآيات", "آیه" to "الآية",
        "بقره" to "البقرة", "آل‌عمران" to "آل عمران", "اعراف" to "الأعراف",
        "ابراهیم" to "إبراهيم", "فرقان" to "الفرقان", "حشر" to "الحشر",
        "فاتحه" to "الفاتحة", "نساء" to "النساء", "مائده" to "المائدة",
        "یونس" to "يونس", "یوسف" to "يوسف", "اسراء" to "الإسراء",
        "کهف" to "الكهف", "انبیاء" to "الأنبياء",
        "مؤمنون" to "المؤمنون", "شعراء" to "الشعراء", "قصص" to "القصص",
        "عنکبوت" to "العنكبوت", "صافات" to "الصافات",
        "دخان" to "الدخان", "احقاف" to "الأحقاف", "ممتحنه" to "الممتحنة",
        "تحریم" to "التحريم", " تا " to " إلى ",
        "ذکر" to "ذكر", " و " to " و", "ی" to "ي", "ک" to "ك"
    )
    words.forEach { (fa, ar) -> result = result.replace(fa, ar) }
    return text(result)
}

/** Hadith collection names read the same in Urdu; only the Persian phrasing and spelling differ. */
private fun AppLanguage.urduReference(source: String): String {
    var result = source
    linkedMapOf(
        "هنگام غلت‌زدن و بی‌قراری در شب" to "رات کو کروٹ بدلتے اور بے چینی کے وقت",
        "هنگام بازگشت از سفر" to "سفر سے واپسی پر",
        "هنگام پوشیدن لباس نو" to "نیا لباس پہنتے وقت",
        "هنگام پوشیدن لباس" to "لباس پہنتے وقت", "هنگام ورود" to "داخل ہوتے وقت",
        "ابن ماجه" to "ابن ماجہ", "سوره" to "سورہ", "آیه" to "آیت",
        "بقره" to "بقرہ", "ابراهیم" to "ابراہیم", "فاتحه" to "فاتحہ", "مائده" to "مائدہ",
        "کهف" to "کہف", "ممتحنه" to "ممتحنہ", "آل‌عمران" to "آلِ عمران"
    ).forEach { (persian, urdu) -> result = result.replace(persian, urdu) }
    return text(result)
}

/**
 * Sorani spelling for citations such as «صحیح مسلم، حدیث ۷۱۳» or «سوره بقره، آیه ۲۸۶». Only
 * `؛`-separated parts that start with a known citation word are rewritten; longer Persian notes
 * about a dhikr's virtue are not translated and stay as written, so they never mix scripts.
 */
private fun AppLanguage.kurdishReference(source: String): String =
    text(source.split("؛ ").joinToString("؛ ") { part ->
        if (KURDISH_CITATION_WORDS.keys.any { part.startsWith(it) }) {
            KURDISH_CITATION_WORDS.entries.fold(part) { acc, (persian, kurdish) -> acc.replace(persian, kurdish) }
        } else part
    })

private val KURDISH_CITATION_WORDS = linkedMapOf(
    "هنگام غلت‌زدن و بی‌قراری در شب" to "لە کاتی تلانەوە و بێ‌ئارامی لە شەودا",
    "هنگام بازگشت از سفر" to "لە کاتی گەڕانەوە لە گەشت",
    "هنگام پوشیدن لباس نو" to "لە کاتی لەبەرکردنی جلی نوێ",
    "هنگام پوشیدن لباس" to "لە کاتی لەبەرکردنی جل", "هنگام ورود" to "لە کاتی چوونە ژوورەوە",
    "هنگام خروج" to "لە کاتی هاتنەدەرەوە", "پیش از غذا" to "پێش خواردن",
    "صحیح بخاری" to "سەحیحی بوخاری", "صحیح مسلم" to "سەحیحی موسلیم",
    "سنن ابوداوود" to "سونەنی ئەبوداود", "سنن ترمذی" to "سونەنی تیرمیزی",
    "حصن المسلم" to "حیسنی موسلیم", "حدیث" to "فەرموودەی", "ذکر" to "زیکری",
    "سوره" to "سوورەتی", "آیات" to "ئایەتەکانی", "آیه" to "ئایەتی",
    "بقره" to "بەقەرە", "آل‌عمران" to "ئال عیمران", "اعراف" to "ئەعراف", "ابراهیم" to "ئیبراهیم",
    "کهف" to "کەهف", "رعد" to "ڕەعد", "یونس" to "یوونس", "یوسف" to "یووسف"
)

fun DhikrItem.shareText(language: AppLanguage): String = buildString {
    append(arabicText.trim())
    if (language.showPersianTranslation && persianTranslation.isNotBlank()) {
        append("\n\n"); append(persianTranslation.trim())
    }
    if (source.isNotBlank()) { append("\n\n"); append(language.reference(source.trim())) }
    append("\n\n"); append(appShareFooter(language))
}

/** Image-card version of [shareText]; the store link is printed on the card itself. */
fun DhikrItem.shareCard(language: AppLanguage): ShareCardSpec = ShareCardSpec(
    eyebrow = language.text("ذکری برای امروز"),
    headline = arabicText.trim(),
    headlineIsArabic = true,
    body = persianTranslation.trim().takeIf { language.showPersianTranslation && it.isNotBlank() },
    caption = source.trim().takeIf { it.isNotBlank() }?.let { language.reference(it) },
    appName = language.text("اذکار نور")
)

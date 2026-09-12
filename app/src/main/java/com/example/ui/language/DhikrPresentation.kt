package com.example.ui.language

import com.example.data.model.DhikrItem

/** Source metadata only; never applied to the original Quran or adhkar text. */
fun AppLanguage.reference(source: String): String {
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
        "ذکر" to "ذكر", " و " to " و", "ی" to "ي", "ک" to "ك"
    )
    words.forEach { (fa, ar) -> result = result.replace(fa, ar) }
    return text(result)
}

fun DhikrItem.shareText(language: AppLanguage): String = buildString {
    append(arabicText.trim())
    if (language.showPersianTranslation && persianTranslation.isNotBlank()) {
        append("\n\n"); append(persianTranslation.trim())
    }
    if (source.isNotBlank()) { append("\n\n"); append(language.reference(source.trim())) }
    append("\n\n"); append(language.text("اذکار نور"))
}

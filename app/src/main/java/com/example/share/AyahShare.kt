package com.example.share

import com.example.quran.QuranVerse
import com.example.ui.language.AppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits

/** «سوره مائده، آیه ۳» (Arabic: «سورة المائدة، الآية ٣»). */
fun ayahReference(verse: QuranVerse, language: AppLanguage): String =
    language.text("سوره ${verse.surahName}، آیه ${verse.verseNumber.toPersianDigits()}")

/**
 * Plain-text share of one verse. Uses the Unicode (Tanzil) text, which reads correctly in any app,
 * not the font-specific display encoding used on the Mushaf page.
 */
fun ayahShareText(
    verse: QuranVerse,
    translation: String?,
    translationCredit: String?,
    language: AppLanguage
): String = buildString {
    append("﴿").append(verse.text.trim()).append("﴾")
    append("\n\n").append(ayahReference(verse, language))
    translation?.trim()?.takeIf { it.isNotEmpty() }?.let {
        append("\n\n").append(it)
        translationCredit?.trim()?.takeIf { credit -> credit.isNotEmpty() }?.let { credit -> append("\n(").append(credit).append(")") }
    }
    append("\n\n").append(appShareFooter(language))
}

/** Image card for one verse; [translation] null shares the Arabic only. */
fun ayahShareCard(verse: QuranVerse, translation: String?, language: AppLanguage): ShareCardSpec = ShareCardSpec(
    eyebrow = ayahReference(verse, language),
    headline = "﴿${verse.text.trim()}﴾",
    headlineIsArabic = true,
    body = translation?.trim()?.takeIf { it.isNotEmpty() },
    appName = language.text("اذکار نور")
)

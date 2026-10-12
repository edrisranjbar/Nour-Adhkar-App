package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.layout.size
import com.example.R
import com.example.quran.QuranCorpus
import com.example.quran.QuranPage
import com.example.quran.QuranVerse

private class NormalizedVerses(val corpus: QuranCorpus) {
    val text = Array(corpus.verses.size) { corpus.verses[it].text.normalizeArabic() }
    val surahName = Array(corpus.verses.size) { corpus.verses[it].surahName.normalizeArabic() }
}


@Volatile
private var normalizedVerses: NormalizedVerses? = null

private fun QuranCorpus.normalizedIndex(): NormalizedVerses =
    normalizedVerses?.takeIf { it.corpus === this }
        ?: NormalizedVerses(this).also { normalizedVerses = it }

internal fun QuranCorpus.search(query: String): List<QuranVerse> {
    val normalized = query.normalizeArabic()
    if (normalized.isBlank()) return emptyList()
    val index = normalizedIndex()
    val result = ArrayList<QuranVerse>(40)
    for (i in verses.indices) {
        if (index.text[i].contains(normalized) || index.surahName[i].contains(normalized)) {
            result += verses[i]
            if (result.size == 40) break
        }
    }
    return result
}

private val ArabicMarks = Regex("[ؐ-ًؚ-ٰٟۖ-ۭ]")

internal fun String.normalizeArabic(): String =
    replace(ArabicMarks, "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace('ك', 'ک')
        .replace('ي', 'ی')
        .trim()

internal fun String.toQuranPageNumber(): Int = map { character ->
    when (character) {
        in '۰'..'۹' -> '0' + (character - '۰')
        in '٠'..'٩' -> '0' + (character - '٠')
        else -> character
    }
}.joinToString("").toIntOrNull() ?: 0

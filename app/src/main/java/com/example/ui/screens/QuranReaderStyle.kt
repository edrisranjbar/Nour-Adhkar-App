package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.QuranPage
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.example.ui.theme.UthmanicHafs

internal enum class QuranReaderColor(val id: String) {
    Paper("paper"),
    Sepia("sepia"),
    Sage("sage"),
    Night("night");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: Paper
    }
}

internal data class QuranPalette(
    val page: Color,
    val text: Color,
    val accent: Color,
    val header: Color
)


internal const val QURAN_VERSE_FONT_SP = 23f
// ~1.78em: the KFGQPC font's own line box is 1.76em, so tall mark stacks still clear the next line.
internal const val QURAN_VERSE_LINE_HEIGHT_SP = 41f
internal const val SURAH_HEADER_HEIGHT_DP = 112f
internal const val BISMILLAH_FONT_SP = 27f
internal const val BISMILLAH_LINE_HEIGHT_SP = 40f
internal const val BISMILLAH_BOTTOM_PADDING_DP = 14f

internal fun quranVerseStyle(scale: Float, textAlign: TextAlign) = TextStyle(
    fontFamily = UthmanicHafs,
    fontSize = (QURAN_VERSE_FONT_SP * scale).sp,
    lineHeight = (QURAN_VERSE_LINE_HEIGHT_SP * scale).sp,
    textAlign = textAlign
)

internal fun bismillahStyle(scale: Float) = TextStyle(
    fontFamily = UthmanicHafs,
    fontSize = (BISMILLAH_FONT_SP * scale).sp,
    lineHeight = (BISMILLAH_LINE_HEIGHT_SP * scale).sp,
    textAlign = TextAlign.Center
)

/**
 * Largest scale (up to the default size) at which every surah header, bismillah and verse block on
 * the page fits [maxHeightPx]. The text keeps the user's font scale, so large system fonts shrink
 * only as much as the page needs.
 */
internal fun fitQuranPageScale(
    textMeasurer: TextMeasurer,
    density: Density,
    maxWidthPx: Int,
    maxHeightPx: Int,
    sectionTexts: List<AnnotatedString>,
    openingBismillahs: List<String?>,
    textAlign: TextAlign
): Float {
    if (maxWidthPx <= 0 || maxHeightPx <= 0) return 1f
    val widthConstraints = Constraints(maxWidth = maxWidthPx)
    fun pageHeight(scale: Float): Float = with(density) {
        sectionTexts.indices.sumOf { index ->
            var height = 0f
            openingBismillahs[index]?.let { bismillah ->
                height += (SURAH_HEADER_HEIGHT_DP * scale).dp.toPx()
                if (bismillah.isNotBlank()) {
                    height += textMeasurer.measure(bismillah, bismillahStyle(scale), constraints = widthConstraints)
                        .size.height
                    height += (BISMILLAH_BOTTOM_PADDING_DP * scale).dp.toPx()
                }
            }
            height += textMeasurer.measure(
                sectionTexts[index],
                quranVerseStyle(scale, textAlign),
                constraints = widthConstraints
            ).size.height
            height.toDouble()
        }.toFloat()
    }
    if (pageHeight(1f) <= maxHeightPx) return 1f
    var low = 0.3f
    var high = 1f
    repeat(10) {
        val mid = (low + high) / 2
        if (pageHeight(mid) <= maxHeightPx) low = mid else high = mid
    }
    return low
}

internal fun QuranReaderColor.palette(): QuranPalette = when (this) {
    QuranReaderColor.Paper -> QuranPalette(
        page = Color(0xFFFFFCF3), text = Color(0xFF29241F), accent = Color(0xFF7A5B17), header = Color(0xFF6F6557)
    )
    QuranReaderColor.Sepia -> QuranPalette(
        page = Color(0xFFF3E6CD), text = Color(0xFF3A2A1A), accent = Color(0xFF8B5A2B), header = Color(0xFF74543A)
    )
    QuranReaderColor.Sage -> QuranPalette(
        page = Color(0xFFE6EFE6), text = Color(0xFF1F3126), accent = Color(0xFF3E7151), header = Color(0xFF526A58)
    )
    QuranReaderColor.Night -> QuranPalette(
        page = Color(0xFF151E30), text = Color(0xFFF9F0E4), accent = Color(0xFFC9A65A), header = Color(0xFFD7B879)
    )
}

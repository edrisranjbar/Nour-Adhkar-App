package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.QuranPage
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.example.quran.QuranVerse
import com.example.quran.QuranKhatmGoal
import com.example.quran.QuranKhatmPlan
import com.example.ui.theme.UthmanicHafs
import com.example.ui.util.toPersianDigits

@Composable
internal fun QuranPageView(
    page: QuranPage,
    palette: QuranPalette,
    labels: QuranLabels,
    highlights: Map<String, String>,
    notes: Map<String, String>,
    playingVerseId: String?,
    searchFocusedVerseId: String?,
    focusedSurahNumber: Int?,
    onOpenSurahPicker: () -> Unit,
    onOpenPagePicker: () -> Unit,
    onSurahFocused: () -> Unit,
    onVerseSelected: (QuranVerse) -> Unit
) {
    val verseById = remember(page) { page.verses.associateBy { it.id } }
    val surahSections = remember(page) {
        page.verses.fold(mutableListOf<MutableList<QuranVerse>>()) { sections, verse ->
            val current = sections.lastOrNull()
            if (current == null || current.last().surahNumber != verse.surahNumber) {
                sections += mutableListOf(verse)
            } else {
                current += verse
            }
            sections
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuranHeaderSelector(
                text = page.verses.map { it.surahName }.distinct().joinToString(" · "),
                contentDescription = labels.goToSurah,
                palette = palette,
                onClick = onOpenSurahPicker,
                modifier = Modifier.weight(1f)
            )
            QuranHeaderSelector(
                text = "${labels.page} ${page.number.toPersianDigits()}",
                contentDescription = labels.goToPage,
                palette = palette,
                onClick = onOpenPagePicker
            )
        }
        HorizontalDivider(color = palette.header.copy(alpha = 0.35f))
        // A mushaf page is one fixed page: scale the text to fit the available height instead of scrolling.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val textAlign = if (page.number <= 2) TextAlign.Center else TextAlign.Justify
            // Temporary focus changes only span colors; keep page fitting independent of it.
            val sectionTexts = remember(surahSections, highlights, notes, palette) {
                surahSections.map { it.asMushafText(highlights, notes, palette, playingVerseId = null) }
            }
            val pageHasPlayingVerse = playingVerseId != null && playingVerseId in verseById
            val pageHasSearchFocus = searchFocusedVerseId != null && searchFocusedVerseId in verseById
            val displayTexts = remember(sectionTexts, pageHasPlayingVerse, playingVerseId, searchFocusedVerseId) {
                if (!pageHasPlayingVerse && !pageHasSearchFocus) sectionTexts
                else surahSections.map { it.asMushafText(highlights, notes, palette, playingVerseId, searchFocusedVerseId) }
            }
            val openingBismillahs = surahSections.map { verses ->
                verses.first().takeIf { it.verseNumber == 1 }?.let { it.bismillah.orEmpty() }
            }
            val maxWidthPx = constraints.maxWidth
            val maxHeightPx = constraints.maxHeight
            val scale = remember(sectionTexts, openingBismillahs, maxWidthPx, maxHeightPx, density, textAlign) {
                fitQuranPageScale(
                    textMeasurer = textMeasurer,
                    density = density,
                    maxWidthPx = maxWidthPx,
                    maxHeightPx = maxHeightPx,
                    sectionTexts = sectionTexts,
                    openingBismillahs = openingBismillahs,
                    textAlign = textAlign
                )
            }
            LaunchedEffect(focusedSurahNumber) {
                if (focusedSurahNumber != null && surahSections.any { it.first().surahNumber == focusedSurahNumber }) {
                    onSurahFocused()
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = if (page.number <= 2) Arrangement.Center else Arrangement.Top
            ) {
                surahSections.forEachIndexed { index, verses ->
                    val openingVerse = verses.first()
                    if (openingVerse.verseNumber == 1) {
                        SurahOpeningHeader(
                            surahName = openingVerse.surahName,
                            bismillah = openingVerse.bismillah,
                            palette = palette,
                            scale = scale
                        )
                    }
                    val sectionText = displayTexts[index]
                    ClickableText(
                        text = sectionText,
                        modifier = Modifier.fillMaxWidth(),
                        style = quranVerseStyle(scale, textAlign).copy(color = palette.text),
                        onClick = { offset ->
                            sectionText.getStringAnnotations(tag = VERSE_TAG, start = offset, end = offset)
                                .firstOrNull()
                                ?.let { annotation -> verseById[annotation.item]?.let(onVerseSelected) }
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun QuranHeaderSelector(
    text: String,
    contentDescription: String,
    palette: QuranPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = palette.header,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = palette.header
        )
    }
}

@Composable
internal fun SurahOpeningHeader(
    surahName: String,
    bismillah: String?,
    palette: QuranPalette,
    scale: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((SURAH_HEADER_HEIGHT_DP * scale).dp)
            .padding(top = (4 * scale).dp, bottom = (10 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.quran_surah_ornament),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Text(
            text = "سُورَةُ $surahName",
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-3).dp),
            fontFamily = UthmanicHafs,
            fontSize = (18 * scale).sp,
            lineHeight = (24 * scale).sp,
            color = palette.text,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
    if (!bismillah.isNullOrBlank()) {
        Text(
            text = bismillah,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (BISMILLAH_BOTTOM_PADDING_DP * scale).dp),
            style = bismillahStyle(scale),
            color = palette.text
        )
    }
}

internal const val VERSE_TAG = "quran_verse"

internal fun List<QuranVerse>.asMushafText(
    highlights: Map<String, String>,
    notes: Map<String, String>,
    palette: QuranPalette,
    playingVerseId: String?,
    searchFocusedVerseId: String? = null
): AnnotatedString = buildAnnotatedString {
    forEach { verse ->
        pushStringAnnotation(tag = VERSE_TAG, annotation = verse.id)
        val highlight = highlightChoices.firstOrNull { it.id == highlights[verse.id] }?.color
        // Search focus must remain visible even while this verse is being recited.
        val background = when {
            verse.id == searchFocusedVerseId -> palette.accent.copy(alpha = 0.35f)
            verse.id == playingVerseId -> palette.accent.copy(alpha = 0.22f)
            highlight != null -> highlight.copy(alpha = 0.5f)
            else -> Color.Transparent
        }
        withStyle(
            SpanStyle(
                background = background,
                textDecoration = if (notes[verse.id].isNullOrBlank()) null else TextDecoration.Underline
            )
        ) {
            append(verse.displayText)
            // The KFGQPC font draws Arabic-Indic digits inside its own ayah ornament, so no U+06DD here.
            append('\u00A0')
            withStyle(SpanStyle(color = palette.accent)) {
                append(verse.verseNumber.toArabicIndicDigits())
            }
            append(' ')
        }
        pop()
    }
}

@Composable
internal fun QuranReaderAppBarTitle(
    title: String,
    khatmGoal: QuranKhatmGoal?,
    khatmPlan: QuranKhatmPlan?,
    khatmLabels: QuranKhatmLabels,
    onKhatmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (khatmGoal == null || khatmPlan == null) {
        Text(
            text = title,
            modifier = modifier,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFDF8EF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }
    val progressText = if (khatmGoal.paused) khatmLabels.paused else khatmLabels.percent(khatmPlan.progress)
    val summary = "${khatmLabels.compactTitle(khatmPlan)} · $progressText"
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = khatmLabels.goalTitle, onClick = onKhatmClick),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFDF8EF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE9C46A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun Int.toArabicIndicDigits(): String =
    toString().map { digit -> '\u0660' + (digit - '0') }.joinToString("")

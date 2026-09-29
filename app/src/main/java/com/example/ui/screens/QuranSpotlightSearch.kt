package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.quran.QuranCorpus
import com.example.quran.QuranSurah
import com.example.quran.QuranVerse
import com.example.ui.util.toPersianDigits

private val SpotlightAccent = Color(0xFFC9A65A)

/**
 * Spotlight-style Quran search: a dimmed overlay with a floating, auto-focused field.
 * Surah names match first (jump to the surah), then verse text matches.
 */
@Composable
internal fun QuranSpotlightSearch(
    corpus: QuranCorpus,
    arabic: Boolean,
    quranFont: FontFamily,
    searchVerses: (String) -> List<QuranVerse>,
    normalize: (String) -> String,
    onDismiss: () -> Unit,
    onSurahSelected: (QuranSurah) -> Unit,
    onVerseSelected: (QuranVerse) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    val trimmed = query.trim()
    val surahMatches = remember(trimmed) {
        if (trimmed.isEmpty()) emptyList() else {
            val n = normalize(trimmed)
            val number = trimmed.toQuranNumberOrNull()
            corpus.surahs.filter { normalize(it.name).contains(n) || it.number == number }.take(6)
        }
    }
    val verseMatches = remember(trimmed) { if (trimmed.length < 2) emptyList() else searchVerses(trimmed) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .statusBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visibleState = appear,
                enter = fadeIn() + slideInVertically { -it / 6 }
            ) {
                Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        // Swallow taps so only the scrim dismisses.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpotlightField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = if (arabic) "ابحث عن سورة أو آية…" else "جست‌وجوی سوره یا آیه…",
                        clearLabel = if (arabic) "مسح" else "پاک کردن",
                        focusRequester = focusRequester,
                        onClose = onDismiss
                    )
                    if (trimmed.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 12.dp
                        ) {
                            if (surahMatches.isEmpty() && verseMatches.isEmpty()) {
                                Text(
                                    if (trimmed.length < 2 && surahMatches.isEmpty()) {
                                        if (arabic) "اكتب حرفين على الأقل" else "دست‌کم دو حرف بنویسید"
                                    } else if (arabic) "لا توجد نتائج" else "نتیجه‌ای پیدا نشد",
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 520.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    if (surahMatches.isNotEmpty()) {
                                        item { SectionLabel(if (arabic) "السور" else "سوره‌ها") }
                                        items(surahMatches, key = { "s${it.number}" }) { surah ->
                                            SurahResult(surah, arabic) { onSurahSelected(surah) }
                                        }
                                    }
                                    if (verseMatches.isNotEmpty()) {
                                        item {
                                            SectionLabel(
                                                (if (arabic) "الآيات" else "آیه‌ها") +
                                                    " · " + verseMatches.size.toPersianDigits() +
                                                    if (verseMatches.size >= 40) "+" else ""
                                            )
                                        }
                                        items(verseMatches, key = { "v${it.id}" }) { verse ->
                                            VerseResult(verse, arabic, quranFont) { onVerseSelected(verse) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    focusRequester: FocusRequester,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = SpotlightAccent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(SpotlightAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            IconButton(onClick = { if (value.isNotEmpty()) onValueChange("") else onClose() }) {
                Icon(Icons.Default.Close, contentDescription = clearLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = SpotlightAccent
    )
}

@Composable
private fun SurahResult(surah: QuranSurah, arabic: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(SpotlightAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(surah.number.toPersianDigits(), fontWeight = FontWeight.Bold, color = SpotlightAccent)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(surah.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (arabic) "${surah.verseCount.toPersianDigits()} آية · الصفحة ${surah.firstPage.toPersianDigits()}"
                else "${surah.verseCount.toPersianDigits()} آیه · صفحه ${surah.firstPage.toPersianDigits()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun VerseResult(verse: QuranVerse, arabic: Boolean, quranFont: FontFamily, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            (if (arabic) "سورة " else "سوره ") + verse.surahName + " · " +
                (if (arabic) "آية " else "آیه ") + verse.verseNumber.toPersianDigits() + " · " +
                (if (arabic) "ص " else "ص ") + verse.pageNumber.toPersianDigits(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(4.dp))
        Text(
            verse.displayText,
            fontFamily = quranFont,
            fontSize = 19.sp,
            lineHeight = 32.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

private fun String.toQuranNumberOrNull(): Int? =
    map { c -> if (c in '۰'..'۹') '0' + (c - '۰') else if (c in '٠'..'٩') '0' + (c - '٠') else c }
        .joinToString("").toIntOrNull()

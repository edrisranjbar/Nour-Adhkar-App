package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.ui.language.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.QuranTafsir
import com.example.quran.QuranTafsirs
import com.example.quran.TafsirPassage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import com.example.quran.QuranVerse
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class HighlightChoice(val id: String, val color: Color)

internal val highlightChoices = listOf(
    HighlightChoice("gold", Color(0xFFFFD166)),
    HighlightChoice("mint", Color(0xFF9EE7C5)),
    HighlightChoice("rose", Color(0xFFF6A6B2)),
    HighlightChoice("sky", Color(0xFF9FD7FF))
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
internal fun VerseActionsSheet(
    verse: QuranVerse,
    labels: QuranLabels,
    /** null while loading; blank if unavailable. */
    translationText: String?,
    translationCredit: String,
    showTafsir: Boolean,
    onShowTafsirChange: (Boolean) -> Unit,
    tafsir: QuranTafsir,
    tafsirOptions: List<QuranTafsir>,
    onTafsirSelected: (QuranTafsir) -> Unit,
    currentHighlight: String?,
    currentNote: String?,
    onDismiss: () -> Unit,
    onHighlightSelected: (HighlightChoice?) -> Unit,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    onPlayFromHere: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = verse.verseNumber.toPersianDigits(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "${labels.surah} ${verse.surahName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${labels.verse} ${verse.verseNumber.toPersianDigits()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // The sheet shows the verse's meaning; the Arabic text is already on the page behind it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(labels.translation to false, labels.tafsir to true).forEach { (label, isTafsir) ->
                    val selected = showTafsir == isTafsir
                    Surface(
                        onClick = { onShowTafsirChange(isTafsir) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            if (showTafsir) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tafsirOptions.forEach { option ->
                        FilterChip(
                            selected = option == tafsir,
                            onClick = { onTafsirSelected(option) },
                            label = { Text(option.title) }
                        )
                    }
                }
            }

            val context = LocalContext.current
            val tafsirState by produceState<TafsirState>(TafsirState.Loading, showTafsir, tafsir, verse.id) {
                if (!showTafsir) return@produceState
                value = TafsirState.Loading
                value = runCatching {
                    withContext(Dispatchers.IO) {
                        QuranTafsirs.passage(context, tafsir, verse.surahNumber, verse.verseNumber)
                    }
                }.getOrNull()?.let { TafsirState.Ready(it) } ?: TafsirState.Missing
            }
            val maxTextHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp
            val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(18.dp),
                color = cardColor
            ) {
                // New scroll position whenever the shown text changes.
                key(showTafsir, tafsir, verse.id) {
                    val textScroll = rememberScrollState()
                    Box {
                        Column(
                            modifier = Modifier
                                .heightIn(max = maxTextHeight)
                                .verticalScroll(textScroll)
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            val loading = if (showTafsir) tafsirState is TafsirState.Loading else translationText == null
                            val body: String? = if (showTafsir) {
                                (tafsirState as? TafsirState.Ready)?.passage?.text
                            } else {
                                translationText?.takeIf { it.isNotBlank() }
                            }
                            when {
                                loading -> CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .align(Alignment.CenterHorizontally),
                                    strokeWidth = 2.dp
                                )
                                body == null -> Text(
                                    text = if (showTafsir) labels.tafsirUnavailable else labels.translationUnavailable,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                else -> {
                                    val passage = (tafsirState as? TafsirState.Ready)?.passage
                                    if (showTafsir && passage != null) {
                                        val scope = when {
                                            passage.previousOnly -> labels.tafsirOfPrevious(passage)
                                            passage.fromAyah != passage.toAyah -> labels.tafsirOfRange(passage)
                                            else -> null
                                        }
                                        scope?.let {
                                            Text(
                                                text = it,
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    SelectionContainer {
                                        Text(
                                            text = body,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 30.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        if (textScroll.canScrollForward) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, cardColor)))
                            )
                        }
                    }
                }
            }
            Text(
                text = if (showTafsir) tafsir.credit else translationCredit,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 18.dp, bottom = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = labels.highlight,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    highlightChoices.forEach { choice ->
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { onHighlightSelected(choice) },
                            color = choice.color,
                            shape = CircleShape,
                            border = if (choice.id == currentHighlight) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                            } else null
                        ) {
                            if (choice.id == currentHighlight) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(7.dp),
                                    tint = Color(0xFF29241F)
                                )
                            }
                        }
                    }
                    if (currentHighlight != null) {
                        IconButton(onClick = { onHighlightSelected(null) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = labels.removeHighlight)
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.Button(onClick = onPlayFromHere) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(labels.playFromHere, maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(labels.shareVerse, maxLines = 1, softWrap = false)
                }
                OutlinedButton(onClick = onEditNote) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (currentNote.isNullOrBlank()) labels.addNote else labels.editNote, maxLines = 1, softWrap = false)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

internal sealed interface TafsirState {
    data object Loading : TafsirState
    data object Missing : TafsirState
    data class Ready(val passage: TafsirPassage) : TafsirState
}

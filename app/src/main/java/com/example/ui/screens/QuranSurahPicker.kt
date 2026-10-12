package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.quran.QuranPage
import com.example.quran.QuranSurah
import androidx.compose.runtime.key
import com.example.ui.theme.UthmanicHafs
import com.example.ui.util.toPersianDigits

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SurahPickerSheet(
    surahs: List<QuranSurah>,
    currentSurahNumber: Int,
    labels: QuranLabels,
    onDismiss: () -> Unit,
    onSurahSelected: (QuranSurah) -> Unit
) {
    var surahQuery by remember { mutableStateOf("") }
    val normalizedQuery = surahQuery.normalizeArabic()
    val numericQuery = surahQuery.toQuranPageNumber()
    val filteredSurahs = remember(surahQuery, surahs) {
        if (normalizedQuery.isBlank()) {
            surahs
        } else {
            surahs.filter { surah ->
                surah.name.normalizeArabic().contains(normalizedQuery) ||
                    surah.number == numericQuery
            }
        }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentSurahNumber - 1).coerceIn(surahs.indices)
    )

    LaunchedEffect(surahQuery) {
        if (filteredSurahs.isNotEmpty()) {
            listState.scrollToItem(
                if (surahQuery.isBlank()) (currentSurahNumber - 1).coerceIn(surahs.indices) else 0
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = labels.chooseSurah,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = surahQuery,
                onValueChange = { surahQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(labels.searchSurah) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (surahQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { surahQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = labels.clearSearch)
                        }
                    }
                } else null
            )
            Spacer(Modifier.height(12.dp))
            if (filteredSurahs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(labels.noSurahResults, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredSurahs, key = QuranSurah::number) { surah ->
                        val selected = surah.number == currentSurahNumber
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSurahSelected(surah) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = surah.number.toPersianDigits(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = surah.name,
                                        fontFamily = UthmanicHafs,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = labels.surahMeta(surah),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Text(
                                        text = labels.current,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

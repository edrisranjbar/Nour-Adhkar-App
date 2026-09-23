package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.quran.QuranCorpus
import com.example.quran.QuranKhatmPlanner
import com.example.quran.QuranKhatmRepository
import com.example.quran.QuranPage
import com.example.quran.QuranRepository
import com.example.quran.QuranSurah
import com.example.quran.QuranVerse
import com.example.notifications.AdhkarNotificationManager
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.theme.AmiriQuran
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class QuranReaderColor(val id: String) {
    Paper("paper"),
    Sepia("sepia"),
    Sage("sage"),
    Night("night");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: Paper
    }
}

private data class QuranPalette(
    val page: Color,
    val text: Color,
    val accent: Color,
    val header: Color
)

private data class HighlightChoice(val id: String, val color: Color)

private val highlightChoices = listOf(
    HighlightChoice("gold", Color(0xFFFFD166)),
    HighlightChoice("mint", Color(0xFF9EE7C5)),
    HighlightChoice("rose", Color(0xFFF6A6B2)),
    HighlightChoice("sky", Color(0xFF9FD7FF))
)

@Composable
fun QuranScreen(
    innerPadding: PaddingValues,
    onNavigateHome: () -> Unit,
    requestedPage: Int? = null,
    onRequestedPageConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val language = LocalAppLanguage.current
    val prefs = remember(context) { PreferenceRepository(context) }
    val khatmRepository = remember(context) { QuranKhatmRepository(context) }
    val notificationManager = remember(context) { AdhkarNotificationManager(context) }
    var corpus by remember { mutableStateOf<QuranCorpus?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var noteVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var readerColor by remember { mutableStateOf(QuranReaderColor.fromId(prefs.getQuranReaderColor())) }
    var highlights by remember { mutableStateOf(prefs.getQuranHighlights()) }
    var notes by remember { mutableStateOf(prefs.getQuranNotes()) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var colorDialogOpen by remember { mutableStateOf(false) }
    var goToSurahSheetOpen by remember { mutableStateOf(false) }
    var goToPageDialogOpen by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    var focusedSurahNumber by remember { mutableStateOf<Int?>(null) }
    var activeSurahNumber by remember { mutableStateOf<Int?>(null) }
    var khatmGoal by remember { mutableStateOf(khatmRepository.getGoal()) }
    var khatmLogs by remember { mutableStateOf(khatmRepository.getDailyLogs()) }
    var khatmSetupOpen by remember { mutableStateOf(false) }
    var khatmDetailsOpen by remember { mutableStateOf(false) }
    var cancelKhatmConfirmationOpen by remember { mutableStateOf(false) }
    var khatmCompletedDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        corpus = withContext(Dispatchers.Default) { QuranRepository.load(context) }
    }

    val loadedCorpus = corpus
    if (loadedCorpus == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = prefs.getQuranLastReadPage() - 1,
        pageCount = { QuranRepository.PAGE_COUNT }
    )
    val palette = readerColor.palette()
    val labels = QuranLabels(language)
    val khatmLabels = QuranKhatmLabels(language)
    val searchResults = remember(query, loadedCorpus) {
        loadedCorpus.search(query)
    }
    val pageFirstSurahNumber = loadedCorpus.pages
        .getOrNull(pagerState.currentPage)
        ?.verses
        ?.firstOrNull()
        ?.surahNumber
        ?: 1
    val currentSurahNumber = activeSurahNumber ?: pageFirstSurahNumber
    val khatmPlan = khatmGoal?.let { QuranKhatmPlanner.plan(it) }

    LaunchedEffect(requestedPage, pagerState) {
        requestedPage?.takeIf { it in 1..QuranRepository.PAGE_COUNT }?.let { page ->
            activeSurahNumber = null
            focusedSurahNumber = null
            pagerState.scrollToPage(page - 1)
            onRequestedPageConsumed()
        }
    }

    LaunchedEffect(pagerState, loadedCorpus) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                prefs.setQuranLastReadPage(page + 1)
                val surahsOnPage = loadedCorpus.pages[page].verses.map(QuranVerse::surahNumber).toSet()
                val activeSurah = activeSurahNumber
                if (activeSurah == null || activeSurah !in surahsOnPage) {
                    activeSurahNumber = surahsOnPage.firstOrNull()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            color = Color(0xFF4D3524),
            tonalElevation = 0.dp
        ) {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                    singleLine = true,
                    placeholder = { Text(labels.search, color = Color(0xFFF5EDE2)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF5EDE2)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            query = ""
                            searchOpen = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = labels.clearSearch, tint = Color(0xFFF5EDE2))
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFDF8EF),
                        unfocusedTextColor = Color(0xFFFDF8EF),
                        focusedBorderColor = Color(0xFFC9A65A),
                        unfocusedBorderColor = Color(0xFFAD937B),
                        cursorColor = Color(0xFFC9A65A)
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = labels.backToHome,
                            tint = Color(0xFFF5EDE2)
                        )
                    }
                    Text(
                        text = labels.readerTitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFDF8EF)
                    )
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Default.Search, contentDescription = labels.search, tint = Color(0xFFF5EDE2))
                    }
                Box {
                    IconButton(onClick = { moreMenuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = labels.more, tint = Color(0xFFF5EDE2))
                    }
                    DropdownMenu(expanded = moreMenuOpen, onDismissRequest = { moreMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(labels.goToSurah) },
                            onClick = {
                                moreMenuOpen = false
                                goToSurahSheetOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(labels.goToPage) },
                            onClick = {
                                pageInput = (pagerState.currentPage + 1).toString()
                                moreMenuOpen = false
                                goToPageDialogOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(khatmLabels.menuTitle) },
                            onClick = {
                                moreMenuOpen = false
                                if (khatmGoal == null) khatmSetupOpen = true else khatmDetailsOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(labels.pageColor) },
                            onClick = {
                                moreMenuOpen = false
                                colorDialogOpen = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(labels.textSource) },
                            onClick = {
                                moreMenuOpen = false
                                uriHandler.openUri("https://tanzil.net")
                            }
                        )
                    }
                }
                }
            }
        }

        val visibleGoal = khatmGoal
        val visiblePlan = khatmPlan
        if (visibleGoal != null && visiblePlan != null) {
            QuranKhatmProgressStrip(
                goal = visibleGoal,
                plan = visiblePlan,
                labels = khatmLabels,
                onClick = { khatmDetailsOpen = true }
            )
        }

        if (query.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF202A3C),
                shadowElevation = 4.dp
            ) {
                if (searchResults.isEmpty()) {
                    Text(
                        text = labels.noResults,
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 210.dp)) {
                        items(searchResults, key = { it.id }) { verse ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeSurahNumber = verse.surahNumber
                                        scope.launch { pagerState.scrollToPage(verse.pageNumber - 1) }
                                        query = ""
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "${labels.surah} ${verse.surahName} · ${labels.verse} ${verse.verseNumber.toPersianDigits()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accent
                                )
                                Text(
                                    text = verse.text,
                                    fontFamily = AmiriQuran,
                                    fontSize = 18.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 8.dp),
            pageSpacing = 10.dp,
            key = { it }
        ) { index ->
            QuranPageView(
                page = loadedCorpus.pages[index],
                palette = palette,
                labels = labels,
                highlights = highlights,
                notes = notes,
                focusedSurahNumber = focusedSurahNumber,
                onOpenSurahPicker = { goToSurahSheetOpen = true },
                onOpenPagePicker = {
                    pageInput = (pagerState.currentPage + 1).toString()
                    goToPageDialogOpen = true
                },
                onSurahFocused = { focusedSurahNumber = null },
                onVerseSelected = { selectedVerse = it }
            )
        }
    }

    if (goToSurahSheetOpen) {
        SurahPickerSheet(
            surahs = loadedCorpus.surahs,
            currentSurahNumber = currentSurahNumber,
            labels = labels,
            onDismiss = { goToSurahSheetOpen = false },
            onSurahSelected = { surah ->
                activeSurahNumber = surah.number
                goToSurahSheetOpen = false
                scope.launch {
                    pagerState.scrollToPage(surah.firstPage - 1)
                    focusedSurahNumber = surah.number
                }
            }
        )
    }

    if (goToPageDialogOpen) {
        val requestedPage = pageInput.toQuranPageNumber()
        AlertDialog(
            onDismissRequest = { goToPageDialogOpen = false },
            title = { Text(labels.goToPage) },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(labels.pageRange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = requestedPage in 1..QuranRepository.PAGE_COUNT,
                    onClick = {
                        activeSurahNumber = null
                        focusedSurahNumber = null
                        scope.launch { pagerState.scrollToPage(requestedPage - 1) }
                        goToPageDialogOpen = false
                    }
                ) { Text(labels.go) }
            },
            dismissButton = {
                TextButton(onClick = { goToPageDialogOpen = false }) { Text(labels.cancel) }
            }
        )
    }

    if (colorDialogOpen) {
        AlertDialog(
            onDismissRequest = { colorDialogOpen = false },
            title = { Text(labels.pageColor) },
            text = {
                Column {
                    QuranReaderColor.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    readerColor = option
                                    prefs.setQuranReaderColor(option.id)
                                    colorDialogOpen = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = readerColor == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(labels.colorName(option))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { colorDialogOpen = false }) { Text(labels.cancel) }
            }
        )
    }

    if (khatmSetupOpen) {
        QuranKhatmSetupSheet(
            currentPage = pagerState.currentPage + 1,
            existingGoal = khatmGoal,
            labels = khatmLabels,
            onDismiss = { khatmSetupOpen = false },
            onSave = { days, startPage, reminderEnabled, reminderTime ->
                khatmGoal = if (khatmGoal == null) {
                    khatmRepository.createGoal(days, startPage, reminderEnabled, reminderTime)
                } else {
                    khatmRepository.updateGoal(days, reminderEnabled, reminderTime)
                }
                khatmLogs = khatmRepository.getDailyLogs()
                notificationManager.scheduleReminders()
                khatmSetupOpen = false
                khatmDetailsOpen = true
            }
        )
    }

    val detailsGoal = khatmGoal
    val detailsPlan = khatmPlan
    if (khatmDetailsOpen && detailsGoal != null && detailsPlan != null) {
        QuranKhatmDetailsSheet(
            goal = detailsGoal,
            plan = detailsPlan,
            logs = khatmLogs,
            currentPage = pagerState.currentPage + 1,
            labels = khatmLabels,
            onDismiss = { khatmDetailsOpen = false },
            onContinue = { page ->
                khatmDetailsOpen = false
                scope.launch { pagerState.scrollToPage(page - 1) }
            },
            onRecord = {
                val updated = khatmRepository.recordProgress(pagerState.currentPage + 1)
                khatmGoal = updated
                khatmLogs = khatmRepository.getDailyLogs()
                notificationManager.scheduleReminders()
                if (updated?.isComplete == true) khatmCompletedDialogOpen = true
            },
            onEdit = {
                khatmDetailsOpen = false
                khatmSetupOpen = true
            },
            onPauseChanged = { paused ->
                khatmGoal = khatmRepository.setPaused(paused)
                notificationManager.scheduleReminders()
            },
            onCancel = { cancelKhatmConfirmationOpen = true }
        )
    }

    if (cancelKhatmConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { cancelKhatmConfirmationOpen = false },
            title = { Text(khatmLabels.cancelGoal) },
            text = { Text(if (language == AppLanguage.ARABIC) "سيتم حذف الخطة والسجل اليومي." else "برنامه و گزارش روزانه حذف می‌شوند.") },
            confirmButton = {
                TextButton(onClick = {
                    khatmRepository.clearGoal()
                    khatmGoal = null
                    khatmLogs = emptyList()
                    khatmDetailsOpen = false
                    cancelKhatmConfirmationOpen = false
                    notificationManager.scheduleReminders()
                }) { Text(khatmLabels.cancelGoal, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { cancelKhatmConfirmationOpen = false }) { Text(labels.cancel) }
            }
        )
    }

    if (khatmCompletedDialogOpen) {
        AlertDialog(
            onDismissRequest = { khatmCompletedDialogOpen = false },
            title = { Text(if (language == AppLanguage.ARABIC) "تم ختم القرآن" else "ختم قرآن کامل شد") },
            text = { Text(if (language == AppLanguage.ARABIC) "تقبل الله تلاوتك وبارك لك فيها." else "تلاوت شما قبول باشد و خداوند به آن برکت دهد.") },
            confirmButton = {
                TextButton(onClick = { khatmCompletedDialogOpen = false }) { Text(if (language == AppLanguage.ARABIC) "الحمد لله" else "الحمدلله") }
            }
        )
    }

    selectedVerse?.let { verse ->
        VerseActionsSheet(
            verse = verse,
            labels = labels,
            currentHighlight = highlights[verse.id],
            currentNote = notes[verse.id],
            onDismiss = { selectedVerse = null },
            onHighlightSelected = { choice ->
                prefs.setQuranHighlight(verse.id, choice?.id)
                highlights = prefs.getQuranHighlights()
                selectedVerse = null
            },
            onEditNote = {
                selectedVerse = null
                noteVerse = verse
            }
        )
    }

    noteVerse?.let { verse ->
        var draft by remember(verse.id) { mutableStateOf(notes[verse.id].orEmpty()) }
        AlertDialog(
            onDismissRequest = { noteVerse = null },
            title = { Text(labels.noteFor(verse)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(labels.writeNote) },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.setQuranNote(verse.id, draft)
                    notes = prefs.getQuranNotes()
                    noteVerse = null
                }) { Text(labels.save) }
            },
            dismissButton = {
                TextButton(onClick = { noteVerse = null }) { Text(labels.cancel) }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SurahPickerSheet(
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
                                        fontFamily = AmiriQuran,
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun QuranPageView(
    page: QuranPage,
    palette: QuranPalette,
    labels: QuranLabels,
    highlights: Map<String, String>,
    notes: Map<String, String>,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            surahSections.forEach { verses ->
                val openingVerse = verses.first()
                val bringIntoViewRequester = remember(openingVerse.surahNumber) {
                    BringIntoViewRequester()
                }
                val shouldFocus = focusedSurahNumber == openingVerse.surahNumber &&
                    openingVerse.verseNumber == 1
                LaunchedEffect(shouldFocus) {
                    if (shouldFocus) {
                        bringIntoViewRequester.bringIntoView()
                        onSurahFocused()
                    }
                }
                Column(
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                ) {
                    if (openingVerse.verseNumber == 1) {
                        SurahOpeningHeader(
                            surahName = openingVerse.surahName,
                            bismillah = openingVerse.bismillah,
                            palette = palette
                        )
                    }
                    val sectionText = remember(verses, highlights, notes, palette) {
                        verses.asMushafText(highlights, notes, palette)
                    }
                    ClickableText(
                        text = sectionText,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontFamily = AmiriQuran,
                            fontSize = 23.sp,
                            lineHeight = 45.sp,
                            textAlign = if (page.number <= 2) TextAlign.Center else TextAlign.Justify,
                            color = palette.text
                        ),
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
private fun QuranHeaderSelector(
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
private fun SurahOpeningHeader(
    surahName: String,
    bismillah: String?,
    palette: QuranPalette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .padding(top = 4.dp, bottom = 10.dp),
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
            fontFamily = AmiriQuran,
            fontSize = 23.sp,
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
                .padding(bottom = 14.dp),
            fontFamily = AmiriQuran,
            fontSize = 27.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Center,
            color = palette.text
        )
    }
}

private const val VERSE_TAG = "quran_verse"

private fun List<QuranVerse>.asMushafText(
    highlights: Map<String, String>,
    notes: Map<String, String>,
    palette: QuranPalette
): AnnotatedString = buildAnnotatedString {
    forEach { verse ->
        pushStringAnnotation(tag = VERSE_TAG, annotation = verse.id)
        val highlight = highlightChoices.firstOrNull { it.id == highlights[verse.id] }?.color
        withStyle(
            SpanStyle(
                background = highlight?.copy(alpha = 0.5f) ?: Color.Transparent,
                textDecoration = if (notes[verse.id].isNullOrBlank()) null else TextDecoration.Underline
            )
        ) {
            append(verse.text)
            append(' ')
            withStyle(SpanStyle(color = palette.accent, fontSize = 18.sp)) {
                append("۝${verse.verseNumber.toPersianDigits()} ")
            }
        }
        pop()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VerseActionsSheet(
    verse: QuranVerse,
    labels: QuranLabels,
    currentHighlight: String?,
    currentNote: String?,
    onDismiss: () -> Unit,
    onHighlightSelected: (HighlightChoice?) -> Unit,
    onEditNote: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(labels.noteFor(verse), style = MaterialTheme.typography.titleMedium)
            Text(
                text = verse.text,
                modifier = Modifier.padding(top = 6.dp),
                fontFamily = AmiriQuran,
                fontSize = 20.sp,
                lineHeight = 32.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = labels.highlight,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                highlightChoices.forEach { choice ->
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onHighlightSelected(choice) },
                        color = choice.color,
                        shape = RoundedCornerShape(12.dp),
                        border = if (choice.id == currentHighlight) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                        } else null
                    ) {
                        if (choice.id == currentHighlight) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (currentHighlight != null) {
                    IconButton(onClick = { onHighlightSelected(null) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = labels.removeHighlight)
                    }
                }
            }
            TextButton(
                onClick = onEditNote,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (currentNote.isNullOrBlank()) labels.addNote else labels.editNote)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun QuranCorpus.search(query: String): List<QuranVerse> {
    val normalized = query.normalizeArabic()
    if (normalized.isBlank()) return emptyList()
    return verses.asSequence()
        .filter { verse ->
            verse.text.normalizeArabic().contains(normalized) ||
                verse.surahName.normalizeArabic().contains(normalized)
        }
        .take(40)
        .toList()
}

private fun String.normalizeArabic(): String =
    replace(Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace('ك', 'ک')
        .replace('ي', 'ی')
        .trim()

private fun String.toQuranPageNumber(): Int = map { character ->
    when (character) {
        in '۰'..'۹' -> '0' + (character - '۰')
        in '٠'..'٩' -> '0' + (character - '٠')
        else -> character
    }
}.joinToString("").toIntOrNull() ?: 0

private fun QuranReaderColor.palette(): QuranPalette = when (this) {
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

private class QuranLabels(private val language: AppLanguage) {
    private val arabic get() = language == AppLanguage.ARABIC

    val search get() = if (arabic) "البحث في القرآن" else "جست‌وجو در قرآن"
    val readerTitle get() = if (arabic) "القرآن الكريم" else "قرآن کریم"
    val backToHome get() = if (arabic) "العودة إلى الرئيسية" else "بازگشت به خانه"
    val clearSearch get() = if (arabic) "مسح البحث" else "پاک کردن جست‌وجو"
    val pageColor get() = if (arabic) "لون الصفحة" else "رنگ صفحه"
    val goToSurah get() = if (arabic) "الانتقال إلى سورة" else "رفتن به سوره"
    val goToPage get() = if (arabic) "الانتقال إلى صفحة" else "رفتن به صفحه"
    val chooseSurah get() = if (arabic) "اختر سورة" else "انتخاب سوره"
    val searchSurah get() = if (arabic) "ابحث باسم السورة أو رقمها" else "جست‌وجوی نام یا شماره سوره"
    val noSurahResults get() = if (arabic) "لم يتم العثور على سورة" else "سوره‌ای پیدا نشد"
    val current get() = if (arabic) "الحالية" else "فعلی"
    val pageRange get() = if (arabic) "رقم الصفحة (١–٦٠٤)" else "شماره صفحه (۱ تا ۶۰۴)"
    val go get() = if (arabic) "انتقال" else "برو"
    val more get() = if (arabic) "المزيد" else "بیشتر"
    val textSource get() = if (arabic) "مصدر النص: Tanzil Project" else "منبع متن: Tanzil Project"
    val noResults get() = if (arabic) "لا توجد نتائج" else "نتیجه‌ای پیدا نشد"
    val page get() = if (arabic) "الصفحة" else "صفحه"
    val surah get() = if (arabic) "سورة" else "سوره"
    val verse get() = if (arabic) "آية" else "آیه"
    val highlight get() = if (arabic) "تمييز الآية" else "هایلایت آیه"
    val removeHighlight get() = if (arabic) "إزالة التمييز" else "حذف هایلایت"
    val addNote get() = if (arabic) "إضافة ملاحظة" else "افزودن یادداشت"
    val editNote get() = if (arabic) "ویرایش یادداشت" else "ویرایش یادداشت"
    val writeNote get() = if (arabic) "اكتب ملاحظتك" else "یادداشت خود را بنویسید"
    val save get() = if (arabic) "حفظ" else "ذخیره"
    val cancel get() = if (arabic) "إلغاء" else "لغو"

    fun noteFor(verse: QuranVerse): String =
        "${surah} ${verse.surahName} · ${verse.verseNumber.toPersianDigits()}"

    fun surahMeta(surah: QuranSurah): String =
        if (arabic) {
            "الصفحة ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آية"
        } else {
            "صفحه ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آیه"
        }

    fun colorName(color: QuranReaderColor): String = when (color) {
        QuranReaderColor.Paper -> if (arabic) "ورقي" else "کاغذی"
        QuranReaderColor.Sepia -> if (arabic) "بني فاتح" else "سپیا"
        QuranReaderColor.Sage -> if (arabic) "سبز ملایم" else "سبز ملایم"
        QuranReaderColor.Night -> if (arabic) "ليلي" else "شب"
    }
}

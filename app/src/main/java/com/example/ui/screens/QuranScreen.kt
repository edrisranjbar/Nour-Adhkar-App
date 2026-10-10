package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
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
import com.example.quran.QuranTafsir
import com.example.quran.QuranTafsirs
import com.example.quran.TafsirPassage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.example.quran.QuranTranslation
import com.example.quran.QuranTranslations
import com.example.quran.QuranVerse
import com.example.notifications.AdhkarNotificationManager
import com.example.ui.language.AppLanguage
import com.example.media.QuranAyahPlayer
import com.example.media.QuranAyahReciters
import com.example.media.quranAyahReciter
import com.example.quran.QuranKhatmGoal
import com.example.quran.QuranKhatmPlan
import com.example.ui.language.LocalAppLanguage
import com.example.ui.theme.UthmanicHafs
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
    onOpenAudio: () -> Unit = {},
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
    var translationId by remember(language) { mutableStateOf(prefs.getQuranTranslation(language.code)) }
    val translation = QuranTranslation.forLanguage(language, translationId)
    val translationOptions = QuranTranslation.optionsFor(language)
    var translationDialogOpen by remember { mutableStateOf(false) }
    var tafsirId by remember(language) { mutableStateOf(prefs.getQuranTafsir(language.code)) }
    val tafsir = QuranTafsir.forLanguage(language, tafsirId)
    // Remembered while reading so the sheet reopens on the tab the reader last used.
    var showTafsir by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // null while loading; empty if the bundled file could not be read.
    val translationTexts by androidx.compose.runtime.produceState<Map<String, String>?>(null, translation) {
        value = null
        value = runCatching {
            withContext(Dispatchers.IO) { QuranTranslations.load(context, translation) }
        }.getOrElse { emptyMap() }
    }
    var selectedVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var shareVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var noteVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var readerColor by remember { mutableStateOf(QuranReaderColor.fromId(prefs.getQuranReaderColor())) }
    var highlights by remember { mutableStateOf(prefs.getQuranHighlights()) }
    var notes by remember { mutableStateOf(prefs.getQuranNotes()) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    // Search focus is temporary reading state, separate from the user's saved highlights.
    var searchFocusedVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var colorDialogOpen by remember { mutableStateOf(false) }
    var goToSurahSheetOpen by remember { mutableStateOf(false) }
    var goToPageDialogOpen by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    var focusedSurahNumber by remember { mutableStateOf<Int?>(null) }
    var activeSurahNumber by remember { mutableStateOf<Int?>(null) }
    var khatmGoal by remember { mutableStateOf(khatmRepository.getGoal()) }
    var khatmLogs by remember { mutableStateOf(khatmRepository.getDailyLogs()) }
    val syncRestore by com.example.data.repository.ProgressSyncRepository.restored.collectAsState()
    LaunchedEffect(syncRestore) {
        highlights = prefs.getQuranHighlights()
        notes = prefs.getQuranNotes()
        khatmGoal = khatmRepository.getGoal()
        khatmLogs = khatmRepository.getDailyLogs()
    }
    var khatmSetupOpen by remember { mutableStateOf(false) }
    var khatmDetailsOpen by remember { mutableStateOf(false) }
    var cancelKhatmConfirmationOpen by remember { mutableStateOf(false) }
    var khatmCompletedDialogOpen by remember { mutableStateOf(false) }
    val audioPrefs = remember(context) { context.getSharedPreferences("quran_audio", android.content.Context.MODE_PRIVATE) }
    // The reader recites verse by verse; full-surah listening and downloads live in «قرآن صوتی».
    var ayahReciter by remember {
        mutableStateOf(quranAyahReciter(audioPrefs.getString("ayah_reciter", null), audioPrefs.getString("reciter", null)))
    }
    var reciterMenuOpen by remember { mutableStateOf(false) }
    val ayahState by QuranAyahPlayer.state.collectAsState()
    // Leaving the reader keeps the recitation playing; QuranPlaybackService shows its notification.
    LaunchedEffect(ayahState.notice) {
        ayahState.notice?.let {
            android.widget.Toast.makeText(context, language.text(it), android.widget.Toast.LENGTH_SHORT).show()
            QuranAyahPlayer.clearNotice()
        }
    }
    // Keep the page visible while following a recitation.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(ayahState.active) {
        view.keepScreenOn = ayahState.active
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(ayahState.error) {
        ayahState.error?.let {
            android.widget.Toast.makeText(context, language.text(it), android.widget.Toast.LENGTH_LONG).show()
            QuranAyahPlayer.clearError()
        }
    }

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
    val pageFirstSurahNumber = loadedCorpus.pages
        .getOrNull(pagerState.currentPage)
        ?.verses
        ?.firstOrNull()
        ?.surahNumber
        ?: 1
    val currentSurahNumber = activeSurahNumber ?: pageFirstSurahNumber
    val khatmPlan = khatmGoal?.let { QuranKhatmPlanner.plan(it) }
    val verseCounts = remember(loadedCorpus) { loadedCorpus.surahs.sortedBy { it.number }.map { it.verseCount } }
    val pageByVerse = remember(loadedCorpus) { loadedCorpus.verses.associate { it.id to it.pageNumber } }
    val playFrom: (QuranVerse) -> Unit = { verse ->
        QuranAyahPlayer.play(context, ayahReciter, verseCounts, verse.surahNumber, verse.verseNumber)
    }

    // Turn to the reciting verse's page as playback moves on; a manual swipe holds until the next verse.
    LaunchedEffect(ayahState.verseId) {
        val page = ayahState.verseId?.let(pageByVerse::get) ?: return@LaunchedEffect
        if (pagerState.currentPage != page - 1) pagerState.animateScrollToPage(page - 1)
    }

    LaunchedEffect(requestedPage, pagerState) {
        requestedPage?.takeIf { it in 1..QuranRepository.PAGE_COUNT }?.let { page ->
            searchFocusedVerse = null
            activeSurahNumber = null
            focusedSurahNumber = null
            pagerState.scrollToPage(page - 1)
            onRequestedPageConsumed()
        }
    }

    LaunchedEffect(pagerState, loadedCorpus) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (searchFocusedVerse?.pageNumber?.let { it != page + 1 } == true) {
                    searchFocusedVerse = null
                }
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                    QuranReaderAppBarTitle(
                        title = labels.readerTitle,
                        khatmGoal = khatmGoal,
                        khatmPlan = khatmPlan,
                        khatmLabels = khatmLabels,
                        onKhatmClick = { khatmDetailsOpen = true },
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { reciterMenuOpen = true }) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = if (language == AppLanguage.ARABIC) "اختيار القارئ" else language.text("انتخاب قاری"),
                                tint = Color(0xFFF5EDE2)
                            )
                        }
                        DropdownMenu(expanded = reciterMenuOpen, onDismissRequest = { reciterMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(language.text("قرآن صوتی")) },
                                onClick = { reciterMenuOpen = false; onOpenAudio() }
                            )
                            HorizontalDivider()
                            Text(
                                text = labels.verseByVerseReciters,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            QuranAyahReciters.forEach { item ->
                                val selected = item.id == ayahReciter.id
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (language == AppLanguage.ARABIC) item.arName else item.faName,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    onClick = {
                                        reciterMenuOpen = false
                                        ayahReciter = item
                                        audioPrefs.edit().putString("ayah_reciter", item.id).apply()
                                        // Continue the current verse in the new voice.
                                        QuranAyahPlayer.changeReciter(context, item)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        if (ayahState.active) QuranAyahPlayer.stop()
                        else loadedCorpus.pages[pagerState.currentPage].verses.firstOrNull()?.let(playFrom)
                    }) {
                        Icon(
                            if (ayahState.active) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (ayahState.active) labels.stopRecitation else labels.playPage,
                            tint = Color(0xFFF5EDE2)
                        )
                    }
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
                        if (translationOptions.size > 1) {
                            DropdownMenuItem(
                                text = { Text(labels.translationPicker) },
                                onClick = {
                                    moreMenuOpen = false
                                    translationDialogOpen = true
                                }
                            )
                        }
                    }
                }
                }
                // Khatm progress lives inside the app bar so the reader page keeps the full height.
                val barPlan = khatmPlan
                if (khatmGoal != null && barPlan != null) {
                    LinearProgressIndicator(
                        progress = { barPlan.progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color(0xFFE9C46A),
                        trackColor = Color(0x33F5EDE2),
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = LocalDensity.current
            var playerHeightPx by remember { mutableStateOf(0) }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // While reciting, the page refits above the player instead of hiding its last lines.
                contentPadding = PaddingValues(
                    bottom = innerPadding.calculateBottomPadding() + 8.dp +
                        if (ayahState.active) with(density) { playerHeightPx.toDp() } + 8.dp else 0.dp
                ),
                pageSpacing = 10.dp,
                key = { it }
            ) { index ->
                QuranPageView(
                    page = loadedCorpus.pages[index],
                    palette = palette,
                    labels = labels,
                    highlights = highlights,
                    notes = notes,
                    playingVerseId = ayahState.verseId,
                    searchFocusedVerseId = searchFocusedVerse?.takeIf { it.pageNumber == index + 1 }?.id,
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
            ayahState.track?.let { track ->
                AyahPlayerBar(
                    surahName = loadedCorpus.surahs.firstOrNull { it.number == track.surah }?.name.orEmpty(),
                    track = track,
                    reciterName = if (language == AppLanguage.ARABIC) ayahReciter.arName else ayahReciter.faName,
                    isLoading = ayahState.isLoading,
                    isPlaying = ayahState.isPlaying,
                    labels = labels,
                    onPrevious = { QuranAyahPlayer.previous(context) },
                    onPlayPause = QuranAyahPlayer::togglePlayPause,
                    onNext = { QuranAyahPlayer.next(context) },
                    onStop = QuranAyahPlayer::stop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = innerPadding.calculateBottomPadding() + 8.dp)
                        .onSizeChanged { playerHeightPx = it.height }
                )
            }
        }
    }

    if (goToSurahSheetOpen) {
        SurahPickerSheet(
            surahs = loadedCorpus.surahs,
            currentSurahNumber = currentSurahNumber,
            labels = labels,
            onDismiss = { goToSurahSheetOpen = false },
            onSurahSelected = { surah ->
                searchFocusedVerse = null
                activeSurahNumber = surah.number
                goToSurahSheetOpen = false
                scope.launch {
                    pagerState.scrollToPage(surah.firstPage - 1)
                    focusedSurahNumber = surah.number
                }
            }
        )
    }

    if (searchOpen) {
        QuranSpotlightSearch(
            corpus = loadedCorpus,
            arabic = language == AppLanguage.ARABIC,
            localize = { language.text(it) },
            quranFont = UthmanicHafs,
            searchVerses = { loadedCorpus.search(it) },
            normalize = { it.normalizeArabic() },
            onDismiss = { searchOpen = false },
            onSurahSelected = { surah ->
                searchFocusedVerse = null
                searchOpen = false
                activeSurahNumber = surah.number
                scope.launch {
                    pagerState.scrollToPage(surah.firstPage - 1)
                    focusedSurahNumber = surah.number
                }
            },
            onVerseSelected = { verse ->
                searchOpen = false
                activeSurahNumber = verse.surahNumber
                scope.launch {
                    pagerState.scrollToPage(verse.pageNumber - 1)
                    // Set focus after navigation so the previous page cannot clear the new result.
                    searchFocusedVerse = verse
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
                        searchFocusedVerse = null
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

    if (translationDialogOpen) {
        AlertDialog(
            onDismissRequest = { translationDialogOpen = false },
            title = { Text(labels.translationPicker) },
            text = {
                Column {
                    translationOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    translationId = option.id
                                    prefs.setQuranTranslation(language.code, option.id)
                                    translationDialogOpen = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = translation == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { translationDialogOpen = false }) { Text(labels.cancel) }
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
            text = { Text(if (language == AppLanguage.ARABIC) "سيتم حذف الخطة والسجل اليومي." else language.text("برنامه و گزارش روزانه حذف می‌شوند.")) },
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
            title = { Text(if (language == AppLanguage.ARABIC) "تم ختم القرآن" else language.text("ختم قرآن کامل شد")) },
            text = { Text(if (language == AppLanguage.ARABIC) "تقبل الله تلاوتك وبارك لك فيها." else language.text("تلاوت شما قبول باشد و خداوند به آن برکت دهد.")) },
            confirmButton = {
                TextButton(onClick = { khatmCompletedDialogOpen = false }) { Text(if (language == AppLanguage.ARABIC) "الحمد لله" else language.text("الحمدلله")) }
            }
        )
    }

    selectedVerse?.let { verse ->
        VerseActionsSheet(
            verse = verse,
            labels = labels,
            translationText = translationTexts?.let { it[verse.id].orEmpty() },
            translationCredit = translation.credit,
            showTafsir = showTafsir,
            onShowTafsirChange = { showTafsir = it },
            tafsir = tafsir,
            tafsirOptions = QuranTafsir.optionsFor(language),
            onTafsirSelected = { option ->
                tafsirId = option.id
                prefs.setQuranTafsir(language.code, option.id)
            },
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
            },
            onShare = {
                selectedVerse = null
                shareVerse = verse
            },
            onPlayFromHere = {
                selectedVerse = null
                playFrom(verse)
            }
        )
    }

    shareVerse?.let { verse ->
        AyahShareSheet(
            verse = verse,
            translation = translationTexts?.let { it[verse.id].orEmpty() },
            translationCredit = translation.credit,
            onDismiss = { shareVerse = null }
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

@Composable
private fun QuranPageView(
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
private fun AyahPlayerBar(
    surahName: String,
    track: com.example.media.QuranAyahTrack,
    reciterName: String,
    isLoading: Boolean,
    isPlaying: Boolean,
    labels: QuranLabels,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${labels.surah} $surahName · " +
                        if (track.bismillah) labels.bismillah else "${labels.verse} ${track.ayah.toPersianDigits()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reciterName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Media transport controls keep their left-to-right order in RTL, per Material guidance.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = labels.previousVerse)
                    }
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            androidx.compose.material3.FilledIconButton(onClick = onPlayPause) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) labels.pause else labels.resume
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = labels.nextVerse)
                    }
                }
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = labels.stopRecitation)
            }
        }
    }
}

private const val QURAN_VERSE_FONT_SP = 23f
// ~1.78em: the KFGQPC font's own line box is 1.76em, so tall mark stacks still clear the next line.
private const val QURAN_VERSE_LINE_HEIGHT_SP = 41f
private const val SURAH_HEADER_HEIGHT_DP = 112f
private const val BISMILLAH_FONT_SP = 27f
private const val BISMILLAH_LINE_HEIGHT_SP = 40f
private const val BISMILLAH_BOTTOM_PADDING_DP = 14f

private fun quranVerseStyle(scale: Float, textAlign: TextAlign) = TextStyle(
    fontFamily = UthmanicHafs,
    fontSize = (QURAN_VERSE_FONT_SP * scale).sp,
    lineHeight = (QURAN_VERSE_LINE_HEIGHT_SP * scale).sp,
    textAlign = textAlign
)

private fun bismillahStyle(scale: Float) = TextStyle(
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
private fun fitQuranPageScale(
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

private const val VERSE_TAG = "quran_verse"

private fun List<QuranVerse>.asMushafText(
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun VerseActionsSheet(
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

private class NormalizedVerses(val corpus: QuranCorpus) {
    val text = Array(corpus.verses.size) { corpus.verses[it].text.normalizeArabic() }
    val surahName = Array(corpus.verses.size) { corpus.verses[it].surahName.normalizeArabic() }
}

@Volatile
private var normalizedVerses: NormalizedVerses? = null

private fun QuranCorpus.normalizedIndex(): NormalizedVerses =
    normalizedVerses?.takeIf { it.corpus === this }
        ?: NormalizedVerses(this).also { normalizedVerses = it }

private fun QuranCorpus.search(query: String): List<QuranVerse> {
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

private fun String.normalizeArabic(): String =
    replace(ArabicMarks, "")
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
    /** Arabic wording is written here; other languages localize the Persian source (Dari, Urdu). */
    private fun pick(arabicText: String, persian: String) = if (arabic) arabicText else language.text(persian)

    val search get() = pick("البحث في القرآن", "جست‌وجو در قرآن")
    val readerTitle get() = pick("القرآن الكريم", "قرآن کریم")
    val backToHome get() = pick("العودة إلى الرئيسية", "بازگشت به خانه")
    val clearSearch get() = pick("مسح البحث", "پاک کردن جست‌وجو")
    val pageColor get() = pick("لون الصفحة", "رنگ صفحه")
    val goToSurah get() = pick("الانتقال إلى سورة", "رفتن به سوره")
    val goToPage get() = pick("الانتقال إلى صفحة", "رفتن به صفحه")
    val chooseSurah get() = pick("اختر سورة", "انتخاب سوره")
    val searchSurah get() = pick("ابحث باسم السورة أو رقمها", "جست‌وجوی نام یا شماره سوره")
    val noSurahResults get() = pick("لم يتم العثور على سورة", "سوره‌ای پیدا نشد")
    val current get() = pick("الحالية", "فعلی")
    val pageRange get() = pick("رقم الصفحة (١–٦٠٤)", "شماره صفحه (۱ تا ۶۰۴)")
    val go get() = pick("انتقال", "برو")
    val more get() = pick("المزيد", "بیشتر")
    val noResults get() = pick("لا توجد نتائج", "نتیجه‌ای پیدا نشد")
    val page get() = pick("الصفحة", "صفحه")
    val surah get() = pick("سورة", "سوره")
    val verse get() = pick("آية", "آیه")
    val highlight get() = pick("تمييز الآية", "هایلایت آیه")
    val removeHighlight get() = pick("إزالة التمييز", "حذف هایلایت")
    // Arabic readers get al-Muyassar's plain meaning here, so "المعنى" rather than "الترجمة".
    val translation get() = pick("المعنى", "ترجمه")
    val translationPicker get() = pick("ترجمة الآيات", "ترجمهٔ آیات")
    val tafsir get() = pick("التفسير", "تفسیر")
    val tafsirUnavailable get() = pick("لا يوجد تفسير لهذه الآية في هذا الكتاب.", "این تفسیر برای این آیه متنی ندارد.")

    private fun ayahRange(passage: TafsirPassage): String {
        val from = passage.fromAyah.toPersianDigits()
        val to = passage.toAyah.toPersianDigits()
        return when {
            // Raw Persian here: the callers' pick() localizes the whole phrase, range included.
            passage.fromAyah == passage.toAyah -> if (arabic) "الآية $from" else "آیهٔ $from"
            arabic -> "الآيات $from–$to"
            else -> "آیه‌های $from تا $to"
        }
    }

    fun tafsirOfRange(passage: TafsirPassage): String =
        pick("تفسير ${ayahRange(passage)}", "تفسیر ${ayahRange(passage)}")

    fun tafsirOfPrevious(passage: TafsirPassage): String = pick(
        "لا يفرد هذا التفسير هذه الآية بكلام؛ هذا تفسير ${ayahRange(passage)}:",
        "این تفسیر برای این آیه متن جداگانه‌ای ندارد؛ تفسیر ${ayahRange(passage)}:"
    )

    val translationUnavailable get() = pick("المعنى غير متاح حاليًا.", "ترجمهٔ این آیه در دسترس نیست.")
    val addNote get() = pick("إضافة ملاحظة", "افزودن یادداشت")
    val shareVerse get() = pick("مشاركة الآية", "اشتراک‌گذاری")
    val editNote get() = pick("ویرایش یادداشت", "ویرایش یادداشت")
    val writeNote get() = pick("اكتب ملاحظتك", "یادداشت خود را بنویسید")
    val playFromHere get() = pick("التلاوة من هنا", "پخش از این آیه")
    val playPage get() = pick("تلاوة آية بآية من هذه الصفحة", "پخش آیه‌به‌آیه از این صفحه")
    val stopRecitation get() = pick("إيقاف التلاوة", "توقف تلاوت")
    val verseByVerseReciters get() = pick("التلاوة آية بآية", "قاریان پخش آیه‌به‌آیه")
    val previousVerse get() = pick("الآية السابقة", "آیهٔ قبل")
    val nextVerse get() = pick("الآية التالية", "آیهٔ بعد")
    val pause get() = pick("إيقاف مؤقت", "توقف موقت")
    val resume get() = pick("متابعة", "ادامه")
    val bismillah get() = pick("البسملة", "بسم‌الله")
    val save get() = pick("حفظ", "ذخیره")
    val cancel get() = pick("إلغاء", "لغو")

    fun noteFor(verse: QuranVerse): String =
        "${surah} ${verse.surahName} · ${verse.verseNumber.toPersianDigits()}"

    fun surahMeta(surah: QuranSurah): String = pick(
        "الصفحة ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آية",
        "صفحه ${surah.firstPage.toPersianDigits()} · ${surah.verseCount.toPersianDigits()} آیه"
    )

    fun colorName(color: QuranReaderColor): String = when (color) {
        QuranReaderColor.Paper -> pick("ورقي", "کاغذی")
        QuranReaderColor.Sepia -> pick("بني فاتح", "سپیا")
        QuranReaderColor.Sage -> pick("سبز ملایم", "سبز ملایم")
        QuranReaderColor.Night -> pick("ليلي", "شب")
    }
}

private sealed interface TafsirState {
    data object Loading : TafsirState
    data object Missing : TafsirState
    data class Ready(val passage: TafsirPassage) : TafsirState
}

@Composable
private fun QuranReaderAppBarTitle(
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

private fun Int.toArabicIndicDigits(): String =
    toString().map { digit -> '\u0660' + (digit - '0') }.joinToString("")

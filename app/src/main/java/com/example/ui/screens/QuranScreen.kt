package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.quran.QuranCorpus
import com.example.quran.QuranKhatmPlanner
import com.example.quran.QuranKhatmRepository
import com.example.quran.QuranPage
import com.example.quran.QuranRepository
import com.example.quran.QuranTafsir
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.unit.Density
import com.example.quran.QuranTranslation
import com.example.quran.QuranTranslations
import com.example.quran.QuranVerse
import com.example.notifications.AdhkarNotificationManager
import com.example.ui.language.AppLanguage
import com.example.media.QuranAyahPlayer
import com.example.media.QuranAyahReciters
import com.example.media.quranAyahReciter
import com.example.quran.QuranKhatmPlan
import com.example.ui.language.LocalAppLanguage
import com.example.ui.theme.UthmanicHafs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        QuranGoToPageDialog(
            pageInput = pageInput,
            onPageInputChange = { pageInput = it },
            labels = labels,
            onDismiss = { goToPageDialogOpen = false },
            onGo = { requestedPage ->
                searchFocusedVerse = null
                activeSurahNumber = null
                focusedSurahNumber = null
                scope.launch { pagerState.scrollToPage(requestedPage - 1) }
                goToPageDialogOpen = false
            }
        )
    }

    if (colorDialogOpen) {
        QuranReaderColorDialog(
            selected = readerColor,
            labels = labels,
            onSelect = { option ->
                readerColor = option
                prefs.setQuranReaderColor(option.id)
                colorDialogOpen = false
            },
            onDismiss = { colorDialogOpen = false }
        )
    }

    if (translationDialogOpen) {
        QuranTranslationDialog(
            options = translationOptions,
            selected = translation,
            labels = labels,
            onSelect = { option ->
                translationId = option.id
                prefs.setQuranTranslation(language.code, option.id)
                translationDialogOpen = false
            },
            onDismiss = { translationDialogOpen = false }
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

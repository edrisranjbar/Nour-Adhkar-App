package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.media.*
import com.example.quran.QuranRepository
import com.example.quran.QuranSurah
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.UthmanicHafs
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The app's green accent, as used by the prayer card and streaks, readable in both themes. */
private data class AudioAccent(val color: Color, val onColor: Color)

@Composable
private fun audioAccent(): AudioAccent {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) AudioAccent(Color(0xFFA3D899), Color(0xFF1A1D1B)) else AudioAccent(SunGold, Color.White)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAudioScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val fontScale by viewModel.fontScale.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("quran_audio", Context.MODE_PRIVATE) }
    var reciterId by rememberSaveable { mutableStateOf(prefs.getString("reciter", QuranReciters.first().id)!!) }
    var surahNumber by rememberSaveable { mutableIntStateOf(prefs.getInt("listening_surah", 1).coerceIn(1, 114)) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val reciter = QuranReciters.firstOrNull { it.id == reciterId } ?: QuranReciters.first()
    val reciterName = reciter.displayName(language)
    val state by QuranAudioPlayer.state.collectAsState()
    val current = state.reciterId == reciter.id && state.surah == surahNumber
    val playing = current && state.isPlaying
    val loading = current && state.isLoading
    val downloading = current && state.isDownloading
    var catalogRetry by remember { mutableIntStateOf(0) }
    val surahLoad by produceState<Result<List<QuranSurah>>?>(null, catalogRetry) {
        value = null
        value = runCatching { withContext(Dispatchers.IO) { QuranRepository.load(context).surahs } }
    }
    val surahs = surahLoad?.getOrNull()
    val saved by produceState<Set<Int>>(emptySet(), reciter.id, state.isLoading, state.isDownloading) {
        value = withContext(Dispatchers.IO) {
            val store = QuranAudioStore(context)
            (1..114).filter { store.stored(reciter.id, it) != null }.toSet()
        }
    }
    val surah = surahs?.firstOrNull { it.number == surahNumber }
    var dragging by remember(reciter.id, surahNumber) { mutableStateOf<Float?>(null) }
    val duration = if (current) state.durationMs else 0
    val position = if (current) state.positionMs else 0
    val reduceMotion = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
    fun selectSurah(number: Int) {
        QuranAudioPlayer.stop()
        surahNumber = number.coerceIn(1, 114)
        prefs.edit().putInt("listening_surah", surahNumber).apply()
    }
    fun openPicker(name: String) { query = ""; picker = name }

    DisposableEffect(Unit) { onDispose { QuranAudioPlayer.stop() } }
    LaunchedEffect(playing) {
        while (playing) { QuranAudioPlayer.refreshPosition(); delay(500) }
    }

    val colors = MaterialTheme.colorScheme
    val accent = audioAccent()
    Column(
        Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NowPlayingCard(
            surah = surah,
            surahNumber = surahNumber,
            reciterName = reciterName,
            status = when {
                playing -> AudioStatus.PLAYING
                loading -> AudioStatus.LOADING
                else -> AudioStatus.READY
            },
            isSaved = surahNumber in saved,
            reduceMotion = reduceMotion,
            fontScale = fontScale,
            onChooseSurah = { openPicker("surah") },
            onChooseReciter = { openPicker("reciter") }
        )

        // Media transport keeps its left-to-right order in RTL, per Material guidance.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.semantics { contentDescription = language.text("موقعیت پخش") },
                    value = (dragging ?: position.toFloat()).coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { dragging = it },
                    onValueChangeFinished = { dragging?.let { QuranAudioPlayer.seekTo(it.toInt()) }; dragging = null },
                    valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                    enabled = current && duration > 0 && !loading,
                    colors = SliderDefaults.colors(
                        thumbColor = accent.color,
                        activeTrackColor = accent.color,
                        inactiveTrackColor = accent.color.copy(alpha = 0.18f)
                    )
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(language.text(audioTime((dragging ?: position.toFloat()).toInt())),
                        style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(language.text(audioTime(duration)),
                        style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectSurah(surahNumber - 1) }, enabled = surahNumber > 1) {
                        Icon(Icons.Default.SkipPrevious, language.text("سورهٔ قبل"))
                    }
                    FilledTonalIconButton(
                        onClick = { QuranAudioPlayer.skip(-10_000) },
                        enabled = current && duration > 0 && !loading,
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Default.Replay10, language.text("۱۰ ثانیه عقب")) }
                    PlayButton(
                        playing = playing,
                        loading = loading,
                        downloadPercent = if (downloading) state.downloadPercent else null,
                        enabled = surah != null,
                        accent = accent,
                        onClick = {
                            when {
                                // While fetching, the button cancels; otherwise it plays or pauses.
                                loading -> QuranAudioPlayer.stop()
                                current && state.error == null && duration > 0 -> QuranAudioPlayer.togglePlayPause()
                                else -> QuranAudioPlayer.play(context, reciter, surahNumber)
                            }
                        }
                    )
                    FilledTonalIconButton(
                        onClick = { QuranAudioPlayer.skip(10_000) },
                        enabled = current && duration > 0 && !loading,
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Default.Forward10, language.text("۱۰ ثانیه جلو")) }
                    IconButton(onClick = { selectSurah(surahNumber + 1) }, enabled = surahNumber < 114) {
                        Icon(Icons.Default.SkipNext, language.text("سورهٔ بعد"))
                    }
                }
            }
        }

        // One status line under the controls: an error, download progress, or the offline hint.
        val statusKey = when {
            current && state.error != null -> "error"
            downloading -> "download"
            surahNumber !in saved && !current -> "hint"
            else -> "none"
        }
        AnimatedContent(targetState = statusKey, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "audioStatus") { key ->
            when (key) {
                "error" -> Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = colors.onErrorContainer)
                        Text(language.text(state.error.orEmpty()), Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer)
                        TextButton(onClick = { QuranAudioPlayer.play(context, reciter, surahNumber) }) {
                            Text(language.text("تلاش دوباره"), color = colors.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "download" -> Text(
                    language.text("در حال دریافت تلاوت") +
                        (state.downloadPercent?.let { " · " + language.text("${it.toPersianDigits()}٪") } ?: "…"),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent.color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                "hint" -> Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                    Text(language.text("تلاوت یک‌بار دریافت و برای شنیدن آفلاین ذخیره می‌شود."),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                else -> Spacer(Modifier.height(0.dp))
            }
        }

        if (surahLoad == null) {
            CircularProgressIndicator(Modifier.size(24.dp), color = accent.color, strokeWidth = 2.dp)
        } else if (surahs == null) {
            Text(language.text("فهرست سوره‌ها در دسترس نیست"), color = colors.error)
            TextButton(onClick = { catalogRetry++ }) { Text(language.text("تلاش دوباره")) }
        }
    }

    if (picker in listOf("reciter", "surah")) {
        val savedPerReciter by produceState<Map<String, Int>>(emptyMap(), picker) {
            if (picker == "reciter") value = withContext(Dispatchers.IO) {
                QuranAudioStore(context).list().groupingBy { it.reciterId }.eachCount()
            }
        }
        ModalBottomSheet(onDismissRequest = { picker = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
                Text(language.text(if (picker == "reciter") "انتخاب قاری" else "انتخاب سوره"),
                    Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 12.dp), singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(language.text(if (picker == "reciter") "جست‌وجوی قاری" else "نام یا شمارهٔ سوره")) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, language.text("پاک کردن")) } }
                    } else null)
                // Open the surah list at the current surah rather than at al-Fatiha.
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = if (picker == "surah") (surahNumber - 3).coerceAtLeast(0) else 0
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (picker) {
                        "reciter" -> {
                            val matches = QuranReciters.filter { audioSearch(it.faName + " " + it.arName).contains(audioSearch(query)) }
                            if (matches.isEmpty()) item { EmptyResult(language.text("نتیجه‌ای پیدا نشد")) }
                            items(matches, key = { it.id }) { voice ->
                                val count = savedPerReciter[voice.id] ?: 0
                                AudioChoiceRow(
                                    title = voice.displayName(language),
                                    subtitle = if (count > 0) language.text("${count.toPersianDigits()} سوره ذخیره‌شده") else null,
                                    selected = voice.id == reciter.id,
                                    leading = { ReciterAvatar(voice.displayName(language), 44.dp) }
                                ) {
                                    if (voice.id != reciter.id) {
                                        QuranAudioPlayer.stop(); reciterId = voice.id
                                        prefs.edit().putString("reciter", voice.id).apply()
                                    }
                                    picker = null
                                }
                            }
                        }
                        "surah" -> {
                            val matches = surahs.orEmpty().filter { audioSearch("${it.number} ${it.name}").contains(audioSearch(query)) }
                            if (matches.isEmpty()) item { EmptyResult(language.text("نتیجه‌ای پیدا نشد")) }
                            items(matches, key = { it.number }) { item ->
                                AudioChoiceRow(
                                    title = item.name,
                                    subtitle = language.text("${item.verseCount.toPersianDigits()} آیه · صفحه ${item.firstPage.toPersianDigits()}"),
                                    selected = item.number == surahNumber,
                                    downloaded = item.number in saved,
                                    leading = { SurahNumberBadge(item.number) }
                                ) {
                                    selectSurah(item.number); picker = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    state.mobileConfirmationBytes?.let { bytes ->
        val pendingVoice = QuranReciters.firstOrNull { it.id == state.reciterId }
        val pendingSurah = state.surah
        if (pendingVoice != null && pendingSurah != null) {
            com.example.ui.components.QuranDownloadDialog(
                reciter = pendingVoice, surahNumber = pendingSurah,
                surahName = surahs?.firstOrNull { it.number == pendingSurah }?.name, bytes = bytes,
                onDownload = { QuranAudioPlayer.play(context, pendingVoice, pendingSurah, allowMobile = true) },
                onDismiss = { QuranAudioPlayer.stop() }
            )
        }
    }
}

private enum class AudioStatus { PLAYING, LOADING, READY }

private fun QuranReciter.displayName(language: AppLanguage) = if (language == AppLanguage.ARABIC) arName else faName

/**
 * The surah on the same ornament and Uthmanic face as the Quran reader's surah headers, with the
 * Qari beneath. Both halves open their pickers.
 */
@Composable
private fun NowPlayingCard(
    surah: QuranSurah?,
    surahNumber: Int,
    reciterName: String,
    status: AudioStatus,
    isSaved: Boolean,
    reduceMotion: Boolean,
    fontScale: Float,
    onChooseSurah: () -> Unit,
    onChooseReciter: () -> Unit
) {
    val language = LocalAppLanguage.current
    val colors = MaterialTheme.colorScheme
    val accent = audioAccent()
    Surface(
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SoftBorder),
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.background(Brush.verticalGradient(listOf(colors.secondaryContainer, colors.surface)))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill(status, reduceMotion, accent)
                if (isSaved) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        Icon(Icons.Default.DownloadDone, null, Modifier.size(16.dp), tint = accent.color)
                        Text(language.text("تلاوت ذخیره شده"), style = MaterialTheme.typography.labelMedium, color = accent.color)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().clickable(onClickLabel = language.text("انتخاب سوره"), onClick = onChooseSurah)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.fillMaxWidth().height((92 * fontScale.coerceAtMost(1.3f)).dp), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.quran_surah_ornament),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = surah?.let { "سُورَةُ ${it.name}" } ?: "",
                        fontFamily = UthmanicHafs,
                        fontSize = (22 * fontScale).sp,
                        lineHeight = (30 * fontScale).sp,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.offset(y = (-3).dp)
                    )
                }
                Row(
                    Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = language.text("سورهٔ ${surahNumber.toPersianDigits()}") +
                            (surah?.let { " · " + language.text("${it.verseCount.toPersianDigits()} آیه · صفحه ${it.firstPage.toPersianDigits()}") } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = SoftBorder)

            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = language.text("انتخاب قاری"), onClick = onChooseReciter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReciterAvatar(reciterName, 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(reciterName, fontSize = (17 * fontScale).sp, fontWeight = FontWeight.Bold,
                        color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(language.text("حفص از عاصم"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(50), color = accent.color.copy(alpha = 0.12f)) {
                    Row(
                        Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(language.text("تغییر"), style = MaterialTheme.typography.labelLarge, color = accent.color)
                        Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = accent.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: AudioStatus, reduceMotion: Boolean, accent: AudioAccent) {
    val language = LocalAppLanguage.current
    Surface(shape = RoundedCornerShape(50), color = accent.color.copy(alpha = 0.12f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SoundBars(animate = status == AudioStatus.PLAYING && !reduceMotion, color = accent.color)
            Text(
                language.text(when (status) {
                    AudioStatus.PLAYING -> "در حال پخش"
                    AudioStatus.LOADING -> "در حال دریافت تلاوت"
                    AudioStatus.READY -> "آماده پخش"
                }),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent.color
            )
        }
    }
}

/** Four small bars that move while a recitation plays and rest otherwise. */
@Composable
private fun SoundBars(animate: Boolean, color: Color) {
    val transition = rememberInfiniteTransition(label = "soundBars")
    Row(
        Modifier.height(14.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(520, 380, 640, 450).forEachIndexed { index, period ->
            val level by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(period), RepeatMode.Reverse),
                label = "bar$index"
            )
            val height = if (animate) level else listOf(0.4f, 0.7f, 0.5f, 0.3f)[index]
            Box(
                Modifier.width(3.dp).fillMaxHeight(height)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

/**
 * Play/pause in the app's green. While the recitation is being fetched, a ring around it shows
 * the download and the button cancels.
 */
@Composable
private fun PlayButton(
    playing: Boolean,
    loading: Boolean,
    downloadPercent: Int?,
    enabled: Boolean,
    accent: AudioAccent,
    onClick: () -> Unit
) {
    val language = LocalAppLanguage.current
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            if (downloadPercent != null) {
                CircularProgressIndicator(
                    progress = { downloadPercent / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = accent.color,
                    trackColor = accent.color.copy(alpha = 0.15f),
                    strokeWidth = 4.dp
                )
            } else {
                CircularProgressIndicator(Modifier.fillMaxSize(), color = accent.color, strokeWidth = 4.dp,
                    trackColor = accent.color.copy(alpha = 0.15f))
            }
        }
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent.color, contentColor = accent.onColor)
        ) {
            Icon(
                imageVector = when {
                    loading -> Icons.Default.Close
                    playing -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = language.text(when {
                    loading -> "لغو"
                    playing -> "توقف"
                    else -> "پخش تلاوت"
                }),
                modifier = Modifier.size(if (loading) 28.dp else 36.dp)
            )
        }
    }
}

/** The Qari's initial in a soft green circle; there are no unverified portraits. */
@Composable
private fun ReciterAvatar(name: String, size: Dp) {
    val accent = audioAccent()
    Box(
        Modifier.size(size).clip(CircleShape).background(accent.color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1),
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            color = accent.color
        )
    }
}

@Composable
private fun SurahNumberBadge(number: Int) {
    val accent = audioAccent()
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(LocalAppLanguage.current.text(number.toPersianDigits()),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = accent.color)
    }
}

@Composable
private fun AudioChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    downloaded: Boolean = false,
    leading: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = audioAccent()
    val language = LocalAppLanguage.current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        if (downloaded) Icon(Icons.Default.DownloadDone, language.text("تلاوت ذخیره شده"), Modifier.size(20.dp), tint = accent.color)
        if (selected) Icon(Icons.Default.Check, null, tint = accent.color)
    }
}

@Composable
private fun EmptyResult(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.SearchOff, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun audioSearch(value: String): String = value.trim().lowercase().replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
    .replace('أ', 'ا').replace('إ', 'ا').replace('ٱ', 'ا').replace('ة', 'ه')
    .map { when (it) { in '۰'..'۹' -> '0' + (it - '۰'); in '٠'..'٩' -> '0' + (it - '٠'); else -> it } }
    .joinToString("").replace(Regex("[\\s\\u064B-\\u065F\\u0670\\u200c]"), "")

private fun audioTime(ms: Int): String = "%02d:%02d".format(java.util.Locale.US, ms / 60_000, ms / 1_000 % 60).toPersianDigits()

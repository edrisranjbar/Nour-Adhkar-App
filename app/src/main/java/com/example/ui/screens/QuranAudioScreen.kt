package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.*
import com.example.quran.QuranRepository
import com.example.quran.QuranSurah
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    val reciterName = if (language == AppLanguage.ARABIC) reciter.arName else reciter.faName
    val state by QuranAudioPlayer.state.collectAsState()
    val current = state.reciterId == reciter.id && state.surah == surahNumber
    val playing = current && state.isPlaying
    val loading = current && state.isLoading
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

    Column(
        Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val colors = MaterialTheme.colorScheme
        // Name-led artwork works offline and never substitutes a guessed portrait.
        Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainerHigh)))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(64.dp), tint = colors.onPrimaryContainer)
                Text(reciterName, fontSize = (25 * fontScale).sp, fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer, textAlign = TextAlign.Center)
                Text(language.text("حفص از عاصم"), color = colors.onPrimaryContainer, fontSize = (13 * fontScale).sp)
                OutlinedButton(onClick = { openPicker("reciter") }) {
                    Text(language.text("انتخاب قاری"))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
        }

        OutlinedCard(onClick = { openPicker("surah") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(surah?.let { "${it.number.toPersianDigits()} · ${it.name}" } ?: surahNumber.toPersianDigits(),
                        fontSize = (21 * fontScale).sp, fontWeight = FontWeight.Bold)
                    Text(language.text("انتخاب سوره"), color = colors.onSurfaceVariant)
                }
                if (surahNumber in saved) Icon(Icons.Default.DownloadDone, language.text("تلاوت ذخیره شده"), tint = colors.primary)
                Icon(Icons.Default.ExpandMore, null)
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.semantics { contentDescription = language.text("موقعیت پخش") },
                    value = (dragging ?: position.toFloat()).coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { dragging = it },
                    onValueChangeFinished = { dragging?.let { QuranAudioPlayer.seekTo(it.toInt()) }; dragging = null },
                    valueRange = 0f..duration.coerceAtLeast(1).toFloat(), enabled = current && duration > 0 && !loading
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(audioTime((dragging ?: position.toFloat()).toInt()), color = colors.onSurfaceVariant)
                    Text(audioTime(duration), color = colors.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectSurah(surahNumber - 1) }, enabled = surahNumber > 1) {
                        Icon(Icons.Default.SkipPrevious, language.text("سورهٔ قبل"))
                    }
                    IconButton(onClick = { QuranAudioPlayer.skip(-10_000) }, enabled = current && duration > 0 && !loading) {
                        Icon(Icons.Default.Replay10, language.text("۱۰ ثانیه عقب"))
                    }
                    FilledIconButton(
                        onClick = {
                            if (current && state.error == null && duration > 0) QuranAudioPlayer.togglePlayPause()
                            else QuranAudioPlayer.play(context, reciter, surahNumber)
                        }, modifier = Modifier.size(72.dp), enabled = !loading && surah != null
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(28.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                        else Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            language.text(if (playing) "توقف" else "پخش تلاوت"), Modifier.size(36.dp))
                    }
                    IconButton(onClick = { QuranAudioPlayer.skip(10_000) }, enabled = current && duration > 0 && !loading) {
                        Icon(Icons.Default.Forward10, language.text("۱۰ ثانیه جلو"))
                    }
                    IconButton(onClick = { selectSurah(surahNumber + 1) }, enabled = surahNumber < 114) {
                        Icon(Icons.Default.SkipNext, language.text("سورهٔ بعد"))
                    }
                }
            }
        }
        if (current && state.isDownloading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.text("در حال دریافت تلاوت") + (state.downloadPercent?.let { " · ${it.toPersianDigits()}٪" } ?: ""))
                TextButton(onClick = { QuranAudioPlayer.stop() }) { Text(language.text("لغو")) }
            }
        }
        if (current && state.error != null) {
            Text(language.text(state.error!!), color = colors.error, textAlign = TextAlign.Center)
            TextButton(onClick = { QuranAudioPlayer.play(context, reciter, surahNumber) }) { Text(language.text("تلاش دوباره")) }
        }
        if (surahLoad == null) {
            CircularProgressIndicator(Modifier.size(24.dp))
        } else if (surahs == null) {
            Text(language.text("فهرست سوره‌ها در دسترس نیست"), color = colors.error)
            TextButton(onClick = { catalogRetry++ }) { Text(language.text("تلاش دوباره")) }
        }
    }

    if (picker in listOf("reciter", "surah")) {
        ModalBottomSheet(onDismissRequest = { picker = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text(language.text(if (picker == "reciter") "انتخاب قاری" else "انتخاب سوره"),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 12.dp), singleLine = true,
                    label = { Text(language.text(if (picker == "reciter") "جست‌وجوی قاری" else "نام یا شمارهٔ سوره")) },
                    leadingIcon = { Icon(Icons.Default.Search, null) })
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp).padding(bottom = 20.dp)) {
                    when (picker) {
                        "reciter" -> {
                            val matches = QuranReciters.filter { audioSearch(it.faName + " " + it.arName).contains(audioSearch(query)) }
                            if (matches.isEmpty()) item { Text(language.text("نتیجه‌ای پیدا نشد"), Modifier.padding(24.dp)) }
                            items(matches, key = { it.id }) { voice ->
                                AudioChoice(if (language == AppLanguage.ARABIC) voice.arName else voice.faName, voice.id == reciter.id) {
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
                            if (matches.isEmpty()) item { Text(language.text("نتیجه‌ای پیدا نشد"), Modifier.padding(24.dp)) }
                            items(matches, key = { it.number }) { item ->
                                AudioChoice("${item.number.toPersianDigits()} · ${item.name}", item.number == surahNumber,
                                    downloaded = item.number in saved, subtitle = "${item.verseCount.toPersianDigits()} " + language.text("آیه")) {
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

@Composable
private fun AudioChoice(title: String, selected: Boolean, downloaded: Boolean = false, subtitle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioButton(selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (downloaded) Icon(Icons.Default.DownloadDone, LocalAppLanguage.current.text("تلاوت ذخیره شده"), tint = MaterialTheme.colorScheme.primary)
    }
}

internal fun audioSearch(value: String): String = value.trim().lowercase().replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
    .replace('أ', 'ا').replace('إ', 'ا').replace('ٱ', 'ا').replace('ة', 'ه')
    .map { when (it) { in '۰'..'۹' -> '0' + (it - '۰'); in '٠'..'٩' -> '0' + (it - '٠'); else -> it } }
    .joinToString("").replace(Regex("[\\s\\u064B-\\u065F\\u0670\\u200c]"), "")

private fun audioTime(ms: Int): String = "%02d:%02d".format(java.util.Locale.US, ms / 60_000, ms / 1_000 % 60).toPersianDigits()

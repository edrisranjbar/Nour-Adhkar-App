package com.example.ui.screens

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lecture
import com.example.data.model.Scholar
import com.example.data.repository.ScholarsRepository
import com.example.media.LecturePlayer
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private fun scholarDark(hue: Float) = Color.hsv(hue, 0.60f, 0.30f)
private fun scholarMid(hue: Float) = Color.hsv(hue, 0.55f, 0.55f)
private fun scholarLight(hue: Float) = Color.hsv(hue, 0.30f, 0.92f)

private fun formatTime(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    return text.toPersianDigits()
}

/** «علما و مشاهیر»: scholars → their lectures → full-screen player. */
@Composable
fun ScholarsScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val fontScale by viewModel.fontScale.collectAsState()
    var scholars by remember { mutableStateOf(ScholarsRepository.cached(context)) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { ScholarsRepository.refresh(context)?.let { scholars = it } }
    // Pull-to-refresh on the list: keeps what is shown if the refresh fails.
    val onRefresh: () -> Unit = {
        if (!refreshing) scope.launch {
            refreshing = true
            val fresh = ScholarsRepository.refresh(context)
            if (fresh != null) {
                scholars = fresh
            } else {
                Toast.makeText(
                    context.applicationContext,
                    "به‌روزرسانی انجام نشد. اتصال اینترنت را بررسی کنید.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            refreshing = false
        }
    }
    var scholarId by rememberSaveable { mutableStateOf<String?>(null) }
    var lectureId by rememberSaveable { mutableStateOf<String?>(null) }
    val scholar = scholars.firstOrNull { it.id == scholarId }
    val lecture = scholar?.lectures?.firstOrNull { it.id == lectureId }

    BackHandler(enabled = scholar != null) {
        if (lecture != null) lectureId = null else scholarId = null
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            scholar != null && lecture != null ->
                LecturePlayerPage(scholar, lecture, fontScale, innerPadding) { lectureId = null }
            scholar != null ->
                ScholarLecturesPage(scholar, fontScale, innerPadding, onBack = { scholarId = null }) { lectureId = it.id }
            else -> ScholarListPage(scholars, fontScale, innerPadding, refreshing, onRefresh) { scholarId = it.id }
        }
    }
}

// ───────────────────────── Cover art ─────────────────────────

/** Cover: the scholar's photo when set in the admin panel, otherwise generated art (tinted gradient, star ornament, initial). */
@Composable
private fun CoverArt(scholar: Scholar, size: Dp, spinning: Boolean = false, corner: Dp = size * 0.18f) {
    val transition = rememberInfiniteTransition(label = "cover")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    val hue = scholar.hue
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(scholarMid(hue), scholarDark(hue)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().rotate(if (spinning) angle else 0f)) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension / 2
            val gold = Color.White.copy(alpha = 0.22f)
            for (i in 1..4) drawCircle(gold, r * (0.28f + i * 0.17f), c, style = Stroke(r * 0.008f + 1f))
            // eight-pointed star (two overlapping squares)
            for (k in 0..1) {
                val path = Path()
                for (i in 0..3) {
                    val a = Math.toRadians((i * 90 + k * 45 + 45).toDouble())
                    val p = Offset(c.x + (r * 0.78f * cos(a)).toFloat(), c.y + (r * 0.78f * sin(a)).toFloat())
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, Color.White.copy(alpha = 0.16f), style = Stroke(r * 0.012f + 1f))
            }
        }
        Text(
            scholar.name.removePrefix("شیخ ").take(1),
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold
        )
        // Photo from the admin panel, drawn over the generated art (which shows while loading,
        // offline before the first load, or if the image fails). The photo itself never spins.
        scholar.photoUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = scholar.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ───────────────────────── Scholar list ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScholarListPage(
    scholars: List<Scholar>,
    fontScale: Float,
    innerPadding: PaddingValues,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (Scholar) -> Unit
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        indicator = {
            // Below the app bar, which overlaps the top of this screen.
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = innerPadding.calculateTopPadding())
            )
        }
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = innerPadding.calculateTopPadding() + 4.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "سخنرانی و دروس بزرگان؛ گوش دهید و بهره ببرید.",
                fontSize = (13 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        if (scholars.isEmpty()) item {
            Text("فهرستی برای نمایش نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(scholars, key = { it.id }) { scholar ->
            ScholarCard(scholar, fontScale) { onOpen(scholar) }
        }
    }
    }
}

@Composable
private fun ScholarCard(scholar: Scholar, fontScale: Float, onClick: () -> Unit) {
    val hue = scholar.hue
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp), ambientColor = scholarDark(hue), spotColor = scholarDark(hue))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(scholarDark(hue), scholarMid(hue))))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CoverArt(scholar, 88.dp, corner = 24.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(scholar.name, color = Color.White, fontSize = (20 * fontScale).sp, fontWeight = FontWeight.Bold)
                if (scholar.tagline.isNotBlank()) {
                    Text(scholar.tagline, color = Color.White.copy(alpha = 0.78f), fontSize = (12.5f * fontScale).sp)
                }
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
                    Text(
                        if (scholar.lectures.isEmpty()) "به‌زودی" else "${scholar.lectures.size.toPersianDigits()} سخنرانی",
                        color = Color.White,
                        fontSize = (11.5f * fontScale).sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ───────────────────────── Lectures of one scholar ─────────────────────────

@Composable
private fun ScholarLecturesPage(
    scholar: Scholar,
    fontScale: Float,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (Lecture) -> Unit
) {
    val hue = scholar.hue
    val playerState by LecturePlayer.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(scholarDark(hue), scholarMid(hue))),
                        RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
                    .padding(top = innerPadding.calculateTopPadding(), bottom = 24.dp, start = 12.dp, end = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                        }
                    }
                    CoverArt(scholar, 132.dp, corner = 34.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(scholar.name, color = Color.White, fontSize = (22 * fontScale).sp, fontWeight = FontWeight.Bold)
                    if (scholar.bio.isNotBlank()) {
                        Text(
                            scholar.bio,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = (13 * fontScale).sp,
                            textAlign = TextAlign.Center,
                            lineHeight = (21 * fontScale).sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        if (scholar.lectures.isEmpty()) {
            item {
                Text(
                    "سخنرانی‌های این استاد به‌زودی اضافه می‌شود.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            }
        }
        items(scholar.lectures, key = { it.id }) { lecture ->
            val active = playerState.lectureId == lecture.id
            val index = scholar.lectures.indexOf(lecture) + 1
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable { onOpen(lecture) }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(scholarMid(hue), scholarDark(hue)))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active && playerState.isPlaying) {
                            Icon(Icons.Default.GraphicEq, null, tint = Color.White)
                        } else {
                            Text(index.toPersianDigits(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(lecture.title, fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Bold)
                        if (lecture.description.isNotBlank()) {
                            Text(
                                lecture.description,
                                fontSize = (12 * fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                    lecture.durationSec?.let {
                        Text(formatTime(it * 1000), fontSize = (12 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Player ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LecturePlayerPage(
    scholar: Scholar,
    lecture: Lecture,
    fontScale: Float,
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val hue = scholar.hue
    val state by LecturePlayer.state.collectAsState()
    val current = state.lectureId == lecture.id
    val playing = current && state.isPlaying
    val loading = current && state.isLoading

    // Start when the page opens (or resume if it is already the loaded lecture).
    LaunchedEffect(lecture.id) { LecturePlayer.play(lecture.id, lecture.audioUrl) }
    LaunchedEffect(playing) {
        while (playing) { LecturePlayer.refreshPosition(); delay(500) }
    }

    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = if (current && state.durationMs > 0) state.durationMs else (lecture.durationSec ?: 0) * 1000
    val position = if (current) state.positionMs else 0

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scholarDark(hue), scholarMid(hue).copy(alpha = 0.55f), MaterialTheme.colorScheme.background)))
            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "بازگشت", tint = Color.White) }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.shadow(24.dp, RoundedCornerShape(44.dp), ambientColor = Color.Black, spotColor = Color.Black)
        ) { CoverArt(scholar, 250.dp, spinning = playing, corner = 44.dp) }
        Spacer(Modifier.height(24.dp))
        Text(
            lecture.title, color = Color.White, fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = (32 * fontScale).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(scholar.name, color = Color.White.copy(alpha = 0.85f), fontSize = (15 * fontScale).sp)

        Spacer(Modifier.height(20.dp))
        // Playback controls read left-to-right regardless of the app direction.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth()) {
                val shown = dragging ?: position.toFloat()
                Slider(
                    value = shown.coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { dragging = it },
                    onValueChangeFinished = { dragging?.let { LecturePlayer.seekTo(it.toInt()) }; dragging = null },
                    valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                    enabled = current && !loading && duration > 0
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(shown.toInt()), fontSize = (12 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(duration), fontSize = (12 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { LecturePlayer.skip(-10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Replay10, "۱۰ ثانیه عقب", modifier = Modifier.size(34.dp))
                    }
                    Box(
                        Modifier.size(76.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(scholarMid(hue), scholarDark(hue))))
                            .clickable {
                                if (current && state.error == null) LecturePlayer.togglePlayPause()
                                else LecturePlayer.play(lecture.id, lecture.audioUrl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                        else Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (playing) "توقف" else "پخش",
                            tint = Color.White, modifier = Modifier.size(44.dp)
                        )
                    }
                    IconButton(onClick = { LecturePlayer.skip(10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Forward10, "۱۰ ثانیه جلو", modifier = Modifier.size(34.dp))
                    }
                }
            }
        }
        if (current && state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = (13 * fontScale).sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                val selected = state.speed == s
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { LecturePlayer.setSpeed(s) }
                ) {
                    Text(
                        "${(if (s == 1f) "1" else s.toString()).toPersianDigits()}×",
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = (12.5f * fontScale).sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (lecture.description.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("درباره این سخنرانی", fontWeight = FontWeight.Bold, fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colorScheme.primary)
                    Text(lecture.description, fontSize = (14 * fontScale).sp, lineHeight = (24 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

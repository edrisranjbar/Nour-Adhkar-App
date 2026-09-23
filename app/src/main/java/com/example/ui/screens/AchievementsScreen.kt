package com.example.ui.screens

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

private val AchievementCanvas = Color(0xFFFBF7ED)
private val AchievementSurface = Color(0xFFFFFDF7)
private val AchievementBorder = Color(0xFFE8DFC9)
private val AchievementText = Color(0xFF222A20)
private val AchievementMuted = Color(0xFF746F63)
private val AchievementEmerald = Color(0xFF246B3D)
private val AchievementEmeraldDark = Color(0xFF0E4B38)
private val AchievementTrack = Color(0xFFE9E2D3)

private enum class AchievementFilter(val label: String) {
    All("همه"), Consistency("استمرار"), Tasks("اعمال روزانه"), Dhikr("اذکار")
}

private val ShieldShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.18f)
    lineTo(size.width * 0.88f, size.height * 0.72f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(size.width * 0.12f, size.height * 0.72f)
    lineTo(0f, size.height * 0.18f)
    close()
}

private data class Achievement(
    val id: String,
    val filter: AchievementFilter,
    val title: String,
    val description: String,
    val progress: Int,
    val levels: List<Int>,
    val unit: String,
    @param:DrawableRes val artwork: Int,
    val color: Color
) {
    val unlockedLevel: Int get() = levels.count { progress >= it }
    val nextTarget: Int get() = levels.firstOrNull { progress < it } ?: levels.last()
    val complete: Boolean get() = unlockedLevel == levels.size
}

@Composable
fun AchievementsScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceRepository(context) }
    val activityDays by viewModel.activityDayKeys.collectAsState()
    val checklistDays by viewModel.checklistCompletionCounts.collectAsState()
    val tasbihSessions by viewModel.recentTasbihSessions.collectAsState()
    var selected by remember { mutableStateOf<Achievement?>(null) }
    var filter by remember { mutableStateOf(AchievementFilter.All) }
    var showInfo by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<Achievement?>(null) }

    val achievements = listOf(
        Achievement(
            id = "consistency",
            filter = AchievementFilter.Consistency,
            title = "همراه پیوسته",
            description = "در روزهای بیشتری با اذکار نور همراه باش",
            progress = activityDays.size,
            levels = listOf(3, 7, 30),
            unit = "روز",
            artwork = R.drawable.achievement_consistency,
            color = Color(0xFF2F7545)
        ),
        Achievement(
            id = "tasks",
            filter = AchievementFilter.Tasks,
            title = "یار اعمال روزانه",
            description = "کارهای چک‌لیست روزانه را کامل کن",
            progress = checklistDays.values.sum(),
            levels = listOf(10, 30, 100),
            unit = "کار",
            artwork = R.drawable.achievement_daily_tasks,
            color = Color(0xFFB17A17)
        ),
        Achievement(
            id = "dhikr",
            filter = AchievementFilter.Dhikr,
            title = "ذاکر پرتلاش",
            description = "ذکرهایت را با ذکرشمار ثبت کن",
            progress = tasbihSessions.sumOf { it.count },
            levels = listOf(100, 500, 1000),
            unit = "ذکر",
            artwork = R.drawable.achievement_tasbih,
            color = Color(0xFF57438B)
        )
    )
    val unlockedLevels = achievements.sumOf { it.unlockedLevel }
    val levelSignature = achievements.joinToString { "${it.id}:${it.unlockedLevel}" }

    LaunchedEffect(levelSignature) {
        val current = achievements.associate { it.id to it.unlockedLevel }
        if (!prefs.hasAchievementLevelBaseline()) {
            prefs.setAchievementLevelsSeen(current)
        } else {
            val seen = prefs.getAchievementLevelsSeen()
            celebration = achievements.firstOrNull { it.unlockedLevel > (seen[it.id] ?: 0) }
            prefs.setAchievementLevelsSeen(current)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AchievementCanvas)
            .padding(innerPadding)
    ) {
        AchievementsTopBar(onBack = onNavigateHome, onInfo = { showInfo = true })
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AchievementSummary(
                    unlockedLevels = unlockedLevels,
                    completed = achievements.count(Achievement::complete),
                    locked = achievements.count { it.unlockedLevel == 0 }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                AchievementFilters(selected = filter, onSelected = { filter = it })
            }
            val visible = if (filter == AchievementFilter.All) achievements else achievements.filter { it.filter == filter }
            items(visible, key = Achievement::id) { achievement ->
                AchievementTile(achievement = achievement, onClick = { selected = achievement })
            }
        }
    }

    selected?.let { achievement ->
        AchievementDetailScreen(achievement = achievement, onDismiss = { selected = null })
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("نشان‌ها و دستاوردها") },
            text = { Text("با استمرار در اذکار، انجام اعمال روزانه و استفاده از ذکرشمار، مرحله‌های هر نشان را باز کنید.") },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("متوجه شدم") } }
        )
    }
    celebration?.let { achievement ->
        AchievementCelebrationScreen(achievement = achievement, onDismiss = { celebration = null })
    }
}

@Composable
private fun AchievementsTopBar(onBack: () -> Unit, onInfo: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = AchievementSurface,
            border = BorderStroke(1.dp, AchievementBorder)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = AchievementText)
            }
        }
        Text(
            "نشان‌ها و دستاوردها",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AchievementText
        )
        Surface(
            modifier = Modifier.align(Alignment.CenterStart).size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = AchievementSurface,
            border = BorderStroke(1.dp, AchievementBorder)
        ) {
            IconButton(onClick = onInfo) {
                Icon(Icons.Default.Info, contentDescription = "راهنما", tint = AchievementText)
            }
        }
    }
}

@Composable
private fun AchievementFilters(selected: AchievementFilter, onSelected: (AchievementFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AchievementFilter.entries.forEach { item ->
            val active = item == selected
            Surface(
                modifier = Modifier.weight(1f).height(34.dp).clickable { onSelected(item) },
                shape = RoundedCornerShape(18.dp),
                color = if (active) AchievementEmerald else Color(0xFFF7F0DF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(item.label, fontSize = 10.sp, color = if (active) Color.White else AchievementMuted)
                }
            }
        }
    }
}

@Composable
private fun AchievementSummary(unlockedLevels: Int, completed: Int, locked: Int) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = AchievementSurface
        ),
        border = BorderStroke(1.dp, AchievementBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMedal(completed.toPersianDigits(), "کامل شده", Color(0xFF2F7545), false)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(82.dp)) {
                CircularProgressIndicator(
                    progress = { unlockedLevels / 9f },
                    modifier = Modifier.size(78.dp),
                    color = Color(0xFF2F7545),
                    trackColor = AchievementTrack,
                    strokeWidth = 7.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        unlockedLevels.toPersianDigits(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AchievementText
                    )
                    Text("از ۹ مرحله", fontSize = 10.sp, color = AchievementMuted)
                }
            }
            SummaryMedal(locked.toPersianDigits(), "قفل شده", SunGold, true)
        }
    }
}

@Composable
private fun SummaryMedal(value: String, label: String, color: Color, locked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = ShieldShape,
            color = color.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (locked) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = color, modifier = Modifier.size(25.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                        Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 14.sp)
                    }
                }
            }
        }
        Text(
            if (locked) "$label ${value}" else label,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 10.sp,
            color = AchievementMuted
        )
    }
}

@Composable
private fun AchievementTile(achievement: Achievement, onClick: () -> Unit) {
    val locked = achievement.unlockedLevel == 0
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = AchievementSurface),
        border = BorderStroke(1.dp, AchievementBorder)
    ) {
        Column(modifier = Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                val grayMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }
                Image(
                    painter = painterResource(achievement.artwork),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .drawWithContent {
                            drawContent()
                            if (locked) drawRect(Color.White.copy(alpha = 0.42f))
                        },
                    contentScale = ContentScale.Crop,
                    colorFilter = if (locked) ColorFilter.colorMatrix(grayMatrix) else null
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 4.dp).size(32.dp, 36.dp),
                    shape = ShieldShape,
                    color = if (locked) Color(0xFF77756D) else achievement.color,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (locked) Icons.Default.Lock else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        if (!locked) {
                            Text(
                                achievement.unlockedLevel.toPersianDigits(),
                                modifier = Modifier.padding(start = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Text(
                achievement.title,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                achievement.description,
                modifier = Modifier.height(38.dp).padding(top = 3.dp, start = 2.dp, end = 2.dp),
                fontSize = 9.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                    color = AchievementMuted
            )
            Text(
                "${achievement.progress.toPersianDigits()} / ${achievement.nextTarget.toPersianDigits()}",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 10.sp,
                color = achievement.color,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { (achievement.progress / achievement.nextTarget.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 7.dp).height(5.dp),
                color = achievement.color,
                trackColor = AchievementTrack
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                achievement.levels.forEach { target ->
                    val reached = achievement.progress >= target
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        color = if (reached) achievement.color else AchievementTrack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (reached) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (reached) Color.White else AchievementMuted.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun AchievementDetailScreen(achievement: Achievement, onDismiss: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = AchievementCanvas) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(Modifier.fillMaxWidth().height(285.dp).background(AchievementEmeraldDark)) {
                        Image(
                            painter = painterResource(achievement.artwork),
                            contentDescription = achievement.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(AchievementEmeraldDark.copy(alpha = 0.32f)))
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                        color = AchievementSurface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 72.dp, bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = achievement.color.copy(alpha = 0.13f)
                            ) {
                                Text(
                                    if (achievement.complete) "دستاورد کامل"
                                    else if (achievement.unlockedLevel == 0) "هنوز قفل است"
                                    else "مرحله ${achievement.unlockedLevel.toPersianDigits()} از ۳",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = achievement.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                achievement.title,
                                modifier = Modifier.padding(top = 10.dp),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = AchievementText
                            )
                            Text(
                                achievement.description,
                                modifier = Modifier.padding(top = 5.dp),
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                                color = AchievementMuted
                            )
                            AchievementProgressCard(achievement)
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = AchievementSurface),
                                border = BorderStroke(1.dp, AchievementBorder)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("آخرین فعالیت", fontWeight = FontWeight.Bold, color = AchievementText)
                                        Text(
                                            if (achievement.progress > 0) "آخرین پیشرفت شما در این نشان ثبت شده است"
                                            else "هنوز فعالیتی برای این نشان ثبت نشده است",
                                            modifier = Modifier.padding(top = 4.dp),
                                            fontSize = 11.sp,
                                            color = AchievementMuted
                                        )
                                    }
                                    if (achievement.progress > 0) {
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDDE9D8)) {
                                            Text("ثبت شد", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = AchievementEmerald)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 225.dp).size(120.dp),
                    shape = ShieldShape,
                    color = AchievementSurface,
                    border = BorderStroke(3.dp, SunGold.copy(alpha = 0.75f)),
                    shadowElevation = 14.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(achievement.artwork),
                            contentDescription = null,
                            modifier = Modifier.size(94.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).size(38.dp, 42.dp),
                            shape = ShieldShape,
                            color = achievement.color,
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    achievement.unlockedLevel.coerceAtLeast(1).toPersianDigits(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.35f)) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.35f)) {
                        IconButton(onClick = {
                            val text = "${achievement.title}\n${achievement.progress.toPersianDigits()} ${achievement.unit} در اذکار نور"
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }, "اشتراک‌گذاری دستاورد"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressCard(achievement: Achievement) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = AchievementSurface),
        border = BorderStroke(1.dp, AchievementBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("پیشرفت فعلی", fontSize = 12.sp, color = AchievementMuted)
                Spacer(Modifier.weight(1f))
                Text(
                    "${achievement.progress.toPersianDigits()} / ${achievement.nextTarget.toPersianDigits()} ${achievement.unit}",
                    color = achievement.color,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { (achievement.progress / achievement.nextTarget.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(9.dp),
                color = achievement.color,
                trackColor = AchievementTrack
            )
            Box(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 21.dp, start = 20.dp, end = 20.dp),
                    color = AchievementBorder
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    achievement.levels.forEachIndexed { index, target ->
                        val reached = achievement.progress >= target
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = ShieldShape,
                                color = if (reached) achievement.color else AchievementTrack,
                                border = BorderStroke(1.dp, if (reached) SunGold.copy(alpha = 0.7f) else AchievementBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (reached) Text(target.toPersianDigits(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    else Icon(Icons.Default.Lock, null, Modifier.size(17.dp), tint = AchievementMuted)
                                }
                            }
                            Text("مرحله ${(index + 1).toPersianDigits()}", Modifier.padding(top = 5.dp), fontSize = 10.sp, color = AchievementMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCelebrationScreen(achievement: Achievement, onDismiss: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(AchievementEmeraldDark).statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✦  ✧  ✦", color = SunGold, fontSize = 30.sp)
                Surface(
                    modifier = Modifier.padding(top = 18.dp).size(220.dp),
                    shape = ShieldShape,
                    color = Color(0xFFF8EBC6),
                    border = BorderStroke(4.dp, SunGold),
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(achievement.artwork),
                            contentDescription = null,
                            modifier = Modifier.size(178.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).size(52.dp, 58.dp),
                            shape = ShieldShape,
                            color = achievement.color,
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(achievement.unlockedLevel.toPersianDigits(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                            }
                        }
                    }
                }
                Text("تبریک!", modifier = Modifier.padding(top = 26.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("یک مرحله جدید باز شد", modifier = Modifier.padding(top = 6.dp), color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                Text(achievement.title, modifier = Modifier.padding(top = 12.dp), color = SunGold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "مرحله ${achievement.unlockedLevel.toPersianDigits()} از ۳",
                    modifier = Modifier.padding(top = 5.dp),
                    color = Color.White.copy(alpha = 0.88f)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp).clickable {
                        val text = "${achievement.title}\nمرحله ${achievement.unlockedLevel.toPersianDigits()} در اذکار نور"
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }, "اشتراک‌گذاری دستاورد"))
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF4D963D)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("اشتراک‌گذاری دستاورد", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp).clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF1D0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("ادامه", color = AchievementText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

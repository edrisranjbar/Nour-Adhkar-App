package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.example.ui.components.StreakCelebrationDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.util.Calendar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarData
import com.example.data.model.DhikrItem
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

/**
 * Validates whether a DhikrItem contains required non-empty data before rendering.
 */
private fun isValidDhikrItem(item: DhikrItem?): Boolean {
    if (item == null) return false
    // Must have at least Arabic text or Persian translation
    if (item.arabicText.isBlank() && item.persianTranslation.isBlank()) return false
    if (item.targetCount <= 0) return false
    return true
}

@Composable
fun DhikrCounterScreen(
    categoryId: String,
    viewModel: AdhkarViewModel
) {
    val rawAdhkarList by viewModel.currentCategoryAdhkar.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    // Validate data availability
    val category = remember(categoryId) { AdhkarData.categories.find { it.id == categoryId } }
    val staticFallback = remember(categoryId) {
        (AdhkarData.adhkarList[categoryId] ?: emptyList()).filter { isValidDhikrItem(it) }
    }
    val validAdhkarList = remember(rawAdhkarList) {
        rawAdhkarList.filter { isValidDhikrItem(it) }
    }

    // Use state-collected list if ready, otherwise fallback to static list immediately to avoid 1-frame flash
    val displayAdhkarList = if (validAdhkarList.isNotEmpty()) validAdhkarList else staticFallback

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showCongratsDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Fallback UI if category is completely invalid or no content exists
        if (category == null || displayAdhkarList.isEmpty()) {
            DataMissingFallbackUI(
                title = "اطلاعات موجود نیست",
                message = "محتوای این بخش از اذکار یا دسته‌بندی در دسترس نمی‌باشد.",
                onBackClick = {
                    viewModel.selectCategory(null)
                }
            )
            return@CompositionLocalProvider
        }

        val title = category.title.ifBlank { "اذکار" }
        val completedItems = displayAdhkarList.count { it.currentCount >= it.targetCount }
        val totalItems = displayAdhkarList.size
        val itemProgressRatio = if (totalItems > 0) {
            completedItems.toFloat() / totalItems
        } else {
            0f
        }

        val allProgress by viewModel.allProgress.collectAsState()
        val recentSessions by viewModel.recentTasbihSessions.collectAsState()

        val days = remember(allProgress, recentSessions) {
            val list = mutableListOf<DayActivity>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val isToday = i == 0
                val isActive = isDayActive(cal, allProgress, recentSessions)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val dayLabel = getPersianDayAbbreviation(dayOfWeek)
                list.add(
                    DayActivity(
                        dayLabel = dayLabel,
                        isActive = isActive,
                        isToday = isToday,
                        dateMillis = cal.timeInMillis
                    )
                )
            }
            list
        }

        val streak = remember(allProgress, recentSessions) {
            var s = 0
            val streakCal = Calendar.getInstance()
            val todayActive = isDayActive(streakCal, allProgress, recentSessions)
            if (todayActive) {
                s = 1
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
                while (isDayActive(streakCal, allProgress, recentSessions)) {
                    s++
                    streakCal.add(Calendar.DAY_OF_YEAR, -1)
                }
            } else {
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
                if (isDayActive(streakCal, allProgress, recentSessions)) {
                    s = 1
                    streakCal.add(Calendar.DAY_OF_YEAR, -1)
                    while (isDayActive(streakCal, allProgress, recentSessions)) {
                        s++
                        streakCal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                }
            }
            s
        }

        // Duolingo-style Streak Celebration Dialog when all dhikrs are completed
        if (showCongratsDialog) {
            StreakCelebrationDialog(
                streakCount = streak.coerceAtLeast(1),
                pastDays = days,
                fontScale = fontScale,
                onDismiss = {
                    showCongratsDialog = false
                    viewModel.selectCategory(null) // Return home
                }
            )
        }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.selectCategory(null)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "برگشت",
                            tint = SandDark
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = (18 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SandDark
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        LinearProgressIndicator(
                            progress = { itemProgressRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                // In RTL, end is the physical left edge. Combined with the
                                // app-bar's 12dp inset, this aligns with the cards' 16dp inset.
                                .padding(end = 4.dp)
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (completedItems == totalItems) Color(0xFF4CAF50) else SunGold,
                            trackColor = SoftBorder
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(displayAdhkarList) { index, item ->
                    DhikrItemCard(
                        index = index + 1,
                        item = item,
                        fontScale = fontScale,
                        onTap = {
                            val currentCount = item.currentCount
                            val targetCount = item.targetCount
                            val isCompleted = currentCount >= targetCount
                            if (!isCompleted) {
                                viewModel.incrementDhikr(categoryId, item.id, targetCount)
                                val willBeCompleted = (currentCount + 1) >= targetCount
                                if (willBeCompleted) {
                                    // Find next incomplete
                                    val currentIndex = index
                                    var targetScrollIndex = -1
                                    for (i in (currentIndex + 1) until displayAdhkarList.size) {
                                        if (displayAdhkarList[i].currentCount < displayAdhkarList[i].targetCount) {
                                            targetScrollIndex = i; break
                                        }
                                    }
                                    if (targetScrollIndex == -1) {
                                        for (i in 0 until currentIndex) {
                                            if (displayAdhkarList[i].currentCount < displayAdhkarList[i].targetCount) {
                                                targetScrollIndex = i; break
                                            }
                                        }
                                    }

                                    if (targetScrollIndex != -1) {
                                        coroutineScope.launch {
                                            delay(400)
                                            try {
                                                listState.animateScrollToItem(targetScrollIndex)
                                            } catch (_: Exception) {}
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            delay(600)
                                            showCongratsDialog = true
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Fallback UI component shown when Dhikr content or category is missing or unavailable.
 */
@Composable
fun DataMissingFallbackUI(
    title: String = "اطلاعات موجود نیست",
    message: String = "محتوای این ذکر یا دسته‌بندی در دسترس نیست یا بارگذاری نشد.",
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, SoftBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "اطلاعات موجود نیست",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SandDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = TextPersian,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SunGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SunGold)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بازگشت به صفحه اصلی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DhikrItemCard(
    index: Int,
    item: DhikrItem,
    fontScale: Float,
    onTap: () -> Unit
) {
    val isCompleted = item.currentCount >= item.targetCount
    val animatedCardBg by animateColorAsState(
        targetValue = if (isCompleted) Color(0xFFF2F5ED) else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCompleted) SunGold else SoftBorder,
        label = "borderBg"
    )

    val scaleAnim = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim.value)
            .then(
                if (isCompleted) {
                    Modifier
                } else {
                    Modifier.clickable {
                        coroutineScope.launch {
                            scaleAnim.animateTo(
                                targetValue = 0.95f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh)
                            )
                            scaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        onTap()
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = animatedCardBg),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Item index
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F0E1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Text
            val arabicDisplay = item.arabicText.ifBlank { "متن ذکر در دسترس نیست" }
            Text(
                text = arabicDisplay,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = AmiriQuran,
                    fontSize = (21 * fontScale).sp,
                    lineHeight = (40 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = TextArabic
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SoftBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Persian Translation
            val persianDisplay = item.persianTranslation.ifBlank { "ترجمه ذکر در دسترس نیست" }
            Text(
                text = persianDisplay,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (13.5 * fontScale).sp,
                    lineHeight = (22 * fontScale).sp,
                    color = TextPersian
                ),
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )



            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Bottom Panel (Digital Tasbih Ring)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(68.dp)
                ) {
                    val progress = if (item.targetCount > 0) {
                        item.currentCount.toFloat() / item.targetCount.toFloat()
                    } else {
                        0f
                    }

                    if (isCompleted) {
                        CircularProgressIndicator(
                            progress = { 1.0f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF4CAF50),
                            trackColor = SoftBorder,
                            strokeWidth = 4.5.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تکمیل شده",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = SunGold,
                            trackColor = SoftBorder,
                            strokeWidth = 4.5.dp
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.currentCount.toString(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SandDark
                                )
                                Text(
                                    text = "/",
                                    fontSize = 12.sp,
                                    color = SandDark.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                Text(
                                    text = item.targetCount.toString(),
                                    fontSize = 12.sp,
                                    color = NightBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "تکرار",
                                fontSize = 9.sp,
                                color = SandDark.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

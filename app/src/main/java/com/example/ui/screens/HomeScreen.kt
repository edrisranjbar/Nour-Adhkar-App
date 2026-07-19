package com.example.ui.screens

import java.util.Calendar
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.model.Category
import com.example.data.model.DhikrItem
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun HomeScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())
    val fontScale by viewModel.fontScale.collectAsState()

    // Wrap the entire screen in Right-To-Left direction for authentic Persian UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "اذکار نور",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = (30 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SandDark
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "پلتفرم فارسی اذکار و ادعیه اسلامی",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (12 * fontScale).sp,
                            color = SandDark.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "جستجوی اذکار...",
                        fontSize = (14 * fontScale).sp,
                        color = SandDark.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SandDark.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = SandDark
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SandDark,
                    unfocusedBorderColor = SandDark.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    focusedTextColor = SandDark,
                    unfocusedTextColor = SandDark
                )
            )

            // Content Area - Switch between Search Results and Main Dashboard
            Box(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                    ) {
                        // 0. Daily Progress -> Streak and Activity Calendar
                        item {
                            StreakCalendarCard(
                                viewModel = viewModel,
                                fontScale = fontScale
                            )
                        }

                        // 1. Ayah of the Day
                        item {
                            AyahOfTheDayCard(viewModel = viewModel, fontScale = fontScale)
                        }

                        // 2. Special Daily Adhkar Header
                        item {
                            Text(
                                text = "│ اذکار ویژه روزانه",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SandDark
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // 3. Special Daily Cards (Morning & Evening)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Morning Card
                                SpecialAdhkarCard(
                                    title = "اذکار صبحگاه",
                                    description = "روز خود را با یاد خدا آغاز کنید",
                                    badgeText = "۳۰ ذکر",
                                    emoji = "☀️",
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("morning") }
                                )

                                // Evening Card
                                SpecialAdhkarCard(
                                    title = "اذکار شامگاه",
                                    description = "هر روز را غرق آرامش کنید",
                                    badgeText = "۲۶ ذکر",
                                    emoji = "🌙",
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("evening") }
                                )
                            }
                        }

                        // 4. Grid Categories Header
                        item {
                            Text(
                                text = "│ دسته‌بندی‌ها",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SandDark
                                ),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        // 5. Grid list of general categories
                        item {
                            CategoriesGrid(viewModel = viewModel, fontScale = fontScale)
                        }
                    }
                } else {
                    SearchResultsView(
                        results = searchResults,
                        fontScale = fontScale,
                        bottomPadding = innerPadding.calculateBottomPadding() + 16.dp,
                        onResultClick = { catId ->
                            viewModel.selectCategory(catId)
                            viewModel.updateSearchQuery("")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AyahOfTheDayCard(viewModel: AdhkarViewModel, fontScale: Float) {
    val ayah = viewModel.ayahOfTheDay
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "آیه روز",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (15 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SandDark
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            // Arabic Text
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (19 * fontScale).sp,
                    lineHeight = (30 * fontScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = TextArabic
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SoftBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            // Persian Translation
            Text(
                text = ayah.translation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (13 * fontScale).sp,
                    lineHeight = (20 * fontScale).sp,
                    color = TextPersian
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Surah Reference
            Text(
                text = ayah.reference,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = NightBlue
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun StreakCalendarCard(
    viewModel: AdhkarViewModel,
    fontScale: Float
) {
    val allProgress by viewModel.allProgress.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()

    // Generate last 7 days (from 6 days ago to today)
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

    // Calculate current streak
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Streak & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "استمرار عبادت",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = (15 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = SandDark
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "گزارش فعالیت ۷ روز گذشته",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (11 * fontScale).sp,
                                color = SandDark.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                // Flame Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFF3E0), shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$streak روز متوالی 🔥",
                        fontSize = (12 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calendar Days Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Day Label
                        Text(
                            text = day.dayLabel,
                            fontSize = (12 * fontScale).sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) SunGold else SandDark.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isActive -> Color(0xFFE8F5E9) // soft light green
                                        day.isToday -> Color(0xFFFFFDE7) // soft light gold
                                        else -> Color(0xFFF5F5F5) // grey
                                    }
                                )
                                .border(
                                    width = if (day.isToday && !day.isActive) 1.5.dp else 1.dp,
                                    color = when {
                                        day.isActive -> Color(0xFF4CAF50) // active green
                                        day.isToday -> SunGold // gold border for today
                                        else -> SoftBorder.copy(alpha = 0.6f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.isActive) {
                                Text(
                                    text = "✔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            } else if (day.isToday) {
                                Text(
                                    text = "●",
                                    fontSize = 10.sp,
                                    color = SunGold.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Subtitle: today indicator
                        Text(
                            text = if (day.isToday) "امروز" else "",
                            fontSize = (9 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SunGold
                        )
                    }
                }
            }
        }
    }
}

data class DayActivity(
    val dayLabel: String,
    val isActive: Boolean,
    val isToday: Boolean,
    val dateMillis: Long
)

fun isDayActive(
    cal: Calendar,
    progressList: List<DhikrProgressEntity>,
    sessions: List<TasbihSessionEntity>
): Boolean {
    val testCal = cal.clone() as Calendar

    testCal.set(Calendar.HOUR_OF_DAY, 0)
    testCal.set(Calendar.MINUTE, 0)
    testCal.set(Calendar.SECOND, 0)
    testCal.set(Calendar.MILLISECOND, 0)
    val startMillis = testCal.timeInMillis

    testCal.set(Calendar.HOUR_OF_DAY, 23)
    testCal.set(Calendar.MINUTE, 59)
    testCal.set(Calendar.SECOND, 59)
    testCal.set(Calendar.MILLISECOND, 999)
    val endMillis = testCal.timeInMillis

    val progressActive = progressList.any {
        it.currentCount > 0 && it.lastUpdated in startMillis..endMillis
    }
    val sessionActive = sessions.any {
        it.timestamp in startMillis..endMillis
    }

    return progressActive || sessionActive
}

fun getPersianDayAbbreviation(calendarDayOfWeek: Int): String {
    return when (calendarDayOfWeek) {
        Calendar.SATURDAY -> "ش"
        Calendar.SUNDAY -> "ی"
        Calendar.MONDAY -> "د"
        Calendar.TUESDAY -> "س"
        Calendar.WEDNESDAY -> "چ"
        Calendar.THURSDAY -> "پ"
        Calendar.FRIDAY -> "ج"
        else -> ""
    }
}

@Composable
fun SpecialAdhkarCard(
    title: String,
    description: String,
    badgeText: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(148.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F0E1)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }

            // Text Info
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = SandDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = badgeText,
                        color = SunGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = NightBlue,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoriesGrid(viewModel: AdhkarViewModel, fontScale: Float) {
    // Show rest of categories in a neat 2-column grid
    val gridCategories = AdhkarData.categories.filter { it.id != "morning" && it.id != "evening" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        gridCategories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { cat ->
                    CategoryTile(cat = cat, fontScale = fontScale, modifier = Modifier.weight(1f)) {
                        viewModel.selectCategory(cat.id)
                    }
                }
                // Placeholder to keep balance if odd items
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Add electronic Tasbih Counter tile at the end of categories list
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab("tasbih") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F0E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📿", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تسبیح شمار هوشمند",
                                fontSize = (14 * fontScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SandDark
                            )
                            Text(
                                text = "ذکرهای دلخواه خود را دیجیتالی تسبیح بیندازید",
                                fontSize = (11 * fontScale).sp,
                                color = NightBlue
                            )
                        }
                    }
                    Text("←", fontSize = 16.sp, color = NightBlue)
                }
            }
        }
    }
}

@Composable
fun CategoryTile(
    cat: Category,
    fontScale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val emoji = when (cat.id) {
                    "daily" -> "📅"
                    "ramadan" -> "🌙"
                    "sleep" -> "🛌"
                    "istikhara" -> "🤲"
                    else -> "✨"
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
                Text(
                    text = "${cat.count} ذکر",
                    fontSize = (10 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                )
            }

            Text(
                text = cat.title,
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = SandDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchResultsView(
    results: List<Pair<String, DhikrItem>>,
    fontScale: Float,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onResultClick: (String) -> Unit
) {
    if (results.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📭", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ذکری با این مشخصات یافت نشد.",
                fontSize = (15 * fontScale).sp,
                color = SandDark.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(results) { (catTitle, dhikr) ->
                val catId = when (catTitle) {
                    "اذکار صبحگاه" -> "morning"
                    "اذکار شامگاه" -> "evening"
                    "اذکار روزانه" -> "daily"
                    "اذکار ماه رمضان" -> "ramadan"
                    "دعای خواب" -> "sleep"
                    "دعای استخاره" -> "istikhara"
                    else -> "morning"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(catId) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SoftBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFE8F0E1),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = catTitle,
                                    fontSize = 11.sp,
                                    color = SunGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dhikr.arabicText,
                            fontSize = (16 * fontScale).sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextArabic,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dhikr.persianTranslation,
                            fontSize = (12 * fontScale).sp,
                            lineHeight = 18.sp,
                            color = TextPersian,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

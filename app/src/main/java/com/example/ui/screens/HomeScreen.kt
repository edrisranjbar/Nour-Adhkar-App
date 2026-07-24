package com.example.ui.screens

import java.util.Calendar
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.components.StreakCelebrationDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.model.Category
import com.example.data.model.DhikrItem
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandBackground
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
            // Compact brand header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE8F0E1).copy(alpha = 0.7f),
                border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_nour_adhkar_logo),
                        contentDescription = "نشان اذکار نور",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(17.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اذکار نور",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = (24 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = SandDark
                            )
                        )
                        Text(
                            text = "اذکار و ادعیه اسلامی",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (11.5 * fontScale).sp,
                                color = NightBlue
                            )
                        )
                    }
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
                        contentDescription = "جستجو",
                        tint = SunGold
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "پاک کردن جستجو",
                                tint = SandDark
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SunGold,
                    unfocusedBorderColor = SoftBorder,
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
                            HomeSectionHeader(
                                title = "اذکار ویژه روزانه",
                                icon = Icons.Default.WbSunny,
                                fontScale = fontScale,
                                modifier = Modifier.padding(top = 6.dp)
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
                                    badgeText = "۳۰ ذکر",
                                    icon = Icons.Default.WbSunny,
                                    accentColor = Color(0xFFD58B19),
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("morning") }
                                )

                                // Evening Card
                                SpecialAdhkarCard(
                                    title = "اذکار شامگاه",
                                    badgeText = "۲۶ ذکر",
                                    icon = Icons.Default.NightsStay,
                                    accentColor = Color(0xFF53699A),
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("evening") }
                                )
                            }
                        }

                        // 4. Grid Categories Header
                        item {
                            HomeSectionHeader(
                                title = "دسته‌بندی‌ها",
                                icon = Icons.Default.GridView,
                                fontScale = fontScale,
                                modifier = Modifier.padding(top = 8.dp)
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
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: SVG Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "آیه روز",
                        tint = SunGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "آیه روز",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (16.5 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = SandDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text - Centered
            Text(
                text = ayah.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AmiriQuran,
                    fontSize = (15.5 * fontScale).sp,
                    lineHeight = (26 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = TextArabic
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Persian Translation
            Text(
                text = ayah.translation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (12.5 * fontScale).sp,
                    lineHeight = (18.5 * fontScale).sp,
                    color = TextPersian
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Surah Reference - Bottom Left
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SandBackground,
                    border = BorderStroke(0.5.dp, SoftBorder)
                ) {
                    Text(
                        text = ayah.reference,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SunGold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
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

    var showStreakDialog by remember { mutableStateOf(false) }

    if (showStreakDialog) {
        StreakCelebrationDialog(
            streakCount = streak,
            pastDays = days,
            fontScale = fontScale,
            onDismiss = { showStreakDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showStreakDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استمرار عبادت",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (14 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SandDark
                        )
                    )
                }

                // Flame Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$streak روز متوالی",
                        fontSize = (11.5 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            fontSize = (11 * fontScale).sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) SunGold else SandDark.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .size(26.dp)
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (day.isToday) {
                                Text(
                                    text = "●",
                                    fontSize = 8.sp,
                                    color = SunGold.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Subtitle: today indicator
                        Text(
                            text = if (day.isToday) "امروز" else "",
                            fontSize = (8.5 * fontScale).sp,
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
fun HomeSectionHeader(
    title: String,
    icon: ImageVector,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE8F0E1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SunGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = SandDark
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SoftBorder.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun SpecialAdhkarCard(
    title: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .wrapContentHeight()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.075f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = SandDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
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
                            Icon(
                                imageVector = Icons.Default.Grain,
                                contentDescription = null,
                                tint = SunGold,
                                modifier = Modifier.size(23.dp)
                            )
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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = NightBlue,
                        modifier = Modifier.size(19.dp)
                    )
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
                val categoryIcon = when (cat.id) {
                    "daily" -> Icons.Default.CalendarMonth
                    "ramadan" -> Icons.Default.DarkMode
                    "sleep" -> Icons.Default.Bedtime
                    "istikhara" -> Icons.Default.Psychology
                    else -> Icons.Default.MenuBook
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = SunGold,
                        modifier = Modifier.size(19.dp)
                    )
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F0E1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = SunGold,
                    modifier = Modifier.size(36.dp)
                )
            }
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
                            fontFamily = AmiriQuran,
                            fontSize = (18 * fontScale).sp,
                            lineHeight = (28 * fontScale).sp,
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

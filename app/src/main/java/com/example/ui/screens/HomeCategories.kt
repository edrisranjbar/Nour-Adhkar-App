package com.example.ui.screens

import java.util.Calendar
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdhkarData
import com.example.data.model.Category
import com.example.data.model.DhikrItem
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun CategoriesGrid(viewModel: AdhkarViewModel, fontScale: Float) {
    // Show rest of categories in a neat 2-column grid
    val separateSectionIds = setOf("morning", "evening", "sleep", "daily", "quran_prayers", "sunnah_prayers")
    val gridCategories = AdhkarData.categories.filterNot { it.id in separateSectionIds }

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
                        if (cat.isEnabled) viewModel.selectCategory(cat.id)
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
                                .background(MaterialTheme.colorScheme.secondaryContainer),
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
                                text = "ذکرشمار",
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
            .clickable(enabled = cat.isEnabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (cat.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
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
                    "waking_up" -> Icons.Default.Alarm
                    "entering_bathroom", "leaving_bathroom" -> Icons.Default.Wc
                    "clothing" -> Icons.Default.Checkroom
                    "before_eating" -> Icons.Default.Restaurant
                    "after_eating" -> Icons.Default.RestaurantMenu
                    "leaving_home" -> Icons.Default.Logout
                    "entering_home" -> Icons.Default.Login
                    "entering_mosque", "leaving_mosque" -> Icons.Default.Mosque
                    "after_salah" -> Icons.Default.SelfImprovement
                    "riding_vehicle" -> Icons.Default.DirectionsCar
                    "starting_journey" -> Icons.Default.FlightTakeoff
                    "returning_travel" -> Icons.Default.FlightLand
                    "night_restlessness" -> Icons.Default.Nightlight
                    "ramadan" -> Icons.Default.DarkMode
                    "sleep" -> Icons.Default.Bedtime
                    "istikhara" -> Icons.Default.Psychology
                    "after_adhan" -> Icons.Default.Campaign
                    "funeral_prayer" -> Icons.Default.VolunteerActivism
                    else -> iconForName(cat.iconName)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
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
                    text = when {
                        !cat.isEnabled -> "به‌زودی"
                        cat.id == "quran_prayers" -> "${cat.count.toPersianDigits()} دعا"
                        else -> "${cat.count.toPersianDigits()} ذکر"
                    },
                    fontSize = (10 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cat.isEnabled) SunGold else NightBlue.copy(alpha = 0.65f)
                )
            }

            Text(
                text = cat.title,
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (cat.isEnabled) SandDark else SandDark.copy(alpha = 0.55f),
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
    searchHeader: @Composable () -> Unit,
    onResultClick: (String) -> Unit
) {
    if (results.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            item { searchHeader() }
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
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
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            item { searchHeader() }
            items(results) { (catTitle, dhikr) ->
                // Every collection opens itself, not only the few that used to be listed here.
                val catId = AdhkarData.categories.firstOrNull { it.title == catTitle }?.id
                    ?: if (catTitle == "دعای خواب") "sleep" else "morning"
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
                                        color = MaterialTheme.colorScheme.secondaryContainer,
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
                        if (com.example.ui.language.LocalAppLanguage.current.showPersianTranslation) {
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
}

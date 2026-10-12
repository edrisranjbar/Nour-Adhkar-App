package com.example.ui.screens

import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdhkarData
import com.example.data.model.Category
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun HomeScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues,
    onOpenShortcut: (String) -> Unit = { viewModel.selectTab(it) }
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
            // Content Area - Switch between Search Results and Main Dashboard
            Box(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                    ) {
                        item {
                            HomeSearchField(
                                query = searchQuery,
                                fontScale = fontScale,
                                onQueryChange = viewModel::updateSearchQuery
                            )
                        }
                        // 0. Daily Progress -> Streak and Activity Calendar
                        item {
                            StreakCalendarCard(
                                viewModel = viewModel,
                                fontScale = fontScale
                            )
                        }

                        item { PrayerTimesCard(viewModel) }

                        // 1. Special Daily Adhkar Header
                        item {
                            HomeSectionHeader(
                                title = "اذکار و دعاها",
                                icon = Icons.Default.WbSunny,
                                fontScale = fontScale,
                                modifier = Modifier.padding(top = 6.dp),
                                actionLabel = "بیشتر",
                                onAction = { viewModel.selectTab("adhkar") }
                            )
                        }

                        // 2. Special Daily Cards (Morning & Evening)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Morning Card
                                SpecialAdhkarCard(
                                    title = "اذکار صبحگاه",
                                    badgeText = "${AdhkarData.adhkarList["morning"].orEmpty().size.toPersianDigits()} ذکر",
                                    icon = Icons.Default.WbSunny,
                                    accentColor = Color(0xFFD58B19),
                                    artworkRes = R.drawable.adhkar_morning_card,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("morning") }
                                )

                                // Evening Card
                                SpecialAdhkarCard(
                                    title = "اذکار شامگاه",
                                    badgeText = "${AdhkarData.adhkarList["evening"].orEmpty().size.toPersianDigits()} ذکر",
                                    icon = Icons.Default.NightsStay,
                                    accentColor = Color(0xFF53699A),
                                    artworkRes = R.drawable.adhkar_evening_card,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("evening") }
                                )
                            }
                        }

                        // Bedtime and daily adhkar share the row beneath morning and evening.
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SpecialAdhkarCard(
                                    title = "اذکار خواب",
                                    badgeText = "${AdhkarData.adhkarList["sleep"].orEmpty().size.toPersianDigits()} ذکر",
                                    icon = Icons.Default.Bedtime,
                                    accentColor = Color(0xFF59658F),
                                    artworkRes = R.drawable.adhkar_sleep_card,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("sleep") }
                                )
                                SpecialAdhkarCard(
                                    title = "اذکار روزانه",
                                    badgeText = "${AdhkarData.adhkarList["daily"].orEmpty().size.toPersianDigits()} ذکر",
                                    icon = Icons.Default.WbSunny,
                                    accentColor = Color(0xFF6B9678),
                                    artworkRes = R.drawable.adhkar_daily_card,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("daily") }
                                )
                            }
                        }

                        // 3. Daily verse based on the user's feeling
                        item {
                            EmotionalAyahCard(viewModel = viewModel, fontScale = fontScale)
                        }

                        // 4. Prayer collections are intentionally separate from general categories.
                        item {
                            HomeSectionHeader(
                                title = "دعا و نیایش",
                                icon = Icons.Default.MenuBook,
                                fontScale = fontScale,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SpecialAdhkarCard(
                                    title = "دعاهای قرآنی",
                                    badgeText = "${AdhkarData.adhkarList["quran_prayers"].orEmpty().size.toPersianDigits()} دعا",
                                    icon = Icons.Default.MenuBook,
                                    accentColor = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("quran_prayers") }
                                )
                                SpecialAdhkarCard(
                                    title = "دعاهایی از سنت رسول (ص)",
                                    badgeText = "${AdhkarData.adhkarList["sunnah_prayers"].orEmpty().size.toPersianDigits()} دعا",
                                    icon = Icons.Default.MenuBook,
                                    accentColor = Color(0xFF6D4C41),
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.selectCategory("sunnah_prayers") }
                                )
                            }
                        }

                        // Fast access to the five obligatory daily prayers.
                        item {
                            HomeObligatoryChecklist(
                                viewModel = viewModel,
                                fontScale = fontScale
                            )
                        }


                        item {
                            HomeQuickAccess(viewModel, fontScale, onOpenShortcut)
                        }

                        item {
                            MonthlyActivityCard(viewModel = viewModel, fontScale = fontScale)
                        }
                    }
                } else {
                    SearchResultsView(
                        results = searchResults,
                        fontScale = fontScale,
                        bottomPadding = innerPadding.calculateBottomPadding() + 16.dp,
                        searchHeader = {
                            HomeSearchField(
                                query = searchQuery,
                                fontScale = fontScale,
                                onQueryChange = viewModel::updateSearchQuery
                            )
                        },
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

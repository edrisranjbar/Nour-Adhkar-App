package com.example.ui.screens

import java.util.Calendar
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.viewmodel.AdhkarViewModel


@Composable
internal fun HomeSearchField(
    query: String,
    fontScale: Float,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
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
            Icon(Icons.Default.Search, contentDescription = "جستجو", tint = SunGold)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "پاک کردن جستجو", tint = SandDark)
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
}

@Composable
internal fun HomeObligatoryChecklist(
    viewModel: AdhkarViewModel,
    fontScale: Float
) {
    val completedIds by viewModel.dailyChecklistCompletedIds.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HomeSectionHeader(
            title = "فرائض روزانه",
            icon = Icons.Default.CheckCircle,
            fontScale = fontScale,
            modifier = Modifier.padding(top = 8.dp)
        )

        obligatoryChecklistItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    val completed = item.id in completedIds
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clickable {
                                viewModel.setDailyChecklistItemCompleted(item.id, !completed)
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (completed) SunGold.copy(alpha = 0.09f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (completed) SunGold.copy(alpha = 0.55f) else SoftBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (completed) Icons.Filled.CheckCircle
                                else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (completed) "انجام شده" else "انجام نشده",
                                tint = if (completed) SunGold else NightBlue.copy(alpha = 0.55f),
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = item.title,
                                color = if (completed) NightBlue.copy(alpha = 0.65f) else NightBlue,
                                fontSize = (12.5 * fontScale).sp,
                                fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

internal data class ActivityCalendarDay(
    val activityScore: Int,
    val isToday: Boolean
)

@Composable
internal fun MonthlyActivityCard(
    viewModel: AdhkarViewModel,
    fontScale: Float
) {
    val allProgress by viewModel.allProgress.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()
    val checklistCounts by viewModel.checklistCompletionCounts.collectAsState()

    val calendarRows = remember(allProgress, recentSessions, checklistCounts) {
        val firstDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -29)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = mutableListOf<ActivityCalendarDay>()

        repeat(30) { offset ->
            val day = (firstDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val start = day.timeInMillis
            val end = start + 86_400_000L - 1L
            val dhikrCount = allProgress
                .filter { it.lastUpdated in start..end }
                .sumOf { it.currentCount.coerceAtLeast(0) }
            val tasbihCount = recentSessions
                .filter { it.timestamp in start..end }
                .sumOf { it.count.coerceAtLeast(0) }
            val checklistScore = checklistCounts[start].orZero() * 3

            days += ActivityCalendarDay(
                activityScore = dhikrCount + tasbihCount + checklistScore,
                isToday = offset == 29
            )
        }
        days.chunked(10)
    }

    val activityColor: @Composable (Int) -> Color = { score ->
        when {
            score <= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            score <= 2 -> Color(0xFFDCECDD)
            score <= 5 -> Color(0xFFA8D5AA)
            score <= 10 -> Color(0xFF64AD68)
            else -> Color(0xFF2E7D32)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = SunGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "فعالیت ۳۰ روز گذشته",
                        fontSize = (13.5 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = SandDark
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                androidx.compose.material3.FilledTonalButton(
                    onClick = { viewModel.selectTab("stats") },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("آمار", fontSize = (12.5 * fontScale).sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                calendarRows.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(10) { column ->
                            val day = rowDays.getOrNull(column)
                            if (day == null) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(activityColor(day.activityScore))
                                        .then(
                                            if (day.isToday) Modifier.border(1.5.dp, SunGold, RoundedCornerShape(5.dp))
                                            else Modifier
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun Int?.orZero(): Int = this ?: 0

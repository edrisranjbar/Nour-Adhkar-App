package com.example.ui.screens

import java.util.Calendar
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.streak.StreakEngine
import com.example.streak.StreakState
import com.example.ui.components.StreakFreezeBadge
import com.example.ui.components.StreakCelebrationDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionEntity
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

/**
 * Streak state shared by Home, the drawer and the streak dialog. Today may still be pending, so a
 * streak ending yesterday counts; one missed day per week is covered by a freeze (see [StreakEngine]).
 */
@Composable
fun rememberStreakState(viewModel: AdhkarViewModel): StreakState {
    val allProgress by viewModel.allProgress.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()
    val activityDayKeys by viewModel.activityDayKeys.collectAsState()
    return remember(allProgress, recentSessions, activityDayKeys) {
        val now = Calendar.getInstance()
        val today = StreakEngine.dayNumber(now.timeInMillis)
        StreakEngine.compute(today) { day ->
            // Calendar.add keeps daylight-saving changes correct, unlike millisecond arithmetic.
            val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, (day - today).toInt()) }
            isDayActive(cal, allProgress, recentSessions, activityDayKeys)
        }
    }
}

/** Current consecutive-day streak (days of real activity; covered days do not add to it). */
@Composable
fun rememberCurrentStreak(viewModel: AdhkarViewModel): Int = rememberStreakState(viewModel).count

/** The last seven days, oldest first, marking days a weekly freeze covered (or may cover today). */
fun buildWeekActivity(
    progressList: List<DhikrProgressEntity>,
    sessions: List<TasbihSessionEntity>,
    activityDayKeys: Set<Long>,
    streak: StreakState
): List<DayActivity> {
    val now = Calendar.getInstance()
    val today = StreakEngine.dayNumber(now.timeInMillis)
    return (6 downTo 0).map { back ->
        val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back) }
        val day = today - back
        DayActivity(
            dayLabel = getPersianDayAbbreviation(cal.get(Calendar.DAY_OF_WEEK)),
            isActive = isDayActive(cal, progressList, sessions, activityDayKeys),
            isToday = back == 0,
            dateMillis = cal.timeInMillis,
            isFrozen = day in streak.frozenDays,
            isFreezePending = day == streak.pendingFreezeDay
        )
    }
}

@Composable
fun StreakCalendarCard(
    viewModel: AdhkarViewModel,
    fontScale: Float
) {
    val allProgress by viewModel.allProgress.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()
    val activityDayKeys by viewModel.activityDayKeys.collectAsState()
    val streakState = rememberStreakState(viewModel)
    val streak = streakState.count

    val days = remember(allProgress, recentSessions, activityDayKeys, streakState) {
        buildWeekActivity(allProgress, recentSessions, activityDayKeys, streakState)
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
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
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
                        .background(MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(10.dp))
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
                        text = "${streak.toPersianDigits()} روز متوالی",
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
                                .then(
                                    if (!day.isActive && (day.isFrozen || day.isFreezePending)) Modifier
                                    else Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                day.isActive -> MaterialTheme.colorScheme.secondaryContainer
                                                day.isToday -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .border(
                                            width = if (day.isToday && !day.isActive) 1.5.dp else 1.dp,
                                            color = when {
                                                day.isActive -> Color(0xFF4CAF50) // active green
                                                day.isFrozen || day.isFreezePending -> FreezeBlue
                                                day.isToday -> SunGold // gold border for today
                                                else -> SoftBorder.copy(alpha = 0.6f)
                                            },
                                            shape = CircleShape
                                        )
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
                            } else if (day.isFrozen || day.isFreezePending) {
                                StreakFreezeBadge(
                                    pending = day.isFreezePending,
                                    modifier = Modifier.size(26.dp)
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

            if (streakState.pendingFreezeDay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "دیروز را از دست دادید؛ امروز ذکری بخوانید تا سپر هفتگی زنجیره‌تان را حفظ کند.",
                    fontSize = (11 * fontScale).sp,
                    color = FreezeBlue
                )
            }
        }
    }
}

data class DayActivity(
    val dayLabel: String,
    val isActive: Boolean,
    val isToday: Boolean,
    val dateMillis: Long,
    /** A missed day a weekly freeze covered; the streak continues through it. */
    val isFrozen: Boolean = false,
    /** A missed yesterday that stays covered only if the user is active today. */
    val isFreezePending: Boolean = false
)

internal val FreezeBlue = Color(0xFF42A5F5)

fun isDayActive(
    cal: Calendar,
    progressList: List<DhikrProgressEntity>,
    sessions: List<TasbihSessionEntity>,
    activityDayKeys: Set<Long> = emptySet()
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

    return startMillis in activityDayKeys || progressActive || sessionActive
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

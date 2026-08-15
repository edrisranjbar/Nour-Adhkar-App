package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

private data class DailyChecklistItem(val id: String, val title: String)

private val dailyChecklistItems = listOf(
    DailyChecklistItem("morning_adhkar", "اذکار صبحگاه"),
    DailyChecklistItem("quran", "تلاوت قرآن"),
    DailyChecklistItem("duha", "نماز ضحی"),
    DailyChecklistItem("rawatib", "نمازهای سنت رواتب"),
    DailyChecklistItem("charity", "صدقه"),
    DailyChecklistItem("istighfar", "استغفار"),
    DailyChecklistItem("salawat", "صلوات بر پیامبر (ص)"),
    DailyChecklistItem("evening_adhkar", "اذکار شامگاه"),
    DailyChecklistItem("witr_tahajjud", "نماز وتر و تهجد"),
    DailyChecklistItem("sleep_adhkar", "اذکار خواب")
)

@Composable
fun DailyChecklistScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val completedIds by viewModel.dailyChecklistCompletedIds.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "چک‌لیست اعمال روزانه",
            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
            color = NightBlue,
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "پیشرفت امروز",
                color = NightBlue,
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${completedIds.size.toPersianDigits()} از ${dailyChecklistItems.size.toPersianDigits()}",
                color = SunGold,
                fontSize = (12 * fontScale).sp,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { completedIds.size / dailyChecklistItems.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            color = SunGold,
            trackColor = SoftBorder
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dailyChecklistItems, key = { it.id }) { item ->
                val completed = item.id in completedIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setDailyChecklistItemCompleted(item.id, !completed)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (completed) SunGold.copy(alpha = 0.08f) else CardBackground,
                    border = BorderStroke(
                        1.dp,
                        if (completed) SunGold.copy(alpha = 0.45f) else SoftBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (completed) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = if (completed) "انجام شده" else "انجام نشده",
                            tint = if (completed) SunGold else NightBlue.copy(alpha = 0.55f)
                        )
                        Text(
                            text = item.title,
                            color = if (completed) NightBlue.copy(alpha = 0.65f) else NightBlue,
                            fontSize = (15 * fontScale).sp,
                            fontWeight = if (completed) FontWeight.Medium else FontWeight.Normal,
                            textDecoration = if (completed) TextDecoration.LineThrough else null
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.calendar.Jalali
import com.example.stats.StatsAggregator
import com.example.stats.StatsInput
import com.example.stats.StatsMetric
import com.example.ui.components.StatsAreaChart
import com.example.ui.components.StatsBarChart
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.theme.SoftBorder
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text

private const val DAILY_WINDOW = 30

/** «آمار من»: daily, monthly and lifetime charts of the user's own activity. See docs/stats.md. */
@Composable
fun StatsScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val fontScale by viewModel.fontScale.collectAsState()
    val input by viewModel.statsInput.collectAsState()
    val streak = rememberStreakState(viewModel).count
    LaunchedEffect(Unit) { viewModel.refreshStats() }

    val data = input
    if (data == null) {
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val summary = remember(data) { StatsAggregator.summary(data) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile(Icons.Rounded.LocalFireDepartment, streak.toPersianDigits(), "روز پیاپی", fontScale, Modifier.weight(1f))
                SummaryTile(Icons.Rounded.CalendarMonth, summary.activeThisMonth.toPersianDigits(), "روز فعال این ماه", fontScale, Modifier.weight(1f))
                SummaryTile(Icons.Rounded.TouchApp, formatCount(summary.totalTasbih), "تسبیح تاکنون", fontScale, Modifier.weight(1f))
            }
        }
        if (summary.totalActiveDays < 3) {
            item(key = "empty") { EmptyStats(fontScale) }
        } else {
            item(key = "daily") { DailyCard(data, fontScale) }
            item(key = "monthly") { MonthlyCard(data, fontScale) }
            item(key = "overall") { OverallCard(data, fontScale) }
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun DailyCard(input: StatsInput, fontScale: Float) {
    val language = LocalAppLanguage.current
    var pagesBack by rememberSaveable { mutableIntStateOf(0) }
    val endJdn = input.todayJdn - pagesBack * DAILY_WINDOW
    val days = remember(input, endJdn) { StatsAggregator.daily(input, endJdn, DAILY_WINDOW) }
    var selected by rememberSaveable(endJdn) { mutableIntStateOf(days.lastIndex) }
    val day = days[selected.coerceIn(0, days.lastIndex)]
    val firstDay = StatsAggregator.firstDay(input)

    StatsCard(title = "روزانه", subtitle = "تسبیح هر روز؛ نقطه یعنی کارهای چک‌لیست هم انجام شده", fontScale = fontScale) {
        SelectionLine(
            primary = dayLabel(day.jdn, language),
            secondary = when {
                day.tasbih == 0 && day.checklist == 0 -> language.text("بدون فعالیت ثبت‌شده")
                else -> listOfNotNull(
                    "${day.tasbih} ${language.text("تسبیح")}".takeIf { day.tasbih > 0 },
                    "${day.checklist} ${language.text("مورد چک‌لیست")}".takeIf { day.checklist > 0 }
                ).joinToString(" · ")
            },
            fontScale = fontScale
        )
        StatsBarChart(
            values = days.map { it.tasbih },
            labels = days.mapIndexed { i, d -> if ((days.size - 1 - i) % 5 == 0) Jalali.fromJdn(d.jdn).day.toPersianDigits() else null },
            selected = selected,
            onSelect = { selected = it },
            marks = days.map { it.checklist > 0 },
            oldestOnLeft = true,
            description = "${language.text("نمودار روزانه")}: ${dayLabel(days.first().jdn, language)} – ${dayLabel(days.last().jdn, language)}"
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { pagesBack++ },
                    enabled = firstDay != null && endJdn - DAILY_WINDOW >= firstDay
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "بازه قبل") }
                Text(
                    text = "${dayLabel(days.first().jdn, language)} – ${dayLabel(days.last().jdn, language)}".toPersianDigits(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { pagesBack-- }, enabled = pagesBack > 0) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "بازه بعد")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyCard(input: StatsInput, fontScale: Float) {
    val language = LocalAppLanguage.current
    var metric by rememberSaveable { mutableStateOf(StatsMetric.ACTIVE_DAYS) }
    val months = remember(input) { StatsAggregator.monthly(input) }
    var selected by rememberSaveable { mutableIntStateOf(months.lastIndex) }
    val month = months[selected.coerceIn(0, months.lastIndex)]

    StatsCard(title = "ماهانه", subtitle = "دوازده ماه اخیر", fontScale = fontScale) {
        MetricSwitch(metric, onChange = { metric = it }, fontScale = fontScale)
        SelectionLine(
            primary = "${language.text(Jalali.monthNames[month.month - 1])} ${month.year}",
            secondary = when (metric) {
                StatsMetric.ACTIVE_DAYS -> "${month.activeDays} ${language.text("از")} ${month.length} ${language.text("روز فعال")}"
                StatsMetric.TASBIH -> "${month.tasbih} ${language.text("تسبیح")}"
                StatsMetric.CHECKLIST -> "${month.checklist} ${language.text("مورد چک‌لیست")}"
            },
            fontScale = fontScale
        )
        StatsBarChart(
            values = months.map { it.value(metric) },
            labels = months.mapIndexed { i, m ->
                if (i == selected || (months.size - 1 - i) % 3 == 0) language.text(Jalali.monthNames[m.month - 1]) else null
            },
            selected = selected,
            onSelect = { selected = it },
            description = language.text("نمودار ماهانه")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverallCard(input: StatsInput, fontScale: Float) {
    val language = LocalAppLanguage.current
    var metric by rememberSaveable { mutableStateOf(StatsMetric.ACTIVE_DAYS) }
    var range by rememberSaveable { mutableStateOf<Int?>(null) }
    val points = remember(input, metric, range) { StatsAggregator.cumulative(input, metric, range) }
    val summary = remember(input) { StatsAggregator.summary(input) }

    StatsCard(title = "کل مسیر", subtitle = "جمع پیشرفت شما از ابتدا", fontScale = fontScale) {
        MetricSwitch(metric, onChange = { metric = it }, fontScale = fontScale)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(90 to "۳ ماه", 180 to "۶ ماه", 365 to "۱ سال", null to "همه").forEach { (days, label) ->
                FilterChip(
                    selected = range == days,
                    onClick = { range = days },
                    label = { Text(label, fontSize = (12 * fontScale).sp) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
        if (points.size >= 2) {
            StatsAreaChart(
                values = points.map { it.total },
                startLabel = dayLabel(points.first().jdn, language),
                endLabel = language.text("امروز"),
                description = "${language.text("نمودار کل مسیر")}: ${points.last().total}"
            )
        }
        summary.firstDay?.let { first ->
            Text(
                text = "${language.text("از")} ${dayLabel(first, language)} ${language.text("تاکنون")}: " +
                    "${summary.totalTasbih} ${language.text("تسبیح در")} ${summary.totalActiveDays} ${language.text("روز فعال")}",
                fontSize = (12.5 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (20 * fontScale).sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricSwitch(metric: StatsMetric, onChange: (StatsMetric) -> Unit, fontScale: Float) {
    val options = listOf(StatsMetric.ACTIVE_DAYS to "روزهای فعال", StatsMetric.TASBIH to "تسبیح", StatsMetric.CHECKLIST to "چک‌لیست")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = metric == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {}
            ) { Text(label, fontSize = (12.5 * fontScale).sp, maxLines = 1) }
        }
    }
}

@Composable
private fun SelectionLine(primary: String, secondary: String, fontScale: Float) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(primary.toPersianDigits(), fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text(
            secondary.toPersianDigits(),
            fontSize = (12.5 * fontScale).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun StatsCard(title: String, subtitle: String, fontScale: Float, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.85f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                Text(title, fontSize = (17 * fontScale).sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = (12 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, value: String, label: String, fontScale: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 104.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.85f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)) }
            Text(value, fontSize = (22 * fontScale).sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, fontSize = (11.5 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = (16 * fontScale).sp)
        }
    }
}

@Composable
private fun EmptyStats(fontScale: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
            Text(
                "با چند روز ذکر، نمودارهای شما این‌جا شکل می‌گیرند.",
                fontSize = (14 * fontScale).sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (23 * fontScale).sp
            )
        }
    }
}

private fun dayLabel(jdn: Int, language: AppLanguage): String {
    val date = Jalali.fromJdn(jdn)
    return "${date.day} ${language.text(Jalali.monthNames[date.month - 1])}".toPersianDigits()
}

/** «۱۲٬۳۴۰» with a Persian thousands separator. */
private fun formatCount(value: Int): String =
    String.format(java.util.Locale.US, "%,d", value).replace(',', '٬').toPersianDigits()

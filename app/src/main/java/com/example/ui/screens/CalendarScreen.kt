package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.CalendarDay
import com.example.calendar.CalendarMonth
import com.example.calendar.Gregorian
import com.example.calendar.Hijri
import com.example.calendar.HijriDate
import com.example.calendar.Jalali
import com.example.calendar.MonthOccasion
import com.example.calendar.Occasion
import com.example.calendar.Weekday
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.theme.SoftBorder
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text

/** Pages either side of the current month in the swipeable grid (≈100 years each way). */
private const val PAGE_ORIGIN = 1200
/** Cell height: compact, but with the cell width (~48dp on a phone) still a comfortable touch target. */
private val CELL_MIN_HEIGHT = 46.dp

/** What the user did on a day, read from existing activity, tasbih and checklist data. */
private data class DayProgress(val active: Boolean, val tasbihCount: Int, val checklistItems: Int)

/**
 * Jalali month calendar with Hijri dates, fixed religious occasions and the user's own activity.
 * Fully offline; see docs/calendar.md.
 */
@Composable
fun CalendarScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val fontScale by viewModel.fontScale.collectAsState()
    val hijriOffset by viewModel.hijriOffset.collectAsState()
    val allProgress by viewModel.allProgress.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()
    val activityDayKeys by viewModel.activityDayKeys.collectAsState()
    val checklistCounts by viewModel.checklistCompletionCounts.collectAsState()

    val todayJdn = remember { Gregorian.jdnOf(System.currentTimeMillis()) }
    val today = remember(todayJdn) { Jalali.fromJdn(todayJdn) }
    val pagerState = rememberPagerState(initialPage = PAGE_ORIGIN) { PAGE_ORIGIN * 2 }
    val scope = rememberCoroutineScope()
    var selectedJdn by rememberSaveable { mutableIntStateOf(todayJdn) }
    var showOffsetSheet by rememberSaveable { mutableStateOf(false) }

    fun monthAt(page: Int): Pair<Int, Int> = Jalali.shiftMonth(today.year, today.month, page - PAGE_ORIGIN)

    val visibleMonth = remember(pagerState.currentPage, hijriOffset) {
        monthAt(pagerState.currentPage).let { (year, month) -> CalendarMonth.build(year, month, hijriOffset) }
    }

    // A swipe or arrow moves the selection into the new month so the day panel never shows
    // a day from a month that is no longer on screen.
    LaunchedEffect(pagerState.settledPage) {
        val (year, month) = monthAt(pagerState.settledPage)
        if (Jalali.fromJdn(selectedJdn).let { it.year != year || it.month != month }) {
            selectedJdn = if (year == today.year && month == today.month) todayJdn else Jalali.toJdn(year, month, 1)
        }
    }

    val progressOf: (Int) -> DayProgress? = remember(allProgress, recentSessions, activityDayKeys, checklistCounts) {
        { jdn ->
            if (jdn > todayJdn) null else {
                val start = Gregorian.startOfDayMillis(jdn)
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                val end = Gregorian.startOfDayMillis(jdn + 1) - 1
                DayProgress(
                    active = isDayActive(cal, allProgress, recentSessions, activityDayKeys),
                    tasbihCount = recentSessions.filter { it.timestamp in start..end }.sumOf { it.count.coerceAtLeast(0) },
                    checklistItems = checklistCounts[start] ?: 0
                )
            }
        }
    }

    val selectedDay = visibleMonth.dayOf(selectedJdn) ?: visibleMonth.dayOf(todayJdn) ?: visibleMonth.days.first()
    val isOnToday = pagerState.currentPage == PAGE_ORIGIN && selectedJdn == todayJdn

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "month") {
            MonthCard(
                month = visibleMonth,
                fontScale = fontScale,
                showTodayButton = !isOnToday,
                onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                onToday = {
                    selectedJdn = todayJdn
                    scope.launch { pagerState.animateScrollToPage(PAGE_ORIGIN) }
                }
            ) {
                HorizontalPager(
                    state = pagerState,
                    // Months need 5 or 6 rows; animate the difference instead of jumping.
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    beyondViewportPageCount = 1,
                    key = { it }
                ) { page ->
                    val month = remember(page, hijriOffset) {
                        monthAt(page).let { (year, m) -> CalendarMonth.build(year, m, hijriOffset) }
                    }
                    MonthGrid(
                        month = month,
                        todayJdn = todayJdn,
                        selectedJdn = selectedJdn,
                        fontScale = fontScale,
                        onSelect = { selectedJdn = it }
                    )
                }
            }
        }

        item(key = "day") {
            SelectedDayCard(
                day = selectedDay,
                todayJdn = todayJdn,
                progress = progressOf(selectedDay.jdn),
                fontScale = fontScale
            )
        }

        if (visibleMonth.occasions.isNotEmpty()) item(key = "occasions") {
            MonthOccasionsCard(
                month = visibleMonth,
                selectedJdn = selectedDay.jdn,
                fontScale = fontScale,
                onSelect = { selectedJdn = it }
            )
        }

        item(key = "hijri-note") {
            HijriNote(fontScale = fontScale, offset = hijriOffset, onAdjust = { showOffsetSheet = true })
        }
    }

    if (showOffsetSheet) {
        HijriOffsetSheet(
            offset = hijriOffset,
            todayJdn = todayJdn,
            fontScale = fontScale,
            onChange = viewModel::setHijriOffset,
            onDismiss = { showOffsetSheet = false }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Month card

@Composable
private fun MonthCard(
    month: CalendarMonth,
    fontScale: Float,
    showTodayButton: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    grid: @Composable () -> Unit
) {
    val language = LocalAppLanguage.current
    CalendarCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { heading() }
            ) {
                Text(
                    text = "${language.text(Jalali.monthNames[month.month - 1])} ${month.year}".toPersianDigits(),
                    fontSize = (19 * fontScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = localizedSpan(month.hijriSpan, language).toPersianDigits(),
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(
                visible = showTodayButton,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("امروز", fontSize = (13 * fontScale).sp, fontWeight = FontWeight.SemiBold)
                }
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "ماه قبل")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "ماه بعد")
            }
        }

        Spacer(Modifier.height(8.dp))
        WeekdayHeader(fontScale)
        Spacer(Modifier.height(4.dp))
        grid()
    }
}

@Composable
private fun WeekdayHeader(fontScale: Float) {
    Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        Weekday.initials.forEachIndexed { index, initial ->
            Text(
                text = initial,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = (12.5 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = if (index == Weekday.FRIDAY) MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: CalendarMonth,
    todayJdn: Int,
    selectedJdn: Int,
    fontScale: Float,
    onSelect: (Int) -> Unit
) {
    val rows = (month.leadingBlanks + month.days.size + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { column ->
                    val day = month.days.getOrNull(row * 7 + column - month.leadingBlanks)
                    if (day == null) {
                        Spacer(Modifier.weight(1f).heightIn(min = CELL_MIN_HEIGHT))
                    } else {
                        DayCell(
                            day = day,
                            isToday = day.jdn == todayJdn,
                            isSelected = day.jdn == selectedJdn,
                            fontScale = fontScale,
                            onClick = { onSelect(day.jdn) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    fontScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val language = LocalAppLanguage.current
    val shape = RoundedCornerShape(12.dp)
    val background by animateColorAsState(
        targetValue = when {
            isToday -> colors.tertiary
            else -> Color.Transparent
        },
        animationSpec = tween(220),
        label = "dayBackground"
    )
    val numberColor = when {
        isToday -> colors.surface
        day.isFriday -> colors.error
        else -> colors.onSurface
    }
    val hijriColor = when {
        isToday -> colors.surface.copy(alpha = 0.85f)
        else -> colors.onSurfaceVariant.copy(alpha = 0.7f)
    }
    val description = buildString {
        append(language.text(Weekday.names[day.weekday])).append(' ')
        append(day.jalali.day).append(' ').append(language.text(Jalali.monthNames[day.jalali.month - 1])).append("، ")
        append(day.hijri.day).append(' ').append(language.text(Hijri.monthNames[day.hijri.month - 1]))
        if (isToday) append("، ").append(language.text("امروز"))
        day.occasions.forEach { append("، ").append(language.text(it.title)) }
    }

    Box(
        modifier = modifier
            .heightIn(min = CELL_MIN_HEIGHT)
            .clip(shape)
            .background(background)
            .then(
                if (isSelected && !isToday) Modifier.border(1.5.dp, colors.tertiary, shape)
                else if (isSelected) Modifier.border(2.dp, colors.onSurface.copy(alpha = 0.18f), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (language == com.example.ui.language.AppLanguage.ARABIC) arabicDigits(description) else description.toPersianDigits()
                role = Role.Button
                selected = isSelected
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { }
        ) {
            Text(
                text = day.jalali.day.toPersianDigits(),
                fontSize = (15 * fontScale).sp,
                fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = numberColor,
                lineHeight = (17 * fontScale).sp
            )
            Text(
                text = day.hijri.day.toPersianDigits(),
                fontSize = (9.5 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = hijriColor,
                lineHeight = (11 * fontScale).sp
            )
            Spacer(Modifier.height(2.dp))
            // Reserve the dot's space on every day so numbers line up across the row.
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            day.occasions.isEmpty() -> Color.Transparent
                            isToday -> colors.surface
                            else -> occasionColor()
                        }
                    )
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Selected day

@Composable
private fun SelectedDayCard(day: CalendarDay, todayJdn: Int, progress: DayProgress?, fontScale: Float) {
    val colors = MaterialTheme.colorScheme
    val language = LocalAppLanguage.current
    CalendarCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (day.jdn == todayJdn) colors.tertiary else colors.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.jalali.day.toPersianDigits(),
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (day.jdn == todayJdn) colors.surface else colors.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${language.text(Weekday.names[day.weekday])} ${day.jalali.day} " +
                        "${language.text(Jalali.monthNames[day.jalali.month - 1])} ${day.jalali.year}".toPersianDigits(),
                    fontSize = (15 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = ("${hijriLabel(day.hijri, language)} · " +
                        "${day.gregorian.day} ${language.text(Gregorian.monthNames[day.gregorian.month - 1])} ${day.gregorian.year}").toPersianDigits(),
                    fontSize = (12 * fontScale).sp,
                    color = colors.onSurfaceVariant
                )
            }
            RelativeDayChip(day.jdn - todayJdn, fontScale)
        }

        if (day.occasions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.occasions.forEach { OccasionDetail(it, fontScale) }
            }
        }

        if (progress != null) {
            Spacer(Modifier.height(12.dp))
            ProgressRow(progress, isToday = day.jdn == todayJdn, fontScale = fontScale)
        }
    }
}

@Composable
private fun RelativeDayChip(daysFromToday: Int, fontScale: Float) {
    val label = when {
        daysFromToday == 0 -> "امروز"
        daysFromToday == 1 -> "فردا"
        daysFromToday == -1 -> "دیروز"
        daysFromToday > 0 -> "$daysFromToday روز دیگر"
        else -> "${-daysFromToday} روز پیش"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label.toPersianDigits(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = (11.5 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OccasionDetail(occasion: Occasion, fontScale: Float) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tertiaryContainer.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = colors.onTertiaryContainer,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occasion.title,
                fontSize = (14.5 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = colors.onTertiaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = occasion.detail,
                fontSize = (13 * fontScale).sp,
                color = colors.onSurface.copy(alpha = 0.85f),
                lineHeight = (21 * fontScale).sp
            )
            occasion.source?.let { source ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "منبع: $source",
                    fontSize = (11.5 * fontScale).sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(progress: DayProgress, isToday: Boolean, fontScale: Float) {
    val colors = MaterialTheme.colorScheme
    val details = buildList {
        if (progress.tasbihCount > 0) add("${progress.tasbihCount} ذکر تسبیح")
        if (progress.checklistItems > 0) add("${progress.checklistItems} مورد از چک‌لیست")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (progress.active) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (progress.active) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (progress.active) colors.tertiary else colors.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    progress.active -> "فعالیت این روز ثبت شده است"
                    isToday -> "هنوز فعالیتی برای امروز ثبت نشده"
                    else -> "فعالیتی در این روز ثبت نشده"
                },
                fontSize = (13.5 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (progress.active) colors.onPrimaryContainer else colors.onSurfaceVariant
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · ").toPersianDigits(),
                    fontSize = (12 * fontScale).sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Month occasions

@Composable
private fun MonthOccasionsCard(
    month: CalendarMonth,
    selectedJdn: Int,
    fontScale: Float,
    onSelect: (Int) -> Unit
) {
    val language = LocalAppLanguage.current
    CalendarCard(contentPadding = PaddingValues(vertical = 12.dp)) {
        Text(
            text = "مناسبت‌های ${language.text(Jalali.monthNames[month.month - 1])}",
            fontSize = (15 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 18.dp).semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        if (month.occasions.isEmpty()) {
            Text(
                text = "در این ماه مناسبت ثبت‌شده‌ای نیست.",
                fontSize = (13 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        } else {
            month.occasions.forEach { item ->
                OccasionRow(
                    item = item,
                    selected = selectedJdn in item.first.jdn..item.last.jdn,
                    fontScale = fontScale,
                    onClick = { onSelect(item.first.jdn) }
                )
            }
        }
    }
}

@Composable
private fun OccasionRow(item: MonthOccasion, selected: Boolean, fontScale: Float, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = LocalAppLanguage.current
    val rowBackground by animateColorAsState(
        if (selected) colors.secondaryContainer.copy(alpha = 0.7f) else Color.Transparent,
        label = "occasionRow"
    )
    val dateLabel = if (item.first.jdn == item.last.jdn) hijriLabel(item.first.hijri, language)
    else "${item.first.hijri.day} ${language.text("تا")} ${item.last.hijri.day} ${language.text(Hijri.monthNames[item.last.hijri.month - 1])}"
    val weekdayLabel = if (item.first.jdn == item.last.jdn) language.text(Weekday.names[item.first.weekday])
    else "${language.text(Weekday.names[item.first.weekday])} ${language.text("تا")} ${language.text(Weekday.names[item.last.weekday])}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .sizeIn(minWidth = 52.dp, minHeight = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.tertiaryContainer.copy(alpha = 0.7f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = (if (item.first.jdn == item.last.jdn) "${item.first.jalali.day}"
                else "${item.first.jalali.day}–${item.last.jalali.day}").toPersianDigits(),
                fontSize = (17 * fontScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onTertiaryContainer,
                lineHeight = (19 * fontScale).sp
            )
            Text(
                text = language.text(Jalali.monthNames[item.first.jalali.month - 1]),
                fontSize = (10 * fontScale).sp,
                color = colors.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.occasion.title,
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = "$weekdayLabel · $dateLabel".toPersianDigits(),
                fontSize = (12 * fontScale).sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Hijri calculation note and offset

@Composable
private fun HijriNote(fontScale: Float, offset: Int, onAdjust: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = (if (offset == 0) "تاریخ قمری: تقویم ام‌القری" else "تاریخ قمری: ام‌القری، ${offsetLabel(offset)}").toPersianDigits(),
            fontSize = (12 * fontScale).sp,
            lineHeight = (19 * fontScale).sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAdjust) {
            Text("تنظیم", fontSize = (13 * fontScale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HijriOffsetSheet(
    offset: Int,
    todayJdn: Int,
    fontScale: Float,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val language = LocalAppLanguage.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "تنظیم تاریخ قمری",
                fontSize = (18 * fontScale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "اگر آغاز ماه قمری در منطقه شما با تقویم ام‌القری فرق دارد، تاریخ را حداکثر دو روز جابه‌جا کنید. این تنظیم فقط تاریخ قمری و مناسبت‌های قمری را تغییر می‌دهد.",
                fontSize = (13 * fontScale).sp,
                lineHeight = (21 * fontScale).sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(Icons.Rounded.Remove, "یک روز عقب‌تر", enabled = offset > -Hijri.MAX_OFFSET) { onChange(offset - 1) }
                Column(
                    modifier = Modifier.width(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (if (offset > 0) "+$offset" else "$offset").toPersianDigits(),
                        fontSize = (34 * fontScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = (if (offset == 0) "بدون اصلاح" else offsetLabel(offset)).toPersianDigits(),
                        fontSize = (12 * fontScale).sp,
                        color = colors.onSurfaceVariant
                    )
                }
                StepButton(Icons.Rounded.Add, "یک روز جلوتر", enabled = offset < Hijri.MAX_OFFSET) { onChange(offset + 1) }
            }
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = colors.secondaryContainer) {
                Text(
                    text = "امروز: ${hijriLabel(Hijri.fromJdn(todayJdn, offset), language)}".toPersianDigits(),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSecondaryContainer
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("تأیید", fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Bold)
            }
            if (offset != 0) {
                TextButton(onClick = { onChange(0) }) {
                    Text("بازگشت به محاسبه ام‌القری", fontSize = (13 * fontScale).sp)
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(56.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(26.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Shared pieces

@Composable
private fun CalendarCard(
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Warm accent for occasion markers, from the shared theme's tertiary container roles. */
@Composable
private fun occasionColor(): Color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)

private fun hijriLabel(date: HijriDate, language: com.example.ui.language.AppLanguage): String =
    "${date.day} ${language.text(Hijri.monthNames[date.month - 1])} ${date.year}"

private fun offsetLabel(offset: Int): String =
    if (offset > 0) "$offset روز جلوتر" else "${-offset} روز عقب‌تر"

/** Translates the month names inside a «ماه – ماه سال» span one by one. */
private fun localizedSpan(span: String, language: com.example.ui.language.AppLanguage): String =
    span.split(' ').joinToString(" ") { language.text(it) }

private fun arabicDigits(text: String): String = text.map {
    if (it in '0'..'9') '٠' + (it - '0') else it
}.joinToString("")

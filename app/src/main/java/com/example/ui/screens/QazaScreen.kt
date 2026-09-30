package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qaza.FastEntry
import com.example.qaza.QazaAction
import com.example.qaza.QazaActionType
import com.example.qaza.QazaCalculator
import com.example.qaza.QazaPeriod
import com.example.qaza.QazaPrayer
import com.example.qaza.QazaState
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.theme.CardBackground
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.util.formatPersianDate
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

private const val ESTIMATE_NOTE =
    "این فقط یک برآورد است، نه حکم شرعی. هر عدد را پیش از ذخیره می‌توانید تغییر دهید و تا زدن «ذخیره» چیزی ثبت نمی‌شود."

/** Missed prayers (قضای نماز) and missed fasts (قضای روزه). Everything is counted by the user. */
@Composable
fun QazaScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val state by viewModel.qaza.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
    ) {
        SegmentedPills(
            labels = listOf("نماز", "روزه"),
            selected = tab,
            onSelect = { tab = it },
            fontScale = fontScale,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (tab == 0) {
            PrayersTab(state, viewModel, fontScale, innerPadding)
        } else {
            FastsTab(state, viewModel, fontScale, innerPadding)
        }
    }
}

/** A compact card for the daily checklist that shows what is still owed and opens the tracker. */
@Composable
fun QazaChecklistCard(viewModel: AdhkarViewModel, onOpen: () -> Unit) {
    val state by viewModel.qaza.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val title = when {
        state.totalRemaining > 0 -> "قضای نماز: ${state.totalRemaining.toPersianDigits()} باقی‌مانده"
        state.hasPrayerData -> "قضای نمازها ادا شده است"
        else -> "قضای نماز و روزه"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NightBlue, fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Medium)
                Text(
                    if (state.hasPrayerData) "باز کردن ردیاب قضا" else "نمازها و روزه‌های فوت‌شده را بشمارید",
                    color = NightBlue.copy(alpha = 0.65f),
                    fontSize = (12 * fontScale).sp
                )
            }
            Text("‹", color = SunGold, fontSize = (22 * fontScale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun SegmentedPills(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) SunGold else Color.Transparent,
                contentColor = if (isSelected) Color.White else NightBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        fontSize = (15 * fontScale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QazaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ProgressRing(progress: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val track = SoftBorder
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = stroke)
            )
            drawArc(
                color = SunGold, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** Reads a whole number typed in Persian, Arabic-Indic or Latin digits. */
private fun parseCount(text: String): Int? {
    val latin = buildString {
        text.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
    }
    return latin.trim().toIntOrNull()?.takeIf { it >= 0 }
}

@Composable
private fun CountEditDialog(
    title: String,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val value = parseCount(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(6) },
                singleLine = true,
                isError = value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

private fun describe(action: QazaAction): String {
    val prayer = action.changes.singleOrNull()?.prayer?.title
    return when (action.type) {
        QazaActionType.MADE_UP -> "ادای نماز $prayer"
        QazaActionType.MISSED -> "فوت نماز $prayer"
        QazaActionType.EDIT -> "ویرایش تعداد نماز $prayer"
        QazaActionType.SETUP -> "ثبت برآورد اولیه"
    }
}

// ---------------------------------------------------------------------------------------------
// Prayers
// ---------------------------------------------------------------------------------------------

@Composable
private fun PrayersTab(
    state: QazaState,
    viewModel: AdhkarViewModel,
    fontScale: Float,
    innerPadding: PaddingValues
) {
    var manualStart by rememberSaveable { mutableStateOf(false) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val started = state.hasPrayerData || manualStart
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!started) {
            item(key = "empty") {
                QazaCard {
                    Text(
                        "قضای نمازهای فوت‌شده را اینجا بشمارید و یکی‌یکی ادا کنید. عددها فقط روی همین دستگاه می‌مانند و برنامه حکم شرعی صادر نمی‌کند.",
                        color = NightBlue,
                        fontSize = (15 * fontScale).sp,
                        lineHeight = (24 * fontScale).sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showSetup = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) { Text("شروع (برآورد تعداد)") }
                    OutlinedButton(
                        onClick = { manualStart = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 48.dp)
                    ) { Text("وارد کردن عدد") }
                }
            }
        } else {
            item(key = "summary") { PrayerSummary(state, viewModel, fontScale, now) }
            items(state.activePrayers, key = { it.id }) { prayer ->
                PrayerRow(
                    prayer = prayer,
                    remaining = state.debt(prayer).remaining,
                    madeUp = state.debt(prayer).madeUp,
                    owed = state.debt(prayer).owed,
                    fontScale = fontScale,
                    onMadeUp = { viewModel.qazaMadeUp(prayer) },
                    onMissed = { viewModel.qazaAddMissed(prayer) },
                    onEdit = { editingId = prayer.id }
                )
            }
            item(key = "witr") {
                QazaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("شمارش نماز وتر", color = NightBlue, fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Medium)
                            Text(
                                "شمارش وتر میان سنت‌ها فرق دارد؛ به انتخاب خودتان.",
                                color = NightBlue.copy(alpha = 0.65f),
                                fontSize = (12 * fontScale).sp
                            )
                        }
                        Switch(checked = state.witrEnabled, onCheckedChange = { viewModel.qazaSetWitrEnabled(it) })
                    }
                }
            }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showSetup = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text("برآورد تعداد") }
                    OutlinedButton(
                        onClick = { viewModel.qazaUndo() },
                        enabled = state.history.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text("واگرد آخرین عمل") }
                }
            }
            if (state.history.isNotEmpty()) {
                item(key = "history_title") {
                    Text(
                        "آخرین فعالیت‌ها",
                        color = SunGold,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.history.takeLast(8).asReversed()) { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(describe(action), color = NightBlue, fontSize = (13 * fontScale).sp)
                        Text(
                            formatPersianDate(action.time),
                            color = NightBlue.copy(alpha = 0.6f),
                            fontSize = (12 * fontScale).sp
                        )
                    }
                }
            }
        }
    }

    if (showSetup) {
        QazaSetupSheet(
            witrDefault = state.witrEnabled,
            replacesExisting = state.totalOwed > 0,
            onDismiss = { showSetup = false },
            onConfirm = { estimate ->
                viewModel.qazaApplyEstimate(estimate)
                showSetup = false
            }
        )
    }
    editingId?.let { id ->
        QazaPrayer.fromId(id)?.let { prayer ->
            CountEditDialog(
                title = "تعداد باقی‌مانده‌ی نماز ${prayer.title}",
                initial = state.debt(prayer).remaining,
                onDismiss = { editingId = null },
                onConfirm = { value ->
                    viewModel.qazaSetRemaining(prayer, value)
                    editingId = null
                }
            )
        }
    }
}

@Composable
private fun PrayerSummary(state: QazaState, viewModel: AdhkarViewModel, fontScale: Float, now: Long) {
    val progress = if (state.totalOwed == 0) 0f else state.totalMadeUp / state.totalOwed.toFloat()
    QazaCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressRing(progress = progress, modifier = Modifier.size(96.dp)) {
                Text(
                    "${(progress * 100).toInt().toPersianDigits()}٪",
                    color = NightBlue,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${state.totalRemaining.toPersianDigits()} نماز باقی‌مانده",
                    color = NightBlue,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "از ${state.totalOwed.toPersianDigits()} نماز، ${state.totalMadeUp.toPersianDigits()} ادا شده",
                    color = NightBlue.copy(alpha = 0.7f),
                    fontSize = (13 * fontScale).sp
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = SoftBorder)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("هدف روزانه", color = NightBlue, fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Medium)
                Text(
                    "هر «دست» یک نماز از هر وعده است",
                    color = NightBlue.copy(alpha = 0.65f),
                    fontSize = (12 * fontScale).sp
                )
            }
            IconButton(
                onClick = { viewModel.qazaSetDailySets(state.dailySets - 1) },
                enabled = state.dailySets > 1,
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Remove, contentDescription = "کمتر") }
            Text(
                "${state.dailySets.toPersianDigits()} دست",
                color = NightBlue,
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { viewModel.qazaSetDailySets(state.dailySets + 1) },
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "بیشتر") }
        }
        val finish = state.estimatedFinish(now)
        val finishText = when {
            state.totalOwed == 0 -> null
            state.totalRemaining == 0 -> "همه ادا شد"
            finish != null -> "پایان تقریبی: ${formatPersianDate(finish.time)} (تخمینی)"
            else -> null
        }
        finishText?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 4.dp),
                color = SunGold,
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: QazaPrayer,
    remaining: Int,
    madeUp: Int,
    owed: Int,
    fontScale: Float,
    onMadeUp: () -> Unit,
    onMissed: () -> Unit,
    onEdit: () -> Unit
) {
    QazaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "نماز ${prayer.title}",
                    color = NightBlue,
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${madeUp.toPersianDigits()} ادا شده از ${owed.toPersianDigits()}",
                    color = NightBlue.copy(alpha = 0.65f),
                    fontSize = (12 * fontScale).sp
                )
            }
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    remaining.toPersianDigits(),
                    color = SunGold,
                    fontSize = (24 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = "ویرایش عدد", tint = SunGold, modifier = Modifier.size(16.dp))
            }
        }
        LinearProgressIndicator(
            progress = { if (owed == 0) 0f else madeUp / owed.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = SunGold,
            trackColor = SoftBorder
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onMadeUp,
                enabled = remaining > 0,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("ادا شد")
            }
            OutlinedButton(
                onClick = onMissed,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("یکی فوت شد")
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Setup estimate
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QazaSetupSheet(
    witrDefault: Boolean,
    replacesExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Map<QazaPrayer, Int>) -> Unit
) {
    var days by rememberSaveable { mutableStateOf("") }
    var includeWitr by rememberSaveable { mutableStateOf(witrDefault) }

    val estimate = QazaCalculator.estimate(QazaPeriod(days = parseCount(days) ?: 0), 0, includeWitr)
    // Numbers edited by hand are dropped whenever the inputs change, so they never go stale.
    val signature = "$days|$includeWitr"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("برآورد نمازهای فوت‌شده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(ESTIMATE_NOTE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumberField("تعداد روزهایی که نماز نخوانده‌اید", days) { days = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeWitr, onCheckedChange = { includeWitr = it })
                Text("نماز وتر هم برآورد شود")
            }

            HorizontalDivider()
            Text("عددهای پیشنهادی (قابل ویرایش)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val finalValues = mutableMapOf<QazaPrayer, Int>()
            estimate.forEach { (prayer, suggested) ->
                var override by rememberSaveable(prayer.id, signature) { mutableStateOf<String?>(null) }
                val shown = override ?: suggested.toString()
                finalValues[prayer] = parseCount(shown) ?: suggested
                OutlinedTextField(
                    value = shown,
                    onValueChange = { override = it.take(6) },
                    label = { Text("نماز ${prayer.title}") },
                    singleLine = true,
                    isError = parseCount(shown) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (replacesExisting) {
                Text(
                    "ذخیره، شمارنده‌های فعلی را جایگزین می‌کند؛ با «واگرد آخرین عمل» می‌توانید برگردید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { onConfirm(finalValues.toMap()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) { Text("ذخیره") }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) { Text("انصراف") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------------------------
// Fasts
// ---------------------------------------------------------------------------------------------

@Composable
private fun FastsTab(
    state: QazaState,
    viewModel: AdhkarViewModel,
    fontScale: Float,
    innerPadding: PaddingValues
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            QazaCard {
                Text(
                    "${state.fastsRemaining.toPersianDigits()} روزه باقی‌مانده",
                    color = NightBlue,
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { if (state.fastsOwed == 0) 0f else state.fastsMadeUp / state.fastsOwed.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    color = SunGold,
                    trackColor = SoftBorder
                )
                Text(
                    "از ${state.fastsOwed.toPersianDigits()} روزه، ${state.fastsMadeUp.toPersianDigits()} ادا شده",
                    color = NightBlue.copy(alpha = 0.7f),
                    fontSize = (13 * fontScale).sp
                )
            }
        }
        item(key = "add") {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن روزه‌ی قضا")
            }
        }
        if (state.fasts.isEmpty()) {
            item(key = "none") {
                Text(
                    "هنوز روزه‌ای ثبت نشده است. هر بار که چند روزه را نگرفته‌اید، همین‌جا با یک عنوان (مثلاً «رمضان ۱۴۰۴») اضافه کنید.",
                    color = NightBlue.copy(alpha = 0.7f),
                    fontSize = (13 * fontScale).sp,
                    lineHeight = (21 * fontScale).sp
                )
            }
        }
        items(state.fasts, key = { it.id }) { entry ->
            FastCard(
                entry = entry,
                fontScale = fontScale,
                onMarkDay = { viewModel.qazaMarkFastDay(entry.id) },
                onUnmarkDay = { viewModel.qazaUnmarkFastDay(entry.id) },
                onEdit = { editId = entry.id },
                onDelete = { deleteId = entry.id }
            )
        }
    }

    if (showAdd) {
        FastDialog(
            title = "افزودن روزه‌ی قضا",
            initial = null,
            onDismiss = { showAdd = false },
            onConfirm = { count, label, reason ->
                viewModel.qazaAddFast(count, label, reason)
                showAdd = false
            }
        )
    }
    editId?.let { id ->
        state.fasts.firstOrNull { it.id == id }?.let { entry ->
            FastDialog(
                title = "ویرایش",
                initial = entry,
                onDismiss = { editId = null },
                onConfirm = { count, label, reason ->
                    viewModel.qazaEditFast(id, count, label, reason)
                    editId = null
                }
            )
        }
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("حذف این مورد؟") },
            text = { Text("این مورد و تاریخ‌های ادای آن از دستگاه پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.qazaDeleteFast(id)
                    deleteId = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun FastCard(
    entry: FastEntry,
    fontScale: Float,
    onMarkDay: () -> Unit,
    onUnmarkDay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    QazaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.label.ifBlank { "بدون عنوان" },
                    color = NightBlue,
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                if (entry.reason.isNotBlank()) {
                    Text(entry.reason, color = NightBlue.copy(alpha = 0.65f), fontSize = (12 * fontScale).sp)
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "ویرایش")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
        LinearProgressIndicator(
            progress = { entry.madeUpDates.size.coerceAtMost(entry.count) / entry.count.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = SunGold,
            trackColor = SoftBorder
        )
        Text(
            "${entry.remaining.toPersianDigits()} باقی‌مانده از ${entry.count.toPersianDigits()}",
            color = NightBlue,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.Medium
        )
        entry.madeUpDates.lastOrNull()?.let {
            Text(
                "آخرین ادا: ${formatPersianDate(it)}",
                color = NightBlue.copy(alpha = 0.6f),
                fontSize = (12 * fontScale).sp
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onMarkDay,
                enabled = entry.remaining > 0,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("یک روز ادا شد")
            }
            OutlinedButton(
                onClick = onUnmarkDay,
                enabled = entry.madeUpDates.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("واگرد") }
        }
    }
}

@Composable
private fun FastDialog(
    title: String,
    initial: FastEntry?,
    onDismiss: () -> Unit,
    onConfirm: (count: Int, label: String, reason: String) -> Unit
) {
    var count by rememberSaveable { mutableStateOf(initial?.count?.toString() ?: "") }
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var reason by rememberSaveable { mutableStateOf(initial?.reason ?: "") }
    val parsed = parseCount(count)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.take(5) },
                    label = { Text("تعداد روزه‌های فوت‌شده") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    label = { Text("عنوان (مثلاً رمضان ۱۴۰۴)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(80) },
                    label = { Text("دلیل (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it, label, reason) } },
                enabled = parsed != null
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

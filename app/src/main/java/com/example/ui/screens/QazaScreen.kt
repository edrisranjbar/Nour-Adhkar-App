package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qaza.FastingState
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.theme.CardBackground
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.util.formatPersianDate
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

/** Missed fasts (قضای روزه). The user enters the number of days; the app makes no rulings. */
@Composable
fun QazaScreen(viewModel: AdhkarViewModel, innerPadding: PaddingValues) {
    val state by viewModel.qaza.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showEdit by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!state.hasData) {
            item(key = "empty") { EmptyState(fontScale, onAdd = { showAdd = true }) }
        } else {
            item(key = "summary") { SummaryCard(state, fontScale, onEdit = { showEdit = true }) }
            item(key = "made_up") {
                Button(
                    onClick = { viewModel.qazaMarkMadeUp() },
                    enabled = state.remaining > 0,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("یک روز ادا شد", fontSize = (16 * fontScale).sp, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showAdd = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("افزودن روزه")
                    }
                    OutlinedButton(
                        onClick = { viewModel.qazaUndoMadeUp() },
                        enabled = state.madeUp > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text("واگرد آخرین روز") }
                }
            }
            if (state.recentDates.isNotEmpty()) {
                item(key = "recent") {
                    QazaCard {
                        Text(
                            "آخرین روزهای ادا‌شده",
                            color = SunGold,
                            fontSize = (15 * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        state.recentDates.takeLast(5).asReversed().forEach { date ->
                            Row(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SunGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    formatPersianDate(date),
                                    color = NightBlue.copy(alpha = 0.8f),
                                    fontSize = (14 * fontScale).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CountDialog(
            title = "افزودن روزه‌ی فوت‌شده",
            initial = "",
            allowZero = false,
            onDismiss = { showAdd = false },
            onConfirm = { count ->
                viewModel.qazaAddMissed(count)
                showAdd = false
            }
        )
    }
    if (showEdit) {
        CountDialog(
            title = "ویرایش تعداد باقی‌مانده",
            initial = state.remaining.toString(),
            allowZero = true,
            onDismiss = { showEdit = false },
            onConfirm = { count ->
                viewModel.qazaSetRemaining(count)
                showEdit = false
            }
        )
    }
}

@Composable
private fun EmptyState(fontScale: Float, onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = SunGold.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = SunGold,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "قضای روزه‌های فوت‌شده را اینجا بشمارید و یکی‌یکی ادا کنید. عددها فقط روی همین دستگاه می‌مانند و برنامه حکم شرعی صادر نمی‌کند.",
                color = NightBlue,
                textAlign = TextAlign.Center,
                fontSize = (15 * fontScale).sp,
                lineHeight = (24 * fontScale).sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("افزودن روزه‌ی فوت‌شده")
            }
        }
    }
}

@Composable
private fun SummaryCard(state: FastingState, fontScale: Float, onEdit: () -> Unit) {
    val target = if (state.owed == 0) 0f else state.madeUp / state.owed.toFloat()
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "fastingProgress"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SunGold.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, SunGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressRing(progress = progress, strokeWidth = 14.dp, modifier = Modifier.size(172.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.remaining == 0) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SunGold, modifier = Modifier.size(40.dp))
                        Text(
                            "همه‌ی روزه‌ها ادا شد",
                            color = NightBlue,
                            textAlign = TextAlign.Center,
                            fontSize = (14 * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            state.remaining.toPersianDigits(),
                            color = NightBlue,
                            fontSize = (40 * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "روزه باقی‌مانده",
                            color = NightBlue.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontSize = (13 * fontScale).sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("ادا شده: ${state.madeUp.toPersianDigits()}", fontScale)
                StatChip("مجموع: ${state.owed.toPersianDigits()}", fontScale)
            }
            TextButton(
                onClick = onEdit,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SunGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("ویرایش تعداد باقی‌مانده", color = SunGold)
            }
        }
    }
}

@Composable
private fun StatChip(text: String, fontScale: Float) {
    Surface(shape = RoundedCornerShape(50), color = SunGold.copy(alpha = 0.14f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = NightBlue,
            fontSize = (13 * fontScale).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QazaCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val track = SoftBorder
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
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

/** Asks for one number: how many days. */
@Composable
private fun CountDialog(
    title: String,
    initial: String,
    allowZero: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val value = parseCount(text)?.takeIf { allowZero || it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(5) },
                label = { Text("تعداد روزها") },
                singleLine = true,
                isError = text.isNotEmpty() && value == null,
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

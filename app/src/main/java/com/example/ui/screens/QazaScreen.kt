package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.hasData) {
            item(key = "empty") {
                QazaCard {
                    Text(
                        "قضای روزه‌های فوت‌شده را اینجا بشمارید و یکی‌یکی ادا کنید. عددها فقط روی همین دستگاه می‌مانند و برنامه حکم شرعی صادر نمی‌کند.",
                        color = NightBlue,
                        fontSize = (15 * fontScale).sp,
                        lineHeight = (24 * fontScale).sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showAdd = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("افزودن روزه‌ی فوت‌شده")
                    }
                }
            }
        } else {
            item(key = "summary") {
                val progress = state.madeUp / state.owed.toFloat()
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
                                if (state.remaining == 0) "همه‌ی روزه‌ها ادا شد"
                                else "${state.remaining.toPersianDigits()} روزه باقی‌مانده",
                                color = NightBlue,
                                fontSize = (18 * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "از ${state.owed.toPersianDigits()} روزه، ${state.madeUp.toPersianDigits()} ادا شده",
                                color = NightBlue.copy(alpha = 0.7f),
                                fontSize = (13 * fontScale).sp
                            )
                        }
                    }
                    TextButton(
                        onClick = { showEdit = true },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = SunGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ویرایش تعداد باقی‌مانده", color = SunGold)
                    }
                }
            }
            item(key = "made_up") {
                Button(
                    onClick = { viewModel.qazaMarkMadeUp() },
                    enabled = state.remaining > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("یک روز ادا شد")
                }
            }
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAdd = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text("افزودن روزه‌ی فوت‌شده") }
                    OutlinedButton(
                        onClick = { viewModel.qazaUndoMadeUp() },
                        enabled = state.madeUp > 0,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) { Text("واگرد آخرین روز") }
                }
            }
            if (state.recentDates.isNotEmpty()) {
                item(key = "recent_title") {
                    Text(
                        "آخرین روزهای ادا‌شده",
                        color = SunGold,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.recentDates.takeLast(5).asReversed()) { date ->
                    Text(
                        formatPersianDate(date),
                        color = NightBlue.copy(alpha = 0.75f),
                        fontSize = (14 * fontScale).sp
                    )
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
private fun QazaCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.toPersianDigits
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/*
 * Small hand-drawn charts for the statistics page. They read right to left like the rest of the
 * app by default. The daily chart opts into a left-to-right time axis. Values are oldest first.
 * Drawing on a Canvas keeps them dependency-free, fast with hundreds of points, and in the theme.
 */

private val AxisLabelHeight = 18.dp

/** Entry animation progress 0→1, or 1 at once when the system "remove animations" setting is on. */
@Composable
private fun rememberEntryProgress(key: Any?): Animatable<Float, AnimationVector1D> {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val progress = remember(key) { Animatable(if (animationsOff) 1f else 0f) }
    LaunchedEffect(key) {
        if (!animationsOff) progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    return progress
}

/** A "nice" axis maximum (1, 2, 2.5 or 5 × 10ⁿ) at or above [value], so gridlines land on round numbers. */
internal fun niceMax(value: Float): Float {
    if (value <= 0f) return 1f
    val magnitude = 10f.pow(kotlin.math.floor(log10(value)))
    val step = listOf(1f, 2f, 2.5f, 5f, 10f).first { it * magnitude >= value }
    return step * magnitude
}

private fun compact(value: Float): String {
    val v = value.toInt()
    return when {
        v >= 1_000_000 -> "${(v / 100_000) / 10.0}M"
        v >= 10_000 -> "${v / 1000}K"
        v >= 1_000 -> "${(v / 100) / 10.0}K"
        else -> v.toString()
    }.replace(".0", "").toPersianDigits()
}

/** Three dashed gridlines with their values at the start (right) edge. */
private fun DrawScope.grid(maxValue: Float, chartHeight: Float, color: Color, labelColor: Color, measurer: TextMeasurer) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
    for (step in 1..3) {
        val y = chartHeight - chartHeight * step / 3f
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        val label = measurer.measure(compact(maxValue * step / 3f), TextStyle(fontSize = 10.sp, color = labelColor))
        drawText(label, topLeft = Offset(size.width - label.size.width.toFloat(), y - label.size.height - 2f))
    }
    drawLine(color, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())
}

/**
 * Vertical bars, one per value. [selected] is highlighted and reported back through [onSelect] when
 * a bar is tapped; [marks] adds a small dot above a bar (e.g. checklist items that day).
 */
@Composable
fun StatsBarChart(
    values: List<Int>,
    labels: List<String?>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    marks: List<Boolean>? = null,
    barColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedColor: Color = MaterialTheme.colorScheme.tertiary,
    markColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    oldestOnLeft: Boolean = false
) {
    val measurer = rememberTextMeasurer()
    val progress = rememberEntryProgress(values)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelPx = with(LocalDensity.current) { AxisLabelHeight.toPx() }
    val maxValue = niceMax((values.maxOrNull() ?: 0).toFloat())

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(values.size, oldestOnLeft) {
                detectTapGestures { offset ->
                    if (values.isEmpty()) return@detectTapGestures
                    val slot = size.width / values.size.toFloat()
                    val slotIndex = (offset.x / slot).toInt().coerceIn(0, values.size - 1)
                    val index = if (oldestOnLeft) slotIndex else values.size - 1 - slotIndex
                    onSelect(index.coerceIn(0, values.size - 1))
                }
            }
    ) {
        if (values.isEmpty()) return@Canvas
        val chartHeight = size.height - labelPx
        grid(maxValue, chartHeight, gridColor, labelColor, measurer)
        val slot = size.width / values.size
        val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
        values.forEachIndexed { index, value ->
            val centerX = if (oldestOnLeft) (index + 0.5f) * slot else size.width - (index + 0.5f) * slot
            val barHeight = (value / maxValue) * chartHeight * progress.value
            val color = if (index == selected) selectedColor else barColor
            if (value > 0) {
                val top = chartHeight - barHeight.coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerX - barWidth / 2, top),
                    size = Size(barWidth, chartHeight - top),
                    cornerRadius = CornerRadius(barWidth / 2.6f)
                )
            } else {
                // A short stub keeps empty days visible as days, not gaps.
                drawLine(gridColor, Offset(centerX - barWidth / 3, chartHeight - 1.5f), Offset(centerX + barWidth / 3, chartHeight - 1.5f),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            if (marks?.getOrNull(index) == true) {
                val top = chartHeight - barHeight - 7.dp.toPx()
                drawCircle(markColor, radius = 2.5.dp.toPx(), center = Offset(centerX, top.coerceAtLeast(4.dp.toPx())))
            }
            labels.getOrNull(index)?.let { text ->
                val label = measurer.measure(text, TextStyle(fontSize = 10.sp, color = if (index == selected) selectedColor else labelColor))
                drawText(label, topLeft = Offset(centerX - label.size.width / 2f, chartHeight + 3.dp.toPx()))
            }
        }
    }
}

/** A smooth filled line for a running total; the newest point gets a dot. */
@Composable
fun StatsAreaChart(
    values: List<Int>,
    startLabel: String,
    endLabel: String,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val measurer = rememberTextMeasurer()
    val progress = rememberEntryProgress(values)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelPx = with(LocalDensity.current) { AxisLabelHeight.toPx() }
    val maxValue = niceMax((values.maxOrNull() ?: 0).toFloat())

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        if (values.isEmpty()) return@Canvas
        val chartHeight = size.height - labelPx
        grid(maxValue, chartHeight, gridColor, labelColor, measurer)
        // Keep the curve clear of the value labels on the right.
        val right = size.width - 4.dp.toPx()
        val left = 4.dp.toPx()
        val span = max(values.size - 1, 1)
        fun x(i: Int) = right - (right - left) * i / span
        fun y(v: Int) = chartHeight - (v / maxValue) * chartHeight * progress.value

        val line = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) line.moveTo(x(i), y(v)) else {
                // Horizontal-tangent cubic segments: smooth, and never overshoot a running total.
                val midX = (x(i - 1) + x(i)) / 2
                line.cubicTo(midX, y(values[i - 1]), midX, y(v), x(i), y(v))
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(values.size - 1), chartHeight)
            lineTo(x(0), chartHeight)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)), endY = chartHeight))
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = values.size - 1
        drawCircle(lineColor.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = Offset(x(last), y(values[last])))
        drawCircle(lineColor, radius = 3.5.dp.toPx(), center = Offset(x(last), y(values[last])))

        val start = measurer.measure(startLabel, TextStyle(fontSize = 10.sp, color = labelColor))
        drawText(start, topLeft = Offset(right - start.size.width, chartHeight + 3.dp.toPx()))
        val end = measurer.measure(endLabel, TextStyle(fontSize = 10.sp, color = labelColor))
        drawText(end, topLeft = Offset(left, chartHeight + 3.dp.toPx()))
    }
}

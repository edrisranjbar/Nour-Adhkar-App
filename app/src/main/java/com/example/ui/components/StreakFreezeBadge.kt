package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Faceted ice, rather than a font glyph; coordinates match the Rive week-strip artwork. */
@Composable
fun StreakFreezeBadge(pending: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics {
        contentDescription = if (pending) "روز در انتظار سپر هفتگی" else "روز حفظ‌شده با سپر هفتگی"
    }) {
        val unitScale = size.minDimension / 38f
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            scale(unitScale, unitScale, pivot = Offset.Zero)
        }) {
            fun polygon(points: List<Offset>) = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            val ice = polygon(listOf(
                Offset(-10f, -16f), Offset(4f, -18f), Offset(15f, -11f),
                Offset(18f, 2f), Offset(12f, 13f), Offset(6f, 12f),
                Offset(1f, 18f), Offset(-5f, 13f), Offset(-12f, 15f),
                Offset(-17f, 4f), Offset(-17f, -7f)
            ))
            drawPath(ice, if (pending) Color(0xFF24536B) else Color(0xFF32AFE5))
            drawPath(polygon(listOf(Offset(-10f, -16f), Offset(4f, -18f), Offset(15f, -11f),
                Offset(7f, -9f), Offset(-6f, -11f), Offset(-13f, 1f), Offset(-17f, 4f),
                Offset(-17f, -7f))), Color(0xFFB5EFFF).copy(alpha = if (pending) 0.35f else 0.85f))
            drawPath(polygon(listOf(Offset(18f, 2f), Offset(12f, 13f), Offset(6f, 12f),
                Offset(1f, 18f), Offset(-5f, 13f), Offset(-12f, 15f), Offset(-17f, 4f),
                Offset(-9f, 7f), Offset(-3f, 10f), Offset(8f, 8f))), Color(0xFF087CB8).copy(alpha = 0.65f))
            drawPath(ice, Color(0xFF9CE5FA), style = Stroke(1.4f))
            drawCircle(Color(0xFF137FAF).copy(alpha = if (pending) 0.2f else 0.55f), 10.5f)
            drawCircle(Color(0xFFD3F6FF).copy(alpha = 0.55f), 10.5f, style = Stroke(1f))
            if (!pending) {
                drawPath(Path().apply {
                    moveTo(-6f, 0.5f); lineTo(-1.8f, 5f); lineTo(6.5f, -4.5f)
                }, Color.White, style = Stroke(3.4f, cap = StrokeCap.Round))
            }
        }
    }
}

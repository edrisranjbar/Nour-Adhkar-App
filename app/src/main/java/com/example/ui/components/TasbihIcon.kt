package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TasbihIcon(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val beadRadius = 2.0f.dp.toPx()
        val centerOffset = Offset(size.width / 2, size.height / 2 - 2.5f.dp.toPx())
        val loopRadius = size.width / 3.4f

        repeat(10) { index ->
            val angle = (2 * Math.PI * index / 10) - Math.PI / 2
            val beadCenter = Offset(
                (centerOffset.x + loopRadius * Math.cos(angle)).toFloat(),
                (centerOffset.y + loopRadius * Math.sin(angle)).toFloat()
            )
            drawCircle(color = color, radius = beadRadius, center = beadCenter)
        }

        val imamahCenter = Offset(centerOffset.x, centerOffset.y + loopRadius)
        drawCircle(color = color, radius = 3.2f.dp.toPx(), center = imamahCenter)

        val tasselStart = Offset(centerOffset.x, imamahCenter.y + 3.2f.dp.toPx())
        val tasselEnd = Offset(centerOffset.x, imamahCenter.y + 8.5f.dp.toPx())
        drawLine(
            color = color.copy(alpha = 0.85f),
            start = tasselStart,
            end = tasselEnd,
            strokeWidth = 1.6f.dp.toPx()
        )
        drawCircle(color = color, radius = 1.5f.dp.toPx(), center = tasselEnd)
    }
}

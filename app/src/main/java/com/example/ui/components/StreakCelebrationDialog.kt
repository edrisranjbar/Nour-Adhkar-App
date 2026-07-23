package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.DayActivity
import com.example.ui.screens.getPersianDayAbbreviation
import com.example.ui.theme.SunGold
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SparkData(
    val xRatio: Float,
    val initialY: Float,
    val speed: Float,
    val sizePx: Float,
    val swayFreq: Float,
    val swayAmp: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun StreakCelebrationDialog(
    streakCount: Int,
    pastDays: List<DayActivity>? = null,
    fontScale: Float = 1.0f,
    onDismiss: () -> Unit
) {
    val cardScale = remember { Animatable(0.75f) }
    val numberScale = remember { Animatable(0.3f) }

    // Fast, snappy pop animation on load
    LaunchedEffect(Unit) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        numberScale.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        numberScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    // Build default 7 days if not provided
    val resolvedDays = remember(pastDays) {
        if (!pastDays.isNullOrEmpty()) {
            pastDays
        } else {
            val list = mutableListOf<DayActivity>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                list.add(
                    DayActivity(
                        dayLabel = getPersianDayAbbreviation(dayOfWeek),
                        isActive = i == 0 || (streakCount > 1 && i < streakCount),
                        isToday = i == 0,
                        dateMillis = cal.timeInMillis
                    )
                )
            }
            list
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                // Background Ambient Glowing Sparks
                BackgroundEmbersCanvas()

                // Main Dialog Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .scale(cardScale.value)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1E26),
                                    Color(0xFF111116)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SunGold.copy(alpha = 0.7f),
                                    Color(0xFFFF9800).copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = false) {}
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top Bar Close Button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = Color.White
                                )
                            }
                        }

                        // Realistic Animated Flame Canvas with Live Sparks
                        AnimatedRealisticFireWithSparks(modifier = Modifier.size(170.dp))

                        Spacer(modifier = Modifier.height(4.dp))

                        // Snappy Big Streak Number
                        Text(
                            text = "$streakCount",
                            fontSize = (54 * fontScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFB74D),
                            modifier = Modifier.scale(numberScale.value)
                        )

                        Text(
                            text = "روز متوالی!",
                            fontSize = (21 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "تبارک‌الله! با استمرار در اذکار روزانه، آتش عبادت خود را فروزان نگه داشته‌اید.",
                            fontSize = (13 * fontScale).sp,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 7-Day Accurate Calendar Section
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                        ) {
                            resolvedDays.forEach { day ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day.dayLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (day.isToday) SunGold else Color.White.copy(alpha = 0.65f)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    day.isActive && day.isToday -> Color(0xFFFF9800)
                                                    day.isActive -> Color(0xFF2E7D32)
                                                    day.isToday -> Color(0xFF37474F)
                                                    else -> Color.White.copy(alpha = 0.12f)
                                                }
                                            )
                                            .border(
                                                width = if (day.isToday) 1.5.dp else 0.dp,
                                                color = if (day.isToday) SunGold else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (day.isActive) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.White.copy(alpha = 0.35f), CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (day.isToday) "امروز" else "",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SunGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Continue Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "ربّنا تَقَبَّلْ مِنَّا (ادامه)",
                                fontSize = (15.5 * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Realistic Fire Canvas with dynamic wiggling flame paths and rising glowing sparks
 */
@Composable
private fun AnimatedRealisticFireWithSparks(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "realisticFire")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Pre-generate random sparks
    val sparks = remember {
        val list = mutableListOf<SparkData>()
        val rnd = Random(1337)
        for (i in 0..45) {
            val colors = listOf(
                Color(0xFFFFD54F), // Gold
                Color(0xFFFF9800), // Vibrant Orange
                Color(0xFFFF3D00), // Fiery Red
                Color(0xFFFFFFFF), // White hot spark
                Color(0xFFFFE082)  // Soft yellow
            )
            list.add(
                SparkData(
                    xRatio = 0.25f + rnd.nextFloat() * 0.5f,
                    initialY = rnd.nextFloat(),
                    speed = 0.15f + rnd.nextFloat() * 0.25f,
                    sizePx = (2f + rnd.nextFloat() * 4f),
                    swayFreq = 3f + rnd.nextFloat() * 5f,
                    swayAmp = 0.03f + rnd.nextFloat() * 0.05f,
                    phase = rnd.nextFloat() * 6.28f,
                    color = colors[rnd.nextInt(colors.size)]
                )
            )
        }
        list
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Radial Glowing Heat Aura Behind Flame
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF9800).copy(alpha = 0.45f),
                    Color(0xFFFF3D00).copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.65f),
                radius = w * 0.52f * pulseGlow
            ),
            radius = w * 0.52f * pulseGlow,
            center = Offset(w * 0.5f, h * 0.65f)
        )

        // Wave time parameters
        val t = time * 2.5f

        // 2. Outer Deep Red/Orange Flame Layer
        val outerPath = Path().apply {
            val startX = w * 0.22f
            val startY = h * 0.88f
            moveTo(startX, startY)

            // Left flame curve dancing up
            val ctrl1X = w * 0.15f + sin(t * 1.8f) * w * 0.06f
            val ctrl1Y = h * 0.50f + cos(t * 1.5f) * h * 0.05f
            val tipX = w * 0.48f + sin(t * 2.2f) * w * 0.08f
            val tipY = h * 0.15f + cos(t * 2.8f) * h * 0.04f
            quadraticTo(ctrl1X, ctrl1Y, tipX, tipY)

            // Right flame curve dancing down
            val ctrl2X = w * 0.82f + sin(t * 1.4f + 1f) * w * 0.06f
            val ctrl2Y = h * 0.52f + cos(t * 1.9f) * h * 0.05f
            val endX = w * 0.78f
            val endY = h * 0.88f
            quadraticTo(ctrl2X, ctrl2Y, endX, endY)

            // Bottom base curve
            quadraticTo(w * 0.5f, h * 0.94f, startX, startY)
            close()
        }

        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF3D00),
                    Color(0xFFFF9100),
                    Color(0xFFFF3D00).copy(alpha = 0.8f)
                )
            )
        )

        // 3. Middle Vibrant Gold Flame Layer
        val midPath = Path().apply {
            val startX = w * 0.28f
            val startY = h * 0.86f
            moveTo(startX, startY)

            val ctrl1X = w * 0.22f + cos(t * 2.1f) * w * 0.05f
            val ctrl1Y = h * 0.55f + sin(t * 1.9f) * h * 0.04f
            val tipX = w * 0.50f + sin(t * 3.1f) * w * 0.06f
            val tipY = h * 0.28f + sin(t * 2.4f) * h * 0.03f
            quadraticTo(ctrl1X, ctrl1Y, tipX, tipY)

            val ctrl2X = w * 0.74f + sin(t * 2.0f + 2f) * w * 0.05f
            val ctrl2Y = h * 0.58f + cos(t * 2.3f) * h * 0.04f
            val endX = w * 0.72f
            val endY = h * 0.86f
            quadraticTo(ctrl2X, ctrl2Y, endX, endY)

            quadraticTo(w * 0.5f, h * 0.91f, startX, startY)
            close()
        }

        drawPath(
            path = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFD54F),
                    Color(0xFFFF9800),
                    Color(0xFFFF6D00)
                )
            )
        )

        // 4. Hot Inner Flame Core (Yellow/White)
        val corePath = Path().apply {
            val startX = w * 0.36f
            val startY = h * 0.84f
            moveTo(startX, startY)

            val ctrl1X = w * 0.32f + sin(t * 3.5f) * w * 0.03f
            val ctrl1Y = h * 0.65f
            val tipX = w * 0.50f + cos(t * 3.8f) * w * 0.04f
            val tipY = h * 0.42f + sin(t * 3.2f) * h * 0.03f
            quadraticTo(ctrl1X, ctrl1Y, tipX, tipY)

            val ctrl2X = w * 0.66f + cos(t * 3.1f) * w * 0.03f
            val ctrl2Y = h * 0.65f
            val endX = w * 0.64f
            val endY = h * 0.84f
            quadraticTo(ctrl2X, ctrl2Y, endX, endY)

            quadraticTo(w * 0.5f, h * 0.88f, startX, startY)
            close()
        }

        drawPath(
            path = corePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFFF176),
                    Color(0xFFFFB74D)
                )
            )
        )

        // 5. Rising Spark Particles (Real Glowing Embers)
        sparks.forEach { spark ->
            // Particle vertical motion
            val elapsed = time * spark.speed
            val currentYRatio = (spark.initialY - elapsed) % 1.0f
            val yNorm = if (currentYRatio < 0) currentYRatio + 1.0f else currentYRatio

            val py = yNorm * h * 0.85f
            val sway = sin(time * spark.swayFreq + spark.phase) * spark.swayAmp * w
            val px = spark.xRatio * w + sway

            // Fade out near top and bottom
            val alpha = sin(yNorm * Math.PI.toFloat()).coerceIn(0f, 1f)

            if (alpha > 0.05f) {
                // Spark Outer Glow
                drawCircle(
                    color = spark.color.copy(alpha = alpha * 0.4f),
                    radius = spark.sizePx * 1.8f,
                    center = Offset(px, py)
                )
                // Spark Core
                drawCircle(
                    color = spark.color.copy(alpha = alpha),
                    radius = spark.sizePx,
                    center = Offset(px, py)
                )
            }
        }
    }
}

/**
 * Background ambient floating embers in full-screen backdrop
 */
@Composable
private fun BackgroundEmbersCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "embers")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (i in 0..30) {
            val speed = 0.08f + (i % 5) * 0.03f
            val initialY = (i * 0.033f)
            val currentY = (initialY - time * speed % 1.0f + 1.0f) % 1.0f
            val py = currentY * height

            val sway = sin(time * 2f + i) * 30.dp.toPx()
            val px = (width * 0.1f + (i * 37f) % (width * 0.8f)) + sway

            val alpha = (1f - currentY) * 0.6f

            drawCircle(
                color = if (i % 2 == 0) Color(0xFFFF9800) else Color(0xFFFFD54F),
                radius = (2 + (i % 3) * 2).dp.toPx(),
                center = Offset(px, py),
                alpha = alpha.coerceIn(0f, 1f)
            )
        }
    }
}

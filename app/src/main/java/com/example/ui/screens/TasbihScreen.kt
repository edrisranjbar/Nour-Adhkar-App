package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TasbihSessionEntity
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasbihScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val count by viewModel.tasbihCount.collectAsState()
    val selectedDhikr by viewModel.selectedTasbihDhikr.collectAsState()
    val recentSessions by viewModel.recentTasbihSessions.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val options = listOf(
        "سبحان الله",
        "الحمد لله",
        "لا إله إلا الله",
        "الله أكبر",
        "أستغفر الله",
        "اللهم صل على محمد"
    )

    // Bead Press Scale effect
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(100),
        label = "beadScale"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Screen Header
            Text(
                text = "تسبیح شمار هوشمند",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (24 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = SandDark
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {
                // 1. Selector of common dhikrs
                item {
                    Column {
                        Text(
                            text = "انتخاب ذکر:",
                            fontSize = (13 * fontScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SandDark,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(options) { phrase ->
                                val isSelected = phrase == selectedDhikr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFFE8F0E1) else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SunGold else SoftBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.selectTasbihDhikr(phrase) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = phrase,
                                        color = if (isSelected) SunGold else SandDark,
                                        fontSize = (12 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Main Large Bead Counter Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, SoftBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedDhikr,
                                fontSize = (22 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = TextArabic,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Interactive Bead
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(172.dp)
                                    .scale(pressScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFE8F0E1),
                                                SoftBorder
                                            )
                                        )
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                tryAwaitRelease()
                                                isPressed = false
                                            },
                                            onTap = {
                                                viewModel.incrementTasbih()
                                            }
                                        )
                                    }
                            ) {
                                // Outer Ring
                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(2.dp, SunGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SandDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Reset button
                                OutlinedButton(
                                    onClick = { viewModel.resetTasbih() },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, SoftBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SandDark
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "بازنشانی",
                                        modifier = Modifier.size(16.dp),
                                        tint = SandDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بازنشانی", fontSize = (12 * fontScale).sp)
                                }

                                // Save button
                                Button(
                                    onClick = { viewModel.saveTasbihSession() },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SunGold,
                                        contentColor = Color.White,
                                        disabledContainerColor = SoftBorder,
                                        disabledContentColor = NightBlue
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = count > 0
                                ) {
                                    Text(
                                        text = "ثبت در تاریخچه",
                                        fontSize = (12 * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. History Panel Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "│ تاریخچه ذکرهای ثبت‌شده",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = (15 * fontScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SandDark
                            )
                        )

                        if (recentSessions.isNotEmpty()) {
                            Text(
                                text = "پاک کردن تاریخچه",
                                fontSize = 11.sp,
                                color = NightBlue,
                                modifier = Modifier
                                    .clickable { viewModel.clearAllUserData() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // 4. History List of Sessions
                if (recentSessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, SoftBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📿", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "هنوز ذکری ثبت نشده است. پس از اتمام شمارش، دکمه «ثبت در تاریخچه» را ضربه بزنید.",
                                    fontSize = 12.sp,
                                    color = NightBlue,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(recentSessions) { session ->
                        HistoryItemCard(
                            session = session,
                            fontScale = fontScale,
                            onDelete = { viewModel.deleteTasbihSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    session: TasbihSessionEntity,
    fontScale: Float,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.count.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunGold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.dhikrName,
                        fontSize = (14 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SandDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(session.timestamp),
                        fontSize = 11.sp,
                        color = NightBlue
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = SandDark.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarData
import com.example.data.model.DhikrItem
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun DhikrCounterScreen(
    categoryId: String,
    viewModel: AdhkarViewModel
) {
    val adhkarList by viewModel.currentCategoryAdhkar.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val category = AdhkarData.categories.find { it.id == categoryId }
    val title = category?.title ?: "اذکار"

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showCongratsDialog by remember { mutableStateOf(false) }

    // Congratulations Dialog when all dhikrs are completed
    if (showCongratsDialog) {
        AlertDialog(
            onDismissRequest = {
                showCongratsDialog = false
                viewModel.selectCategory(null) // Return home
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showCongratsDialog = false
                        viewModel.selectCategory(null) // Return home
                    }
                ) {
                    Text(
                        text = "قبول حق (بازگشت)",
                        fontWeight = FontWeight.Bold,
                        color = SunGold,
                        fontSize = (14 * fontScale).sp
                    )
                }
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⭐", fontSize = 32.sp)
                }
            },
            title = {
                Text(
                    text = "قبول حق",
                    fontWeight = FontWeight.Bold,
                    fontSize = (18 * fontScale).sp,
                    color = SandDark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "تمامی اذکار این بخش را با موفقیت قرائت کردید. طاعات و عبادات شما مقبول درگاه الهی باد.",
                    fontSize = (14 * fontScale).sp,
                    lineHeight = 22.sp,
                    color = TextPersian,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.selectCategory(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "برگشت",
                                tint = SandDark
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = (18 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = SandDark
                            )
                            Text(
                                text = "جهت ثبت تکرار، روی هر کارت ضربه بزنید",
                                fontSize = 11.sp,
                                color = SandDark.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Reset Category button
                    IconButton(onClick = { viewModel.resetCategoryProgress(categoryId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بازنشانی همه",
                            tint = SandDark
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (adhkarList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "داده‌ای یافت نشد.",
                        color = SandDark
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(adhkarList) { index, item ->
                        DhikrItemCard(
                            index = index + 1,
                            item = item,
                            fontScale = fontScale,
                            onTap = {
                                val currentCount = item.currentCount
                                val targetCount = item.targetCount
                                val isCompleted = currentCount >= targetCount
                                if (!isCompleted) {
                                    viewModel.incrementDhikr(categoryId, item.id, targetCount)
                                    val willBeCompleted = (currentCount + 1) >= targetCount
                                    if (willBeCompleted) {
                                        // Find next incomplete
                                        val currentIndex = index
                                        var targetScrollIndex = -1
                                        for (i in (currentIndex + 1) until adhkarList.size) {
                                            if (adhkarList[i].currentCount < adhkarList[i].targetCount) {
                                                targetScrollIndex = i
                                                break
                                            }
                                        }
                                        if (targetScrollIndex == -1) {
                                            for (i in 0 until currentIndex) {
                                                if (adhkarList[i].currentCount < adhkarList[i].targetCount) {
                                                    targetScrollIndex = i
                                                    break
                                                }
                                            }
                                        }

                                        if (targetScrollIndex != -1) {
                                            coroutineScope.launch {
                                                delay(400) // slight delay to let user see checkmark
                                                listState.animateScrollToItem(targetScrollIndex)
                                            }
                                        } else {
                                            // No incomplete ones left! That means everything is complete!
                                            coroutineScope.launch {
                                                delay(600)
                                                showCongratsDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Bottom reset button for easy reachability
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.resetCategoryProgress(categoryId) },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, SandDark.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SandDark
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شروع مجدد این دسته از اذکار",
                                fontSize = (13 * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DhikrItemCard(
    index: Int,
    item: DhikrItem,
    fontScale: Float,
    onTap: () -> Unit
) {
    val isCompleted = item.currentCount >= item.targetCount
    val animatedCardBg by animateColorAsState(
        targetValue = if (isCompleted) Color(0xFFF2F5ED) else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCompleted) SunGold else SoftBorder,
        label = "borderBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCompleted) {
                    Modifier
                } else {
                    Modifier.clickable { onTap() }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = animatedCardBg),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Index and Target Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F0E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunGold
                    )
                }

                // Status Indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isCompleted) SunGold.copy(alpha = 0.15f) else Color(0xFFE8F0E1),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isCompleted) "کامل شد ✓" else "تعداد هدف: ${item.targetCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) SunGold else SandDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Text
            Text(
                text = item.arabicText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (21 * fontScale).sp,
                    lineHeight = (34 * fontScale).sp,
                    fontWeight = FontWeight.Medium,
                    color = TextArabic
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SoftBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Persian Translation
            Text(
                text = item.persianTranslation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (13.5 * fontScale).sp,
                    lineHeight = (22 * fontScale).sp,
                    color = TextPersian
                ),
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )

            // Source/Benefit if available
            if (item.source.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F0E1).copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                        .border(1.dp, SoftBorder, shape = RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "فضیلت: ${item.source}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (11 * fontScale).sp,
                            lineHeight = 16.sp,
                            color = TextPersian
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Bottom Panel (Counter Ring & Tap Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) "این ذکر با موفقیت تکمیل شد" else "جهت شمارش، روی کارت ضربه بزنید",
                    fontSize = 11.sp,
                    color = if (isCompleted) Color(0xFF4CAF50) else NightBlue,
                    fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal
                )

                // Interactive Circle Counter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    val progress = if (item.targetCount > 0) {
                        item.currentCount.toFloat() / item.targetCount.toFloat()
                    } else {
                        0f
                    }

                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✔",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = SunGold,
                            trackColor = SoftBorder,
                            strokeWidth = 3.5.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.currentCount.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SandDark
                            )
                            HorizontalDivider(
                                modifier = Modifier.width(18.dp),
                                color = SoftBorder,
                                thickness = 1.dp
                            )
                            Text(
                                text = item.targetCount.toString(),
                                fontSize = 10.sp,
                                color = NightBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

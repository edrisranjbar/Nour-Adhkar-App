package com.example.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserFeeling
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun EmotionalAyahCard(viewModel: AdhkarViewModel, fontScale: Float) {
    val selectedFeeling by viewModel.selectedFeeling.collectAsState()
    val ayah by viewModel.emotionalAyah.collectAsState()
    var showFeelingPicker by remember { mutableStateOf(selectedFeeling == null) }

    LaunchedEffect(selectedFeeling) {
        if (selectedFeeling == null) showFeelingPicker = true
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = SunGold, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "آیه‌ای برای حال امروزت",
                        fontSize = (16.5 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = SandDark
                    )
                    Text(
                        text = "امروز دلت چه حالی دارد؟",
                        fontSize = (11.5 * fontScale).sp,
                        color = NightBlue
                    )
                }
                selectedFeeling?.takeUnless { showFeelingPicker }?.let { feeling ->
                    Surface(
                        modifier = Modifier.clickable { showFeelingPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = SunGold.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SunGold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = feeling.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (showFeelingPicker) {
                Spacer(modifier = Modifier.height(12.dp))
                UserFeeling.entries.chunked(2).forEach { feelings ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    feelings.forEach { feeling ->
                        val isSelected = selectedFeeling == feeling
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                viewModel.selectFeeling(feeling)
                                showFeelingPicker = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) SunGold else SoftBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = feeling.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = feeling.title,
                                    fontSize = (11.5 * fontScale).sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = SandDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                    }
                }
            }
            }

            AnimatedVisibility(
                visible = ayah != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ayah?.let { emotionalAyah ->
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        HorizontalDivider(color = SoftBorder.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = emotionalAyah.text.toPersianDigits(),
                            fontFamily = AmiriQuran,
                            fontSize = (16 * fontScale).sp,
                            lineHeight = (27 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = TextArabic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (com.example.ui.language.LocalAppLanguage.current.showPersianTranslation) {
                        Text(
                            text = emotionalAyah.translation.toPersianDigits(),
                            fontSize = (12.5 * fontScale).sp,
                            lineHeight = (19 * fontScale).sp,
                            color = TextPersian,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        }
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = emotionalAyah.reference.toPersianDigits(),
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SunGold,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        if (com.example.ui.language.LocalAppLanguage.current.showPersianTranslation) {
                        Text(
                            text = emotionalAyah.translationSource,
                            fontSize = (9.5 * fontScale).sp,
                            color = TextPersian.copy(alpha = 0.72f),
                            modifier = Modifier.align(Alignment.End)
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AyahOfTheDayCard(viewModel: AdhkarViewModel, fontScale: Float) {
    val ayah = viewModel.ayahOfTheDay
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: SVG Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "آیه روز",
                        tint = SunGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "آیه روز",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (16.5 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = SandDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text - Centered
            Text(
                        text = ayah.text.toPersianDigits(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AmiriQuran,
                    fontSize = (15.5 * fontScale).sp,
                    lineHeight = (26 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = TextArabic
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Persian Translation
            if (com.example.ui.language.LocalAppLanguage.current.showPersianTranslation) {
            Text(
                        text = ayah.translation.toPersianDigits(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (12.5 * fontScale).sp,
                    lineHeight = (18.5 * fontScale).sp,
                    color = TextPersian
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Surah Reference - Bottom Left
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(0.5.dp, SoftBorder)
                ) {
                    Text(
                        text = ayah.reference.toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SunGold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

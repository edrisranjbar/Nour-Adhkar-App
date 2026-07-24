package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriQuran
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.theme.TextArabic
import com.example.ui.theme.TextPersian
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun SettingsScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val morningTime by viewModel.morningTime.collectAsState()
    val eveningTime by viewModel.eveningTime.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }

    val morningSlots = listOf("05:00", "06:00", "07:00", "08:00", "09:00")
    val eveningSlots = listOf("16:00", "17:00", "18:00", "19:00", "20:00")

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
                text = "تنظیمات برنامه",
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
                // SECTION 1: Notifications & Reminders
                item {
                    SettingsSectionHeader(title = "یادآوری‌های روزانه (اعلان‌ها)")
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, SoftBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 1. Switch Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "فعال‌سازی یادآور اذکار",
                                        fontSize = (14 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Text(
                                        text = "نمایش اعلان در ساعت‌های تعیین‌شده",
                                        fontSize = 11.sp,
                                        color = NightBlue
                                    )
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SunGold,
                                        uncheckedThumbColor = SandDark.copy(alpha = 0.4f),
                                        uncheckedTrackColor = SoftBorder
                                    )
                                )
                            }

                            if (notificationsEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = SoftBorder)
                                Spacer(modifier = Modifier.height(16.dp))

                                // 2. Morning Notification Time Slots Selector
                                Column {
                                    Text(
                                        text = "ساعت یادآوری صبحگاه:",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        morningSlots.forEach { slot ->
                                            val isSelected = slot == morningTime
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        color = if (isSelected) Color(0xFFE8F0E1) else MaterialTheme.colorScheme.surface
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SunGold else SoftBorder,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.updateMorningTime(slot) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = slot,
                                                    color = if (isSelected) SunGold else SandDark,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = SoftBorder)
                                Spacer(modifier = Modifier.height(16.dp))

                                // 3. Evening Notification Time Slots Selector
                                Column {
                                    Text(
                                        text = "ساعت یادآوری شامگاه:",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        eveningSlots.forEach { slot ->
                                            val isSelected = slot == eveningTime
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        color = if (isSelected) Color(0xFFE8F0E1) else MaterialTheme.colorScheme.surface
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SunGold else SoftBorder,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.updateEveningTime(slot) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = slot,
                                                    color = if (isSelected) SunGold else SandDark,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // 4. Test Notification Button
                                Button(
                                    onClick = { viewModel.triggerTestNotification() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SunGold,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "ارسال اعلان آزمایشی",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تست آنی یادآوری (اعلان آزمایشی)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 2: UI Preferences
                item {
                    SettingsSectionHeader(title = "بازخورد لمسی و اندازه قلم")
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, SoftBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 1. Vibration Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "لرزش هنگام لمس",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Text(
                                        text = "ویبره ملایم در موقع ثبت تکرارها",
                                        fontSize = 11.sp,
                                        color = NightBlue
                                    )
                                }
                                Switch(
                                    checked = vibrationEnabled,
                                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SunGold,
                                        uncheckedThumbColor = SandDark.copy(alpha = 0.4f),
                                        uncheckedTrackColor = SoftBorder
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = SoftBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Sound Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "پخش صدای ملایم",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Text(
                                        text = "پخش صدای بوق کوتاه همراه لرزش",
                                        fontSize = 11.sp,
                                        color = NightBlue
                                    )
                                }
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = { viewModel.setSoundEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SunGold,
                                        uncheckedThumbColor = SandDark.copy(alpha = 0.4f),
                                        uncheckedTrackColor = SoftBorder
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = SoftBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 3. Font Scale Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "اندازه قلم متون:",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SandDark
                                    )
                                    Text(
                                        text = when {
                                            fontScale < 0.9f -> "کوچک"
                                            fontScale > 1.3f -> "خیلی بزرگ"
                                            fontScale > 1.1f -> "بزرگ"
                                            else -> "استاندارد"
                                        },
                                        fontSize = 12.sp,
                                        color = SunGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = fontScale,
                                    onValueChange = { viewModel.updateFontScale(it) },
                                    valueRange = 0.8f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SunGold,
                                        activeTrackColor = SunGold,
                                        inactiveTrackColor = SoftBorder
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Preview box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F0E1).copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp))
                                        .border(1.dp, SoftBorder, shape = RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        fontFamily = AmiriQuran,
                                        fontSize = (20 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextArabic,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Storage & Resets
                item {
                    SettingsSectionHeader(title = "مدیریت داده‌ها و بازنشانی")
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, SoftBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!showClearConfirm) {
                                OutlinedButton(
                                    onClick = { showClearConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFD32F2F)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        text = "پاکسازی کامل داده‌ها و تاریخچه",
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "آیا از پاک کردن کامل تاریخچه ذکرهای تسبیح و بازنشانی تمام شمارنده‌های اذکار روزانه اطمینان دارید؟",
                                        fontSize = (12 * fontScale).sp,
                                        color = Color(0xFFD32F2F),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.clearAllUserData()
                                                showClearConfirm = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("بله، پاک شود", fontSize = 11.sp, color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = { showClearConfirm = false },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, SoftBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SandDark),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("انصراف", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = "│ $title",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SandDark
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

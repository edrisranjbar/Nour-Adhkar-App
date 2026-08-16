package com.example.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val morningTime by viewModel.morningTime.collectAsState()
    val eveningTime by viewModel.eveningTime.collectAsState()

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

                                // 2. Morning Notification Time Picker
                                NotificationTimePicker(
                                    label = "ساعت یادآوری صبحگاه:",
                                    time = morningTime,
                                    fontScale = fontScale,
                                    onTimeSelected = viewModel::updateMorningTime
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = SoftBorder)
                                Spacer(modifier = Modifier.height(16.dp))

                                // 3. Evening Notification Time Picker
                                NotificationTimePicker(
                                    label = "ساعت یادآوری شامگاه:",
                                    time = eveningTime,
                                    fontScale = fontScale,
                                    onTimeSelected = viewModel::updateEveningTime
                                )

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = SunGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("حالت تاریک", fontSize = (13 * fontScale).sp, fontWeight = FontWeight.SemiBold, color = SandDark)
                                        Text("نمایش آرام‌تر در محیط کم‌نور", fontSize = 11.sp, color = NightBlue)
                                    }
                                }
                                Switch(
                                    checked = darkModeEnabled,
                                    onCheckedChange = viewModel::setDarkModeEnabled,
                                    colors = SwitchDefaults.colors(checkedTrackColor = SunGold)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = SoftBorder)
                            Spacer(modifier = Modifier.height(14.dp))

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
                                        .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp))
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

            }
        }
    }
}

@Composable
private fun NotificationTimePicker(
    label: String,
    time: String,
    fontScale: Float,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val parts = time.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    Column {
        Text(
            text = label,
            fontSize = (13 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = SandDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimeSelected(String.format(Locale.US, "%02d:%02d", hour, minute))
                    },
                    initialHour,
                    initialMinute,
                    true
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, SoftBorder),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SandDark)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = SunGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = String.format(Locale.US, "%02d:%02d", initialHour, initialMinute),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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

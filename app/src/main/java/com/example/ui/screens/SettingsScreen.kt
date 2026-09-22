package com.example.ui.screens

import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.language.LocalizedText as Text
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

@Composable
fun SettingsScreen(
    viewModel: AdhkarViewModel,
    innerPadding: PaddingValues
) {
    val section by viewModel.settingsSection.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val morningTime by viewModel.morningTime.collectAsState()
    val eveningTime by viewModel.eveningTime.collectAsState()
    val fridayKahfReminderEnabled by viewModel.fridayKahfReminderEnabled.collectAsState()
    val fridayKahfReminderTime by viewModel.fridayKahfReminderTime.collectAsState()
    val volumeKeyCountingEnabled by viewModel.volumeKeyCountingEnabled.collectAsState()
    val volumeCountButton by viewModel.volumeCountButton.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            TabRow(modifier = Modifier.padding(bottom = 24.dp), selectedTabIndex = section, containerColor = MaterialTheme.colorScheme.background, contentColor = SunGold) {
                listOf("عمومی", "اعلان‌ها", "اوقات شرعی").forEachIndexed { index, title ->
                    Tab(selected = section == index, onClick = { viewModel.selectSettingsSection(index) }, text = { Text(title) }, selectedContentColor = SunGold, unselectedContentColor = NightBlue)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
            ) {
                if (section == 2) { item { PrayerSettingsEditor(viewModel) } }
                if (section == 1) {
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

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = SoftBorder)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("یادآوری سوره کهف در جمعه", fontSize = (13 * fontScale).sp, fontWeight = FontWeight.SemiBold, color = SandDark)
                                        Text("باز کردن مستقیم سوره برای تلاوت", fontSize = 11.sp, color = NightBlue)
                                    }
                                    Switch(
                                        checked = fridayKahfReminderEnabled,
                                        onCheckedChange = viewModel::setFridayKahfReminderEnabled,
                                        colors = SwitchDefaults.colors(checkedTrackColor = SunGold, checkedThumbColor = Color.White, uncheckedThumbColor = NightBlue, uncheckedTrackColor = SoftBorder, uncheckedBorderColor = SoftBorder)
                                    )
                                }
                                if (fridayKahfReminderEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    NotificationTimePicker(
                                        label = "ساعت یادآوری جمعه:",
                                        time = fridayKahfReminderTime,
                                        fontScale = fontScale,
                                        onTimeSelected = viewModel::updateFridayKahfReminderTime
                                    )
                                }

                            }
                        }
                    }
                }

                }
                if (section == 0) {
                item { LanguageSettings(viewModel) }
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
                                    colors = SwitchDefaults.colors(checkedTrackColor = SunGold, checkedThumbColor = Color.White, uncheckedThumbColor = NightBlue, uncheckedTrackColor = SoftBorder, uncheckedBorderColor = SoftBorder)
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

                // SECTION 3: Tasbih Volume Key Settings
                item {
                    SettingsSectionHeader(title = "تنظیمات ذکرشمار")
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
                            // 1. Warning Notice (Red)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (darkModeEnabled) Color(0xFF331616) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (darkModeEnabled) Color(0xFF6B2828) else Color(0xFFFCA5A5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (darkModeEnabled) Color(0xFFF87171) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "هشدار احتمال استهلاک کلیدها",
                                            fontSize = (12 * fontScale).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (darkModeEnabled) Color(0xFFF87171) else Color(0xFFB91C1C)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "فشردن مکرر و طولانی دکمه‌های ولوم برای ذکرشماری ممکن است به کلیدهای فیزیکی گوشی آسیب بزند.",
                                            fontSize = (11 * fontScale).sp,
                                            color = if (darkModeEnabled) Color(0xFFFCA5A5) else Color(0xFF7F1D1D),
                                            lineHeight = (16 * fontScale).sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Solution Notice (Green)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (darkModeEnabled) Color(0xFF142E1B) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, if (darkModeEnabled) Color(0xFF285C36) else Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = if (darkModeEnabled) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "راهکار پیشنهادی",
                                            fontSize = (12 * fontScale).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (darkModeEnabled) Color(0xFF4ADE80) else Color(0xFF15803D)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "می‌توانید به‌جای دکمه‌های کم و زیاد صدای خود گوشی، از هندزفری یا هدست‌های دارای دکمه تنظیم صدا استفاده کنید.",
                                            fontSize = (11 * fontScale).sp,
                                            color = if (darkModeEnabled) Color(0xFF86EFAC) else Color(0xFF14532D),
                                            lineHeight = (16 * fontScale).sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = SoftBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = SunGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "شمارش با دکمه‌های صدا",
                                            fontSize = (13 * fontScale).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SandDark
                                        )
                                        Text(
                                            text = "استفاده از کلید فیزیکی ولوم برای شمارش ذکر در بخش ذکرشمار",
                                            fontSize = 11.sp,
                                            color = NightBlue
                                        )
                                    }
                                }
                                Switch(
                                    checked = volumeKeyCountingEnabled,
                                    onCheckedChange = viewModel::setVolumeKeyCountingEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = SunGold,
                                        checkedThumbColor = Color.White,
                                        uncheckedThumbColor = NightBlue,
                                        uncheckedTrackColor = SoftBorder,
                                        uncheckedBorderColor = SoftBorder
                                    )
                                )
                            }

                            if (volumeKeyCountingEnabled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = SoftBorder)
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "تعیین دکمه برای ذکرشماری:",
                                    fontSize = (12 * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SandDark
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    com.example.data.model.VolumeCountButton.entries.forEach { option ->
                                        val isSelected = volumeCountButton == option
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { viewModel.setVolumeCountButton(option) },
                                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) SunGold else SoftBorder
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = when (option) {
                                                            com.example.data.model.VolumeCountButton.BOTH -> Icons.Default.SwapVert
                                                            com.example.data.model.VolumeCountButton.UP -> Icons.Default.VolumeUp
                                                            com.example.data.model.VolumeCountButton.DOWN -> Icons.Default.VolumeDown
                                                        },
                                                        contentDescription = null,
                                                        tint = if (isSelected) SunGold else SandDark,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = option.titlePersian,
                                                        fontSize = (12 * fontScale).sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) SunGold else SandDark
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "انتخاب شده",
                                                        tint = SunGold,
                                                        modifier = Modifier.size(18.dp)
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
        text = "│ ${LocalAppLanguage.current.text(title)}",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SandDark
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

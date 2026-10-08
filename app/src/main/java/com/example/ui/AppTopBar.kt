package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import com.example.ui.language.text
import com.example.ui.theme.NightBlue
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun AppTopBar(
    viewModel: AdhkarViewModel,
    currentTab: String,
    signedIn: Boolean,
    fontScale: Float,
    onOpenDrawer: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val context = LocalContext.current
    if (currentTab !in setOf("quran", "achievements")) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onOpenDrawer() }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "باز کردن منو",
                tint = NightBlue
            )
        }
        Text(
            text = when (currentTab) {
                "checklist" -> "چک‌لیست روزانه"
                "tasbih" -> "ذکرشمار"
                "achievements" -> "نشان‌ها و دستاوردها"
                "qibla" -> "قبله‌نما"
                "qaza" -> "قضای روزه"
                "calendar" -> "تقویم"
                "stats" -> "آمار من"
                "settings" -> "تنظیمات"
                "about" -> "درباره برنامه"
                "app_inbox" -> "پیام‌ها"
                "account" -> if (!signedIn) "ورود به حساب" else "پروفایل"
                "scholars" -> "علما و مشاهیر"
                "articles" -> "مقالات"
                "adhkar" -> "اذکار و ادعیه"
                "quran" -> "قرآن کریم"
                "quran_audio" -> "قرآن صوتی"
                "favorites" -> "علاقه‌مندی‌ها"
                else -> "اذکار نور"
            },
            fontSize = (18 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = NightBlue,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (currentTab == "home") {
            val unread by com.example.data.repository.AppInboxApi.unreadCount.collectAsState()
            LaunchedEffect(Unit) { com.example.data.repository.AppInboxApi.refreshUnreadCount(context) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                onOpenAchievements()
            }) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "نشان‌ها و دستاوردها",
                    tint = NightBlue
                )
            }
            IconButton(onClick = { viewModel.selectTab("favorites") }) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "علاقه‌مندی‌ها",
                    tint = NightBlue
                )
            }
            IconButton(onClick = { viewModel.selectTab("app_inbox") }) {
                androidx.compose.material3.BadgedBox(
                    badge = {
                        if (unread > 0) {
                            androidx.compose.material3.Badge {
                                Text(if (unread > 99) "۹۹+" else unread.toPersianDigits())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = if (unread > 0) "پیام‌ها، ${unread.toPersianDigits()} خوانده‌نشده" else "پیام‌ها",
                        tint = NightBlue
                    )
                }
            }
        }
    }
    }
}

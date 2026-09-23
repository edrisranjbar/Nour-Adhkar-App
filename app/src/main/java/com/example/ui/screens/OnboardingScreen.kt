package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.language.AppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    language: AppLanguage,
    notificationsEnabled: Boolean,
    darkModeEnabled: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = language.text("اذکار نور"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(14.dp))
            OnboardingStepper(
                currentPage = pagerState.currentPage,
                language = language
            )
            Spacer(Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> WelcomePage(language)
                    1 -> LanguagePage(language, onLanguageChange)
                    2 -> PreferencesPage(
                        language = language,
                        notificationsEnabled = notificationsEnabled,
                        darkModeEnabled = darkModeEnabled,
                        onNotificationsChange = onNotificationsChange,
                        onDarkModeChange = onDarkModeChange
                    )
                    else -> ReadyPage(
                        language = language,
                        notificationsEnabled = notificationsEnabled,
                        darkModeEnabled = darkModeEnabled
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(language.text("بازگشت"))
                    }
                }
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E6B4E),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = language.text(if (isLastPage) "شروع کنیم" else "ادامه"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepper(currentPage: Int, language: AppLanguage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(ONBOARDING_PAGE_COUNT) { index ->
                val active = index <= currentPage
                val nodeSize by animateDpAsState(
                    targetValue = if (index == currentPage) 30.dp else 24.dp,
                    label = "stepSize"
                )
                val nodeColor by animateColorAsState(
                    targetValue = if (active) Color(0xFF2E6B4E) else MaterialTheme.colorScheme.surfaceVariant,
                    label = "stepColor"
                )
                if (index > 0) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (active) Color(0xFF2E6B4E)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(nodeSize)
                        .clip(CircleShape)
                        .background(nodeColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentPage) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.FARSI) (index + 1).toPersianDigits()
                            else (index + 1).toString(),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun WelcomePage(language: AppLanguage) = PageColumn {
    CharacterHero(modifier = Modifier.height(260.dp))
    Text(
        text = language.text("همراهی روشن برای هر روز"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = language.text("ذکر، قرآن، نماز و عادت‌های معنوی را ساده و آرام در کنار هم داشته باشید."),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 380.dp)
    )
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactFeature(Icons.Rounded.MenuBook, language.text("اذکار و قرآن"), Modifier.weight(1f))
        CompactFeature(Icons.Rounded.Schedule, language.text("اوقات نماز"), Modifier.weight(1f))
        CompactFeature(Icons.Rounded.Checklist, language.text("اعمال روزانه"), Modifier.weight(1f))
    }
}

@Composable
private fun CharacterHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(218.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF2C14E).copy(alpha = 0.22f),
                            Color(0xFF2E7D58).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        Image(
            painter = painterResource(R.drawable.onboarding_nour_boy),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun CompactFeature(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = Color(0xFF2E6B4E), modifier = Modifier.size(22.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun LanguagePage(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) = PageColumn {
    OnboardingIcon(Icons.Rounded.Language)
    Spacer(Modifier.height(22.dp))
    Text(
        text = language.text("زبان دلخواهتان را انتخاب کنید"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = language.text("همه بخش‌های برنامه با زبان انتخابی شما نمایش داده می‌شوند."),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(28.dp))
    LanguageOption(
        iconText = "فا",
        iconContentDescription = "نماد زبان فارسی",
        title = "فارسی",
        subtitle = "زبان فارسی و ترجمه اذکار",
        selected = language == AppLanguage.FARSI,
        onClick = { onLanguageChange(AppLanguage.FARSI) }
    )
    Spacer(Modifier.height(12.dp))
    LanguageOption(
        iconText = "ع",
        iconContentDescription = "رمز اللغة العربية",
        title = "العربية",
        subtitle = "واجهة عربية دون ترجمة فارسية",
        selected = language == AppLanguage.ARABIC,
        onClick = { onLanguageChange(AppLanguage.ARABIC) }
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = language.text("هر زمان بخواهید می‌توانید زبان را از تنظیمات تغییر دهید."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LanguageOption(
    iconText: String,
    iconContentDescription: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF2E6B4E) else MaterialTheme.colorScheme.outlineVariant,
        label = "languageBorder"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp)),
        color = if (selected) Color(0xFF2E6B4E).copy(alpha = 0.09f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Color(0xFF2E6B4E).copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .semantics { contentDescription = iconContentDescription },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E6B4E),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
                    .background(if (selected) Color(0xFF2E6B4E) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PreferencesPage(
    language: AppLanguage,
    notificationsEnabled: Boolean,
    darkModeEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) = PageColumn {
    OnboardingIcon(Icons.Rounded.NotificationsActive)
    Spacer(Modifier.height(22.dp))
    Text(
        text = language.text("تجربه خودتان را بسازید"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = language.text("دو انتخاب مهم برای شروع؛ بعداً همه جزئیات از تنظیمات قابل تغییر است."),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(26.dp))
    PreferenceOption(
        icon = Icons.Rounded.NotificationsActive,
        title = language.text("یادآوری‌های روزانه"),
        subtitle = language.text("یادآوری آرام برای اذکار صبحگاه، شامگاه و جمعه"),
        checked = notificationsEnabled,
        onCheckedChange = onNotificationsChange
    )
    Spacer(Modifier.height(12.dp))
    PreferenceOption(
        icon = Icons.Rounded.DarkMode,
        title = language.text("حالت تاریک"),
        subtitle = language.text("نمایش آرام‌تر و مناسب نور کم"),
        checked = darkModeEnabled,
        onCheckedChange = onDarkModeChange
    )
    Spacer(Modifier.height(18.dp))
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = language.text("برای اعلان‌ها فقط هنگام پایان راه‌اندازی اجازه سیستم درخواست می‌شود."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PreferenceOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2E6B4E).copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF2E6B4E), modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
    }
}

@Composable
private fun ReadyPage(
    language: AppLanguage,
    notificationsEnabled: Boolean,
    darkModeEnabled: Boolean
) = PageColumn {
    CharacterHero(modifier = Modifier.height(210.dp))
    Text(
        text = language.text("همه‌چیز آماده است"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = language.text("با قدم‌های کوچک، یک همراهی ماندگار با ذکر و عبادت بسازید."),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(20.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReadyRow(
                language.text("زبان برنامه"),
                if (language == AppLanguage.FARSI) "فارسی" else "العربية"
            )
            ReadyRow(
                language.text("یادآوری‌های روزانه"),
                language.text(if (notificationsEnabled) "فعال" else "غیرفعال")
            )
            ReadyRow(
                language.text("نمایش برنامه"),
                language.text(if (darkModeEnabled) "تاریک" else "روشن")
            )
        }
    }
}

@Composable
private fun ReadyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OnboardingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .padding(top = 30.dp)
            .size(92.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2E6B4E), Color(0xFF4D8A68))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp))
    }
}

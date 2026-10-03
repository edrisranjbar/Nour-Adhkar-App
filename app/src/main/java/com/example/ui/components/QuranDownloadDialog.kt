package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.media.QuranReciter
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import java.util.Locale

/** Both Quran surfaces confirm the pending request, rather than the current picker selection. */
@Composable
fun QuranDownloadDialog(
    reciter: QuranReciter,
    surahNumber: Int,
    surahName: String?,
    bytes: Long,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val language = LocalAppLanguage.current
    val colors = MaterialTheme.colorScheme
    val name = if (language == AppLanguage.ARABIC) reciter.arName else reciter.faName
    val surah = surahName ?: language.text("سورهٔ ${surahNumber.toPersianDigits()}")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CloudDownload, null, tint = colors.primary, modifier = Modifier.size(32.dp)) },
            title = { Text(language.text("دریافت تلاوت"), fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(language.text("دانلود با اینترنت همراه"), style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant)
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerHigh) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Headphones, null, tint = colors.primary)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(surah, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                        color = colors.onSurface)
                                    Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                            Text(language.text("حجم دانلود"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            Text(language.text(downloadSize(bytes)), style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        }
                    }
                    Text(language.text("با دانلود، از حجم اینترنت همراه شما استفاده می‌شود."),
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.DownloadDone, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Text(language.text("تلاوت یک‌بار دریافت و برای شنیدن آفلاین ذخیره می‌شود."),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { Button(onClick = onDownload) { Text(language.text("دانلود و پخش")) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(language.text("فعلاً نه")) } }
        )
    }
}

private fun downloadSize(bytes: Long): String {
    if (bytes <= 0) return "حجم فایل اعلام نشده است"
    val (amount, unit) = when {
        bytes >= 1_073_741_824 -> bytes / 1_073_741_824.0 to "گیگابایت"
        bytes >= 1_048_576 -> bytes / 1_048_576.0 to "مگابایت"
        else -> kotlin.math.ceil(bytes / 1024.0) to "کیلوبایت"
    }
    val size = if (unit == "کیلوبایت") amount.toLong().toString() else String.format(Locale.US, "%.1f", amount)
    return "حدود ${size.replace('.', '٫').toPersianDigits()} $unit"
}

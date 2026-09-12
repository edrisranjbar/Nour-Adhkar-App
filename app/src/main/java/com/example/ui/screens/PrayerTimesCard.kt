package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.util.formatPersianDate
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun PrayerTimesCard(viewModel: AdhkarViewModel) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val accent = if (dark) colors.tertiary else Color(0xFF087F65)
    val cardSurface = if (dark) colors.surface else Color(0xFFFFFCF8)
    val foreground = if (dark) colors.onSurface else Color(0xFF19272C)
    val muted = if (dark) colors.onSurfaceVariant else Color(0xFF596568)
    val line = if (dark) colors.outlineVariant else Color(0xFFDEDCD5)
    val settings by viewModel.prayerSettings.collectAsState()
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    val zone = remember(settings.zone) { TimeZone.getTimeZone(settings.zone) }
    val day = formatPersianDate(now.time, zone)
    val formatter = remember(zone) { SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone } }
    val schedules = remember(settings, day) {
        if (!settings.isValid()) null else runCatching {
            val tomorrow = Calendar.getInstance(zone).apply { time = now; add(Calendar.DAY_OF_MONTH, 1) }.time
            settings.times(now) to settings.times(tomorrow)
        }.getOrNull()
    }
    val next = schedules?.let { (today, tomorrow) ->
        (today.map { Triple(it.first, it.second, false) } + tomorrow.map { Triple(it.first, it.second, true) })
            .firstOrNull { it.first != "طلوع" && it.second != null && it.second!!.time / 60_000 > now.time / 60_000 }
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface, contentColor = foreground),
        border = BorderStroke(1.dp, line)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("اوقات شرعی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = accent)
                    Spacer(Modifier.width(4.dp))
                    Text(if (settings.isValid()) settings.location else "موقعیت شما",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = { viewModel.openPrayerSettings() }) {
                    Icon(Icons.Default.Settings, contentDescription = "تنظیمات اوقات شرعی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!settings.isValid()) {
                Text("موقعیت خود را مشخص کنید تا زمان نمازها و زمان باقی‌مانده تا نماز بعدی را اینجا ببینید.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { viewModel.openPrayerSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = cardSurface)) {
                    Text("تعیین موقعیت")
                }
            } else if (schedules == null) {
                Text("محاسبه اوقات برای این موقعیت ممکن نیست. تنظیمات موقعیت را بررسی کنید.")
            } else {
                if (next != null) {
                    val minutes = ((next.second!!.time - now.time + 59_999) / 60_000).coerceAtLeast(1)
                    val remaining = when {
                        minutes < 60 -> "${minutes.toPersianDigits()} دقیقه"
                        minutes % 60 == 0L -> "${(minutes / 60).toPersianDigits()} ساعت"
                        else -> "${(minutes / 60).toPersianDigits()} ساعت و ${(minutes % 60).toPersianDigits()} دقیقه"
                    }
                    Surface(color = if (dark) colors.secondaryContainer else accent.copy(alpha = 0.05f), contentColor = foreground,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)), shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("نماز بعدی${if (next.third) " • فردا" else ""}",
                                    style = MaterialTheme.typography.labelLarge, color = accent)
                                Text(next.first, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                            Text(formatter.format(next.second!!).toPersianDigits(),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
                            Text("$remaining دیگر", style = MaterialTheme.typography.labelSmall, color = muted)
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, line)) {
                    Column {
                        schedules.first.chunked(2).forEachIndexed { index, entries ->
                          Row(Modifier.fillMaxWidth()) {
                           entries.forEach { (label, time) ->
                            val active = next?.first == label && next.third == false
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = if (active) { if (dark) colors.secondaryContainer else accent.copy(alpha = 0.10f) } else Color.Transparent,
                                contentColor = if (active) accent else foreground
                            ) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(when (label) {
                                        "صبح" -> Icons.Default.WbTwilight
                                        "طلوع" -> Icons.Default.WbSunny
                                        "ظهر" -> Icons.Default.LightMode
                                        "عصر" -> Icons.Default.WbCloudy
                                        "مغرب" -> Icons.Default.NightsStay
                                        else -> Icons.Default.DarkMode
                                    }, null, Modifier.size(16.dp), tint = if (active) accent else muted)
                                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                    Text(time?.let { formatter.format(it).toPersianDigits() } ?: "—",
                                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                           }
                          }
                            if (index < (schedules.first.size - 1) / 2) HorizontalDivider(color = line.copy(alpha = 0.65f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerHorizon(accent: Color, line: Color) {
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val horizon = size.height * 0.85f
        val center = Offset(size.width * 0.5f, horizon)
        val radius = size.height * 0.42f
        drawLine(line, Offset(0f, horizon), Offset(size.width, horizon), 1.dp.toPx())
        drawArc(accent, 180f, 180f, false,
            Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
            style = Stroke(1.5.dp.toPx()))
        for (step in 0..8) {
            val angle = Math.PI + step * Math.PI / 8
            val inner = radius * 1.25f
            val outer = radius * 1.55f
            drawLine(line,
                Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner),
                Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer),
                1.dp.toPx())
        }
    }
}

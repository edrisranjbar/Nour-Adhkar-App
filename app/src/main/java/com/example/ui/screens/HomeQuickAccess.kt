package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HomeShortcut
import com.example.data.model.HomeShortcuts
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeQuickAccess(viewModel: AdhkarViewModel, fontScale: Float, onOpenPage: (String) -> Unit) {
    val language = LocalAppLanguage.current
    val selected by viewModel.homeShortcuts.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    val shortcuts = selected.mapNotNull { id -> HomeShortcuts.all.firstOrNull { it.id == id } }
    val columns = if (LocalConfiguration.current.screenWidthDp >= 480 && fontScale * LocalDensity.current.fontScale <= 1.3f) 3 else 2
    fun edit() { draft = ArrayList(selected); query = ""; editing = true }
    fun toggle(id: String) {
        draft = ArrayList(if (id in draft) draft.filterNot { it == id } else draft + id)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(language.text("دسترسی سریع"), Modifier.weight(1f), fontSize = (18 * fontScale).sp,
                fontWeight = FontWeight.Bold)
            TextButton(onClick = ::edit) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(language.text("ویرایش"))
            }
        }
        if (shortcuts.isEmpty()) {
            OutlinedCard(onClick = ::edit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AddCircleOutline, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(language.text("میان‌برهای دلخواهتان را به اینجا اضافه کنید."),
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(language.text("افزودن میان‌بر"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else shortcuts.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { shortcut ->
                    OutlinedCard(
                        onClick = {
                            when {
                                shortcut.categoryId != null -> viewModel.selectCategory(shortcut.categoryId)
                                shortcut.id == "prayer_settings" -> viewModel.openPrayerSettings()
                                else -> onOpenPage(shortcut.id)
                            }
                        }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 10.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Icon(shortcutIcon(shortcut), null, Modifier.padding(10.dp).size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(language.text(shortcut.title), textAlign = TextAlign.Center, fontSize = (13 * fontScale).sp)
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    if (editing) {
        ModalBottomSheet(onDismissRequest = { editing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.text("ویرایش دسترسی سریع"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(language.text("صفحه‌ها و اذکار دلخواهتان را انتخاب کنید."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(language.text("جست‌وجوی میان‌بر")) }, leadingIcon = { Icon(Icons.Default.Search, null) })
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp)) {
                    val matches = HomeShortcuts.all.filter { shortcut ->
                        shortcutSearch(language.text(shortcut.title)).contains(shortcutSearch(query))
                    }
                    if (matches.isEmpty()) item {
                        Text(language.text("نتیجه‌ای پیدا نشد"), Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                    }
                    listOf(HomeShortcuts.pages, HomeShortcuts.collections).forEachIndexed { index, group ->
                        val visible = group.filter { it in matches }
                        if (visible.isNotEmpty()) {
                            item {
                                Text(language.text(if (index == 0) "صفحه‌ها" else "اذکار و دعاها"),
                                    Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            items(visible, key = { it.id }) { shortcut ->
                                Row(Modifier.fillMaxWidth().toggleable(shortcut.id in draft, role = Role.Checkbox,
                                    onValueChange = { toggle(shortcut.id) }).padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(shortcutIcon(shortcut), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(language.text(shortcut.title), Modifier.weight(1f))
                                    Checkbox(shortcut.id in draft, onCheckedChange = null)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.text("${draft.size.toPersianDigits()} میان‌بر انتخاب شده"), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { editing = false }) { Text(language.text("لغو")) }
                    Button(onClick = { viewModel.setHomeShortcuts(draft); editing = false }) { Text(language.text("ذخیره")) }
                }
            }
        }
    }
}

private fun shortcutSearch(value: String) = value.trim().replace('ي', 'ی').replace('ك', 'ک').replace("‌", "").replace(" ", "")

private fun shortcutIcon(shortcut: HomeShortcut): ImageVector = when (shortcut.icon) {
    "audio" -> Icons.Default.Headphones
    "tasbih" -> Icons.Default.Grain
    "checklist" -> Icons.Default.Checklist
    "calendar" -> Icons.Default.CalendarMonth
    "qibla" -> Icons.Default.Explore
    "favorite" -> Icons.Default.FavoriteBorder
    "lectures" -> Icons.Default.RecordVoiceOver
    "article" -> Icons.Default.Article
    "fasting" -> Icons.Default.EventRepeat
    "stats" -> Icons.Default.Insights
    "prayer" -> Icons.Default.Mosque
    "settings" -> Icons.Default.Settings
    "wb_sunny", "today" -> Icons.Default.WbSunny
    "nights_stay" -> Icons.Default.NightsStay
    "alarm" -> Icons.Default.Alarm
    "bedtime" -> Icons.Default.Bedtime
    "self_improvement" -> Icons.Default.SelfImprovement
    "mosque" -> Icons.Default.Mosque
    "directions_car" -> Icons.Default.DirectionsCar
    "flight_takeoff" -> Icons.Default.FlightTakeoff
    "flight_land" -> Icons.Default.FlightLand
    "restaurant", "restaurant_menu" -> Icons.Default.Restaurant
    "checkroom" -> Icons.Default.Checkroom
    "nightlight", "brightness_3" -> Icons.Default.Nightlight
    "psychology" -> Icons.Default.Psychology
    "campaign" -> Icons.Default.Campaign
    "volunteer_activism" -> Icons.Default.VolunteerActivism
    else -> Icons.Default.MenuBook
}

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.prayer.MAX_PRAYER_PLACES
import com.example.prayer.PrayerPlace
import com.example.prayer.PrayerPlaces
import com.example.prayer.PrayerSettings
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text

/** «تهران · ‎+۳:۳۰»: city and the place's current UTC offset, kept left-to-right inside RTL text. */
internal fun placeSubtitle(settings: PrayerSettings): String {
    val minutes = TimeZone.getTimeZone(settings.zone).getOffset(System.currentTimeMillis()) / 60_000
    val sign = if (minutes >= 0) "+" else "−"
    val offset = String.format(Locale.US, "%d:%02d", kotlin.math.abs(minutes) / 60, kotlin.math.abs(minutes) % 60)
    val city = settings.location.takeUnless { it == "موقعیت فعلی" } ?: "موقعیت دریافت‌شده"
    return "$city · ‎$sign$offset".toPersianDigits()
}

/** The next prayer (sunrise excluded) at [settings] today or tomorrow, e.g. «ظهر ۱۲:۰۴». */
internal fun nextPrayerAt(settings: PrayerSettings, now: Date = Date()): String? = runCatching {
    val zone = TimeZone.getTimeZone(settings.zone)
    val tomorrow = java.util.Calendar.getInstance(zone).apply { time = now; add(java.util.Calendar.DAY_OF_MONTH, 1) }.time
    val next = (settings.times(now) + settings.times(tomorrow))
        .firstOrNull { (label, time) -> label != "طلوع" && time != null && time.after(now) } ?: return@runCatching null
    val formatter = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }
    "${next.first} ${formatter.format(next.second!!)}".toPersianDigits()
}.getOrNull()

/** One selectable place row, shared by the switcher sheet and the settings card. */
@Composable
private fun PlaceRow(
    place: PrayerPlace,
    active: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val language = LocalAppLanguage.current
    val next = remember(place.settings) { nextPrayerAt(place.settings) }
    val description = listOfNotNull(
        place.name, placeSubtitle(place.settings),
        next?.let { "${language.text("نماز بعدی")} $it" }, language.text("فعال").takeIf { active }
    ).joinToString("، ")
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) colors.secondaryContainer else colors.surface,
        border = if (active) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (active) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (active) colors.tertiary else colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(place.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(placeSubtitle(place.settings), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            next?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = if (active) colors.tertiary else colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
            trailing()
        }
    }
}

/**
 * «مکان اوقات شرعی»: switch the active place with one tap. Opened from the chip on the prayer card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerPlacesSheet(viewModel: AdhkarViewModel, places: PrayerPlaces, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); then() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("مکان اوقات شرعی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("اوقات، اذان، یادآوری‌ها و قبله بر اساس مکان انتخاب‌شده محاسبه می‌شوند.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(2.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                places.places.forEach { place ->
                    PlaceRow(place, active = place.id == places.active?.id, onClick = {
                        if (place.id != places.active?.id) {
                            viewModel.switchPlace(place.id)
                            Toast.makeText(context, language.text("اوقات و اذان برای «${place.name}» تنظیم شد"), Toast.LENGTH_SHORT).show()
                        }
                        close()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilledTonalButton(
                    onClick = { close { viewModel.startAddingPlace() } },
                    enabled = places.places.size < MAX_PRAYER_PLACES,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن مکان")
                }
                OutlinedButton(
                    onClick = { close { viewModel.openPrayerSettings() } },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مدیریت مکان‌ها")
                }
            }
            if (places.places.size >= MAX_PRAYER_PLACES) {
                Text("حداکثر ${MAX_PRAYER_PLACES.toPersianDigits()} مکان می‌توانید ذخیره کنید.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The «مکان‌ها» card in prayer settings: switch, rename, reorder, delete and add places. */
@Composable
fun PrayerPlacesManager(viewModel: AdhkarViewModel, places: PrayerPlaces, adding: Boolean) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var renaming by remember { mutableStateOf<PrayerPlace?>(null) }
    var deleting by remember { mutableStateOf<PrayerPlace?>(null) }

    if (places.places.isEmpty()) {
        Text("هنوز مکانی ذخیره نشده است. نخستین مکان را در بخش زیر مشخص کنید.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Text("مکان فعال، اوقات و اذان را تعیین می‌کند. روش محاسبه و اصلاح زمان‌ها برای هر مکان جداگانه ذخیره می‌شود.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        places.places.forEachIndexed { index, place ->
            val isActive = place.id == places.active?.id
            PlaceRow(place, active = isActive, onClick = { if (!isActive) viewModel.switchPlace(place.id) }) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "گزینه‌های ${place.name}")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (!isActive) DropdownMenuItem(text = { Text("فعال کردن") }, onClick = { menu = false; viewModel.switchPlace(place.id) })
                        DropdownMenuItem(text = { Text("تغییر نام") }, onClick = { menu = false; renaming = place })
                        if (index > 0) DropdownMenuItem(text = { Text("انتقال به بالا") }, onClick = { menu = false; viewModel.movePlace(place.id, -1) })
                        if (index < places.places.lastIndex) DropdownMenuItem(text = { Text("انتقال به پایین") }, onClick = { menu = false; viewModel.movePlace(place.id, 1) })
                        if (places.places.size > 1) DropdownMenuItem(
                            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; deleting = place }
                        )
                    }
                }
            }
        }
    }
    if (!adding) {
        FilledTonalButton(
            onClick = { viewModel.startAddingPlace() },
            enabled = places.places.size < MAX_PRAYER_PLACES,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (places.places.size < MAX_PRAYER_PLACES) "افزودن مکان" else "حداکثر ${MAX_PRAYER_PLACES.toPersianDigits()} مکان")
        }
    }

    renaming?.let { place ->
        var name by rememberSaveable(place.id) { mutableStateOf(place.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("تغییر نام مکان") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                    label = { Text("نام مکان") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { viewModel.renamePlace(place.id, name); renaming = null }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("انصراف") } }
        )
    }
    deleting?.let { place ->
        val isActive = place.id == places.active?.id
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف «${place.name}»؟") },
            text = {
                Text(if (isActive) "این مکان فعال است؛ پس از حذف، «${places.places.first { it.id != place.id }.name}» فعال می‌شود."
                    else "تنظیمات و اصلاح زمان‌های این مکان حذف می‌شود.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePlace(place.id)
                    deleting = null
                    Toast.makeText(context, language.text("«${place.name}» حذف شد"), Toast.LENGTH_SHORT).show()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}

/** Compact chip on the prayer card showing the active place; opens the switcher. */
@Composable
fun ActivePlaceChip(name: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.10f),
        modifier = Modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "مکان اوقات شرعی: $name. برای تغییر لمس کنید" }
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(name, style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            Text(" ▾", style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}

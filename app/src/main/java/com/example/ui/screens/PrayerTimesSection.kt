package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prayer.MAX_OFFSET_MINUTES
import com.example.prayer.PrayerSettings
import com.example.prayer.prayerMethods
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun PrayerSettingsEditor(viewModel: AdhkarViewModel) {
    val places by viewModel.prayerPlaces.collectAsState()
    val adding by viewModel.addingPlace.collectAsState()
    // Leaving prayer settings abandons an unfinished new place.
    DisposableEffect(Unit) { onDispose { viewModel.cancelAddingPlace() } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PrayerSettingsGroup("صدای اذان") { AdhanSoundSettings(viewModel) }
        PrayerSettingsGroup("پخش اذان در") { AdhanPrayerSettings(viewModel) }
        PrayerSettingsGroup("یادآوری اذکار پس از نماز") { PostPrayerReminderSettings(viewModel) }
        PrayerSettingsGroup("مکان‌ها") { PrayerPlacesManager(viewModel, places, adding) }
        // A fresh draft for each place: switching or starting a new place resets the editor.
        key(places.activeId, adding) {
            PlaceEditor(viewModel, adding = adding || places.places.isEmpty(), activeName = places.active?.name)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun PlaceEditor(viewModel: AdhkarViewModel, adding: Boolean, activeName: String?) {
    val language = LocalAppLanguage.current
    val active by viewModel.prayerSettings.collectAsState()
    // A new place starts empty but copies the active place's method, madhab and corrections.
    val saved = if (adding) active.copy(location = "", automaticLocation = false, zone = TimeZone.getDefault().id) else active
    val hasPlaces = viewModel.prayerPlaces.collectAsState().value.places.isNotEmpty()
    var placeName by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf(saved.location) }
    var lat by rememberSaveable { mutableStateOf(if (saved.location.isEmpty()) "" else saved.latitude.toString()) }
    var lon by rememberSaveable { mutableStateOf(if (saved.location.isEmpty()) "" else saved.longitude.toString()) }
    var zone by rememberSaveable { mutableStateOf(saved.zone) }
    var method by rememberSaveable { mutableStateOf(saved.method) }
    var hanafi by rememberSaveable { mutableStateOf(saved.hanafi) }
    var offsets by rememberSaveable { mutableStateOf(saved.offsets) }
    var automatic by rememberSaveable { mutableStateOf(saved.automaticLocation) }
    var hasDetectedLocation by rememberSaveable { mutableStateOf(saved.automaticLocation && saved.location.isNotBlank()) }
    var message by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var asrExpanded by remember { mutableStateOf(false) }
    var citySelected by rememberSaveable { mutableStateOf(!adding && saved.isValid()) }
    val forPlace = if (!adding && activeName != null) " · $activeName" else ""
    val timeZones = remember(zone) {
        (listOf(zone, TimeZone.getDefault().id, "Asia/Tehran", "UTC") +
            TimeZone.getAvailableIDs().sorted()).distinct()
    }
    val zoneLabels = remember(timeZones, language) { timeZones.associateWith { persianTimeZoneLabel(it, language.code) } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PrayerSettingsGroup(if (adding && hasPlaces) "مکان تازه" else "موقعیت و منطقه زمانی$forPlace") {
        if (adding && hasPlaces) {
            OutlinedTextField(
                value = placeName, onValueChange = { placeName = it.take(40) },
                label = { Text("نام مکان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("خانه", "محل کار", "خانه والدین", "سفر").forEach { suggestion ->
                    SuggestionChip(onClick = { placeName = suggestion }, label = { Text(suggestion) })
                }
            }
        }
        Column(Modifier.selectableGroup()) {
            listOf(true to "تشخیص خودکار موقعیت", false to "ورود دستی موقعیت").forEach { (isAutomatic, label) ->
                Row(Modifier.fillMaxWidth().selectable(
                    selected = automatic == isAutomatic, role = Role.RadioButton,
                    onClick = { automatic = isAutomatic; message = "" }
                ).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = automatic == isAutomatic, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }
        }
        if (automatic) {
            DetectPrayerLocationButton(onLocation = { detected ->
            location = "موقعیت فعلی"
            if (placeName.isBlank()) placeName = "مکان من"
            lat = detected.latitude.toString()
            lon = detected.longitude.toString()
            zone = TimeZone.getDefault().id
            hasDetectedLocation = true
            citySelected = true
            message = "موقعیت دریافت شد؛ منطقه زمانی از گوشی گرفته شده است. بررسی و ذخیره کنید."
        }, onError = { message = it })
            Text(if (hasDetectedLocation) "موقعیت دریافت‌شده: ${lat.toPersianDigits()}، ${lon.toPersianDigits()}"
                else "برای تعیین موقعیت، دکمه دریافت موقعیت را لمس کنید.")
        } else {
            CityLocationSearch(query = location, onQueryChange = {
                location = it
                citySelected = false
                hasDetectedLocation = false
                lat = ""; lon = ""; message = ""
            }, onSelected = { name, latitude, longitude ->
                location = name
                if (placeName.isBlank()) placeName = name.substringBefore("،").substringBefore(",").trim()
                lat = latitude.toString(); lon = longitude.toString()
                citySelected = true
                message = "شهر انتخاب شد؛ منطقه زمانی را بررسی و ذخیره کنید."
            })
        }
        ExposedDropdownMenuBox(expanded = zoneExpanded, onExpandedChange = { zoneExpanded = it }) {
            OutlinedTextField(
                value = zoneLabels[zone].orEmpty(), onValueChange = {}, readOnly = true,
                label = { Text("منطقه زمانی") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(zoneExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = zoneExpanded, onDismissRequest = { zoneExpanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                timeZones.forEach { id ->
                    DropdownMenuItem(text = { Text(zoneLabels[id].orEmpty()) }, onClick = {
                        zone = id
                        zoneExpanded = false
                        message = ""
                    })
                }
            }
        }
        }
        PrayerSettingsGroup("روش محاسبه$forPlace") {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = language.text(prayerMethods[method].orEmpty()), onValueChange = {}, readOnly = true,
                label = { Text("روش محاسبه") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                prayerMethods.forEach { (id, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { method = id; expanded = false; message = "" })
                }
            }
        }
        if (method == "UMM_AL_QURA") {
            Text("در این روش، عشا ۹۰ دقیقه پس از مغرب محاسبه می‌شود. افزایش ۳۰ دقیقه‌ای رمضان هنوز خودکار اعمال نمی‌شود؛ در رمضان زمان عشا را با تقویم معتبر محلی تطبیق دهید.",
                style = MaterialTheme.typography.bodySmall)
        }
        ExposedDropdownMenuBox(expanded = asrExpanded, onExpandedChange = { asrExpanded = it }) {
            OutlinedTextField(value = language.text(if (hanafi) "حنفی" else "شافعی / مالکی / حنبلی"),
                onValueChange = {}, readOnly = true,
                label = { Text("محاسبه وقت عصر") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(asrExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
            ExposedDropdownMenu(expanded = asrExpanded, onDismissRequest = { asrExpanded = false }) {
                listOf(false to "شافعی / مالکی / حنبلی", true to "حنفی").forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        hanafi = value; asrExpanded = false; message = ""
                    })
                }
            }
        }
        }
        PrayerSettingsGroup("اصلاح زمان‌های محاسبه‌شده") {
            Text("اگر با تقویم محلی تفاوت دارد، هر زمان را تا ۳۰ دقیقه زودتر یا دیرتر کنید. پس از ذخیره روی اوقات، اذان و یادآوری‌ها اعمال می‌شود.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val previewDate by produceState(Date()) {
                while (true) { delay(60_000L); value = Date() }
            }
            val previewDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone(zone.trim())
            }.format(previewDate)
            val baseTimes = remember(location, lat, lon, zone, method, hanafi, automatic, citySelected, hasDetectedLocation, previewDay) {
                val draft = PrayerSettings(location.trim(), coordinateNumber(lat), coordinateNumber(lon),
                    zone.trim(), method, hanafi, automatic)
                if (draft.isValid() && (if (automatic) hasDetectedLocation else citySelected)) draft.times(previewDate)
                else null
            }
            val previewFormatter = remember(zone) {
                SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone(zone.trim()) }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    listOf("صبح", "طلوع", "ظهر", "عصر", "مغرب", "عشاء").forEachIndexed { index, label ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        OffsetRow(
                            label = label,
                            adjustment = offsets[index],
                            base = baseTimes?.getOrNull(index)?.second,
                            hasPreview = baseTimes != null,
                            formatter = previewFormatter,
                            onChange = { value ->
                                offsets = offsets.toMutableList().also { it[index] = value.coerceIn(-MAX_OFFSET_MINUTES, MAX_OFFSET_MINUTES) }
                                message = ""
                            }
                        )
                    }
                }
            }
            if (offsets.any { it != 0 }) {
                TextButton(onClick = { offsets = List(6) { 0 }; message = "" }) { Text("حذف همهٔ اصلاحات") }
            }
        }
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            val value = PrayerSettings(location.trim(), coordinateNumber(lat), coordinateNumber(lon), zone.trim(), method, hanafi, automatic, offsets)
            when {
                !value.isValid() -> message = "نام محل، مختصات معتبر و منطقه زمانی صحیح را وارد کنید."
                adding && hasPlaces -> viewModel.addPlace(placeName, value)
                else -> { viewModel.updatePrayerSettings(value); message = "تنظیمات اوقات شرعی ذخیره شد" }
            }
        }, enabled = if (automatic) hasDetectedLocation else citySelected,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary)) {
            Text(if (adding && hasPlaces) "افزودن و فعال کردن این مکان" else "ذخیره تنظیمات")
        }
        if (adding && hasPlaces) {
            TextButton(onClick = { viewModel.cancelAddingPlace() }, modifier = Modifier.fillMaxWidth()) { Text("انصراف") }
        }
        if (message.isNotEmpty()) Text(message)
    }
}

/** One compact correction row: name and today's time (corrected time when adjusted), then a − value + stepper. */
@Composable
private fun OffsetRow(
    label: String,
    adjustment: Int,
    base: Date?,
    hasPreview: Boolean,
    formatter: SimpleDateFormat,
    onChange: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val changed = adjustment != 0
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val time = when {
                base != null && changed -> "${formatter.format(base)} ← ${formatter.format(Date(base.time + adjustment * 60_000L))}"
                base != null -> formatter.format(base)
                hasPreview -> "در دسترس نیست"
                else -> null
            }
            time?.let {
                Text(it.toPersianDigits(), style = MaterialTheme.typography.bodySmall,
                    color = if (changed) colors.tertiary else colors.onSurfaceVariant)
            }
        }
        IconButton(onClick = { onChange(adjustment - 1) }, enabled = adjustment > -MAX_OFFSET_MINUTES) {
            Icon(Icons.Rounded.Remove, contentDescription = "$label یک دقیقه زودتر")
        }
        // The value doubles as a reset: tapping a non-zero correction clears it.
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (changed) colors.secondaryContainer else Color.Transparent,
            modifier = Modifier.widthIn(min = 52.dp).then(
                if (changed) Modifier.clickable(onClickLabel = "حذف اصلاح") { onChange(0) } else Modifier
            )
        ) {
            Text(
                text = when {
                    adjustment > 0 -> "+${adjustment.toPersianDigits()}"
                    adjustment < 0 -> "−${(-adjustment).toPersianDigits()}"
                    else -> "۰"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (changed) FontWeight.Bold else FontWeight.Normal,
                color = if (changed) colors.onSecondaryContainer else colors.onSurfaceVariant
            )
        }
        IconButton(onClick = { onChange(adjustment + 1) }, enabled = adjustment < MAX_OFFSET_MINUTES) {
            Icon(Icons.Rounded.Add, contentDescription = "$label یک دقیقه دیرتر")
        }
    }
}

@Composable
private fun PrayerSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private fun persianTimeZoneLabel(id: String, languageCode: String): String {
    val timeZone = android.icu.util.TimeZone.getTimeZone(id)
    val formatter = android.icu.text.TimeZoneFormat.getInstance(android.icu.util.ULocale(languageCode))
    val now = System.currentTimeMillis()
    val localized = formatter.format(android.icu.text.TimeZoneFormat.Style.GENERIC_LOCATION, timeZone, now)
    val name = when {
        id == "Asia/Tehran" -> "تهران"
        id == "UTC" || id == "Etc/UTC" || id == "GMT" -> "زمان هماهنگ جهانی"
        localized.any { it in 'A'..'Z' || it in 'a'..'z' } -> "منطقه زمانی"
        else -> localized
    }
    val minutes = timeZone.getOffset(now) / 60_000
    val offset = String.format(Locale.US, "%02d:%02d", kotlin.math.abs(minutes) / 60, kotlin.math.abs(minutes) % 60).toPersianDigits()
    return com.example.ui.language.AppLanguage.fromCode(languageCode).text("$name (زمان جهانی ${if (minutes >= 0) "+" else "−"}$offset)")
}

private fun coordinateNumber(text: String): Double = text.trim().map {
    when { it == '٫' -> '.'; it.isDigit() -> Character.getNumericValue(it).toString()[0]; else -> it }
}.joinToString("").toDoubleOrNull() ?: Double.NaN

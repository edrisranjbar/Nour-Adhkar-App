package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text

import androidx.compose.foundation.layout.*
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
@OptIn(ExperimentalMaterial3Api::class)
fun PrayerSettingsEditor(viewModel: AdhkarViewModel) {
    val language = LocalAppLanguage.current
    val saved by viewModel.prayerSettings.collectAsState()
    var location by rememberSaveable { mutableStateOf(saved.location) }
    var lat by rememberSaveable { mutableStateOf(if (saved.location.isEmpty()) "" else saved.latitude.toString()) }
    var lon by rememberSaveable { mutableStateOf(if (saved.location.isEmpty()) "" else saved.longitude.toString()) }
    var zone by rememberSaveable { mutableStateOf(saved.zone) }
    var method by rememberSaveable { mutableStateOf(saved.method) }
    var hanafi by rememberSaveable { mutableStateOf(saved.hanafi) }
    var automatic by rememberSaveable { mutableStateOf(saved.automaticLocation) }
    var hasDetectedLocation by rememberSaveable { mutableStateOf(saved.automaticLocation && saved.location.isNotBlank()) }
    var message by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var zoneExpanded by remember { mutableStateOf(false) }
    var asrExpanded by remember { mutableStateOf(false) }
    var citySelected by rememberSaveable { mutableStateOf(saved.isValid()) }
    val timeZones = remember(zone) {
        (listOf(zone, TimeZone.getDefault().id, "Asia/Tehran", "UTC") +
            TimeZone.getAvailableIDs().sorted()).distinct()
    }
    val zoneLabels = remember(timeZones, language) { timeZones.associateWith { persianTimeZoneLabel(it, language.code) } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PrayerSettingsGroup("صدای اذان") { AdhanSoundSettings(viewModel) }
        PrayerSettingsGroup("پخش اذان در") { AdhanPrayerSettings(viewModel) }
        PrayerSettingsGroup("موقعیت و منطقه زمانی") {
        Text("اوقات به‌صورت آفلاین و بر اساس موقعیت ذخیره‌شده محاسبه می‌شوند. هنگام سفر موقعیت را تغییر دهید.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        PrayerSettingsGroup("روش محاسبه") {
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
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            val value = PrayerSettings(location.trim(), coordinateNumber(lat), coordinateNumber(lon), zone.trim(), method, hanafi, automatic)
            if (value.isValid()) { viewModel.updatePrayerSettings(value); message = "تنظیمات اوقات شرعی ذخیره شد" }
            else message = "نام محل، مختصات معتبر و منطقه زمانی صحیح را وارد کنید."
        }, enabled = if (automatic) hasDetectedLocation else citySelected,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("ذخیره تنظیمات") }
        if (message.isNotEmpty()) Text(message)
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

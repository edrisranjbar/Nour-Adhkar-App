package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prayer.AdhanPrayer
import com.example.prayer.AdhanScheduler
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
fun AdhanPrayerSettings(viewModel: AdhkarViewModel) {
    val selected by viewModel.adhanPrayers.collectAsState()
    val sound by viewModel.adhanSound.collectAsState()
    val location by viewModel.prayerSettings.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exact by remember { mutableStateOf(AdhanScheduler(context).hasAlarmPermission()) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
                exact = AdhanScheduler(context).hasAlarmPermission()
                AdhanScheduler(context).reschedule()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdhanPrayer.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { prayer ->
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).toggleable(
                        value = prayer in selected,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.setAdhanPrayer(prayer, it) }
                    ), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = prayer in selected, onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.tertiary,
                                checkmarkColor = MaterialTheme.colorScheme.onTertiary))
                        Text(prayer.label, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (selected.isNotEmpty()) {
            if (!sound.isSelected) Text("برای پخش اذان، موذن را انتخاب کنید.", style = MaterialTheme.typography.bodySmall)
            if (!location.isValid()) Text("موقعیت را در پایین همین صفحه ذخیره کنید.", style = MaterialTheme.typography.bodySmall)
            if (!notifications) TextButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 26)
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }) { Text("اجازه نمایش اعلان اذان") }
            if (!exact && Build.VERSION.SDK_INT >= 31) TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }) { Text("اجازه پخش در وقت دقیق نماز") }
        }
    }
}

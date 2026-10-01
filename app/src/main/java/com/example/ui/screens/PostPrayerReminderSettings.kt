package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.BatteryOptimization
import com.example.prayer.PostPrayerReminderScheduler
import com.example.ui.components.BatteryOptimizationNotice
import com.example.ui.language.LocalizedText as Text
import com.example.ui.viewmodel.AdhkarViewModel

/**
 * One opt-in for all prayers, 10 minutes after the configured prayer start.
 */
@Composable
fun PostPrayerReminderSettings(viewModel: AdhkarViewModel) {
    val selected by viewModel.postPrayerReminderPrayers.collectAsState()
    val location by viewModel.prayerSettings.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
                // Picks up a newly granted notification permission or a changed location.
                PostPrayerReminderScheduler(context).reschedule()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "یادآوری ۱۰ دقیقه پس از نماز می‌آید؛ یعنی ۴۰ دقیقه پس از اذان و برای مغرب ۱۵ دقیقه پس از اذان.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                value = selected.isNotEmpty(),
                role = Role.Switch,
                onValueChange = {
                    if (it && selected.isEmpty()) BatteryOptimization.request(context)
                    viewModel.setPostPrayerReminderEnabled(it)
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("فعال‌سازی یادآوری اذکار پس از نماز", modifier = Modifier.weight(1f).padding(end = 8.dp))
            Switch(checked = selected.isNotEmpty(), onCheckedChange = null)
        }
        if (selected.isNotEmpty()) {
            if (!location.isValid()) {
                Text("موقعیت را در پایین همین صفحه ذخیره کنید.", style = MaterialTheme.typography.bodySmall)
            }
            if (!notifications) TextButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 26)
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }) { Text("اجازه نمایش اعلان") }
            BatteryOptimizationNotice()
        }
    }
}

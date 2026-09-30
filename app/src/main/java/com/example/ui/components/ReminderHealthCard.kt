package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.BatteryOptimization
import com.example.notifications.ReminderHealth
import com.example.notifications.ReminderVendorGuide
import com.example.ui.language.LocalizedText as Text

/** Bumps every time the screen resumes, so checks refresh after returning from system settings. */
@Composable
private fun rememberResumeTick(): Int {
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

/**
 * Shows whether reminders can reach the user (notifications allowed, battery unrestricted), lets
 * them send a test notification, and, after a test, explains vendor battery-manager settings.
 * Nothing here changes a setting by itself; every button opens a system screen or sends the test.
 */
@Composable
fun ReminderHealthCard(onSendTest: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val notificationsOk = remember(tick) { ReminderHealth.notificationsAllowed(context) }
    val batteryOk = remember(tick) { BatteryOptimization.isIgnoring(context) }
    var testSent by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("سلامت یادآورها", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        HealthRow(
            ok = notificationsOk,
            okText = "اعلان‌ها برای اذکار نور مجاز هستند.",
            problemText = "اعلان‌ها برای اذکار نور بسته است؛ یادآورها نمایش داده نمی‌شوند.",
            actionLabel = "باز کردن تنظیمات اعلان",
            onAction = { ReminderHealth.openNotificationSettings(context) }
        )
        HealthRow(
            ok = batteryOk,
            okText = "محدودیت باتری برداشته شده است.",
            problemText = "برای نمایش به‌موقع یادآورها وقتی برنامه بسته است، محدودیت باتری را بردارید.",
            actionLabel = "اجازه اجرا بدون محدودیت باتری",
            onAction = { BatteryOptimization.request(context) }
        )
        OutlinedButton(onClick = { onSendTest(); testSent = true }) { Text("ارسال اعلان آزمایشی") }
        if (testSent) {
            Text(
                "اعلان آزمایشی تا حدود ۱۰ ثانیه دیگر ارسال می‌شود. برای آزمایش واقعی، صفحه گوشی را قفل کنید و منتظر بمانید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("اگر اعلان نرسید", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                ReminderVendorGuide.stepsFor(ReminderVendorGuide.brandFor(Build.MANUFACTURER)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                ReminderVendorGuide.MENU_NAMES_DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { ReminderHealth.openAppDetails(context) }) { Text("باز کردن تنظیمات برنامه") }
        }
    }
}

@Composable
private fun HealthRow(ok: Boolean, okText: String, problemText: String, actionLabel: String, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                if (ok) okText else problemText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!ok) OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

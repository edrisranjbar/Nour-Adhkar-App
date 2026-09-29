package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notifications.BatteryOptimization
import com.example.ui.language.LocalizedText as Text

/** Remembers whether the app is exempt from battery optimization, refreshed on every resume. */
@Composable
fun rememberIgnoringBatteryOptimization(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var ignoring by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ignoring = BatteryOptimization.isIgnoring(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return ignoring
}

/** Explains why background work needs an unrestricted battery setting; hidden once granted. */
@Composable
fun BatteryOptimizationNotice(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (rememberIgnoringBatteryOptimization()) return
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "برای پخش به‌موقع اذان و نمایش یادآورها وقتی برنامه بسته است، محدودیت باتری را برای اذکار نور بردارید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { BatteryOptimization.request(context) },
            modifier = Modifier.padding(top = 6.dp)
        ) { Text("اجازه اجرا بدون محدودیت باتری") }
    }
}

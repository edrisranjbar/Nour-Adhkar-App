package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import com.example.ui.language.LocalizedText as Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prayer.AdhanAlertService
import com.example.prayer.AdhanSound
import com.example.prayer.adhanRecordings
import com.example.ui.util.toPersianDigits
import com.example.ui.viewmodel.AdhkarViewModel
import kotlin.math.roundToInt

/**
 * Muezzin, a preview that plays exactly like the real adhan (the same service: alarm volume, the
 * saved adhan volume, volume keys stop it), and the adhan volume.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdhanSoundSettings(viewModel: AdhkarViewModel) {
    val sound by viewModel.adhanSound.collectAsState()
    val volume by viewModel.adhanVolume.collectAsState()
    val playing by AdhanAlertService.playingState.collectAsState()
    val selected = adhanRecordings.firstOrNull { it.id == sound.id }
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val owner = LocalLifecycleOwner.current
    var expanded by remember { mutableStateOf(false) }
    // Only a preview started here is stopped when leaving; a real adhan keeps playing.
    var previewing by remember { mutableStateOf(false) }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && previewing) { AdhanAlertService.stop(context); previewing = false }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (previewing) AdhanAlertService.stop(context)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = language.text(selected?.title.orEmpty()),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("موذن") },
                    placeholder = { Text("انتخاب کنید") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    adhanRecordings.forEach { recording ->
                        DropdownMenuItem(
                            text = { Text(recording.title) },
                            onClick = {
                                if (previewing) { AdhanAlertService.stop(context); previewing = false }
                                viewModel.setAdhanSound(AdhanSound(recording.id))
                                expanded = false
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    if (playing) {
                        AdhanAlertService.stop(context)
                        previewing = false
                    } else if (selected != null) {
                        AdhanAlertService.preview(context)
                        previewing = true
                    }
                },
                enabled = selected != null,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "توقف پیش‌نمایش" else "پیش‌نمایش اذان"
                )
            }
        }

        val percent = language.text("${(volume * 100).roundToInt().toPersianDigits()}٪")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("بلندی صدای اذان", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(percent, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = volume,
                onValueChange = viewModel::setAdhanVolume,
                valueRange = 0.1f..1f,
                steps = 8,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp).semantics {
                    contentDescription = language.text("بلندی صدای اذان")
                    stateDescription = percent
                }
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "نسبت به صدای هشدار گوشی. هنگام پخش اذان، دکمهٔ افزایش یا کاهش صدای گوشی اذان را قطع می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

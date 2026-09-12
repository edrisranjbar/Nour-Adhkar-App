package com.example.ui.screens
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prayer.AdhanSound
import com.example.prayer.adhanRecordings
import com.example.ui.viewmodel.AdhkarViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdhanSoundSettings(viewModel: AdhkarViewModel) {
    val sound by viewModel.adhanSound.collectAsState()
    val selected = adhanRecordings.firstOrNull { it.id == sound.id }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var expanded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val preview = remember(context) { AdhanPreview(context) { isPlaying = false } }

    DisposableEffect(owner, preview) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) preview.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            preview.stop()
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
                    value = LocalAppLanguage.current.text(selected?.title.orEmpty()),
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
                                preview.stop()
                                error = ""
                                viewModel.setAdhanSound(AdhanSound(recording.id))
                                expanded = false
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    error = ""
                    if (isPlaying) {
                        preview.stop()
                    } else {
                        selected?.let {
                            isPlaying = true
                            preview.play(it.resource) { error = "پخش صدا ممکن نیست." }
                        }
                    }
                },
                enabled = selected != null,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "توقف پیش‌نمایش" else "پیش‌نمایش اذان"
                )
            }
        }
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private class AdhanPreview(private val context: Context, private val onStopped: () -> Unit) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private val focus = AudioManager.OnAudioFocusChangeListener { if (it < 0) stop() }

    @Suppress("DEPRECATION")
    fun play(resource: Int, onError: () -> Unit) {
        try {
            if (audio.requestAudioFocus(
                    focus,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                ) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            ) {
                stop()
                onError()
                return
            }
            val instance = MediaPlayer()
            player = instance
            instance.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            context.resources.openRawResourceFd(resource).use {
                instance.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            instance.setOnPreparedListener { if (player === it) it.start() }
            instance.setOnCompletionListener { if (player === it) stop() }
            instance.setOnErrorListener { failed, _, _ ->
                if (player === failed) {
                    stop()
                    onError()
                }
                true
            }
            instance.prepareAsync()
        } catch (_: Exception) {
            stop()
            onError()
        }
    }

    @Suppress("DEPRECATION")
    fun stop() {
        player?.release()
        player = null
        audio.abandonAudioFocus(focus)
        onStopped()
    }
}

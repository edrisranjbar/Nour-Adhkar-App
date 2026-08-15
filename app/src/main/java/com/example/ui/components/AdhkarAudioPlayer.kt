package com.example.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.media.AdhkarPlaybackService
import com.example.media.AudioPlaybackState
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SandDark
import com.example.ui.theme.SoftBorder
import com.example.ui.theme.SunGold
import com.example.ui.util.toPersianDigits
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun AdhkarAudioPlayer(
    categoryId: String,
    fontScale: Float,
    onPlaybackCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (categoryId != "morning" && categoryId != "evening") return

    val context = LocalContext.current
    var playbackService by remember { mutableStateOf<AdhkarPlaybackService?>(null) }
    val disconnectedState = remember { MutableStateFlow(AudioPlaybackState()) }
    val playbackState by (playbackService?.state ?: disconnectedState).collectAsState()
    var sliderPositionMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                playbackService = (binder as? AdhkarPlaybackService.LocalBinder)?.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playbackService = null
            }
        }
        context.bindService(
            Intent(context, AdhkarPlaybackService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        onDispose {
            runCatching { context.unbindService(connection) }
            playbackService = null
        }
    }

    val isCurrentTrack = playbackState.categoryId == categoryId
    val isPlaying = isCurrentTrack && playbackState.isPlaying
    val isLoading = isCurrentTrack && playbackState.isLoading
    val isDownloading = isCurrentTrack && playbackState.isDownloading
    val durationMs = if (isCurrentTrack) playbackState.durationMs else 0L
    val positionMs = if (isCurrentTrack) playbackState.positionMs else 0L
    val completionEventId = if (isCurrentTrack) playbackState.completionEventId else 0L
    LaunchedEffect(completionEventId) {
        if (completionEventId != 0L) {
            playbackService?.consumeCompletionEvent(completionEventId)
            onPlaybackCompleted()
        }
    }
    LaunchedEffect(positionMs, isSeeking) {
        if (!isSeeking) sliderPositionMs = positionMs
    }
    val title = if (categoryId == "morning") "اذکار الصباح" else "اذکار المساء"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        val playIntent = Intent(context, AdhkarPlaybackService::class.java).apply {
                            action = AdhkarPlaybackService.ACTION_PLAY
                            putExtra(AdhkarPlaybackService.EXTRA_CATEGORY_ID, categoryId)
                        }
                        ContextCompat.startForegroundService(context, playIntent)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SunGold, CircleShape)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            progress = {
                                playbackState.downloadPercent?.div(100f) ?: 0f
                            },
                            modifier = Modifier.size(23.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "توقف موقت" else "پخش",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = SunGold,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = title,
                            fontSize = (15 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = SandDark,
                            modifier = Modifier.padding(start = 7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "با صدای مشاری راشد العفاسی",
                        fontSize = (11.5 * fontScale).sp,
                        color = NightBlue
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = SunGold,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = playbackState.downloadPercent?.let {
                            "در حال دریافت برای استفاده آفلاین — ${it.toPersianDigits()}٪"
                        } ?: "در حال دریافت برای استفاده آفلاین…",
                        fontSize = (10.5 * fontScale).sp,
                        color = NightBlue,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = sliderPositionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                onValueChange = {
                    isSeeking = true
                    sliderPositionMs = it.toLong()
                },
                onValueChangeFinished = {
                    playbackService?.takeIf { isCurrentTrack }?.seekTo(sliderPositionMs)
                    isSeeking = false
                },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                enabled = durationMs > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = SunGold,
                    activeTrackColor = SunGold,
                    inactiveTrackColor = SoftBorder
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = formatAudioTime(if (isSeeking) sliderPositionMs else positionMs),
                    fontSize = (10.5 * fontScale).sp,
                    color = NightBlue
                )
                Text(
                    text = formatAudioTime(durationMs),
                    fontSize = (10.5 * fontScale).sp,
                    color = NightBlue
                )
            }

            if (isCurrentTrack) {
                playbackState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = (11 * fontScale).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }
    }
}

private fun formatAudioTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds).toPersianDigits()
}

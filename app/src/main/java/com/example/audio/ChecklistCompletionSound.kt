package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** A short, gently enveloped confirmation sound kept preloaded in a static audio buffer. */
class ChecklistCompletionSound {
    private val sampleRate = 44_100
    private val durationSeconds = 0.22
    private val samples = ShortArray((sampleRate * durationSeconds).toInt()) { index ->
        val time = index.toDouble() / sampleRate
        val progress = time / durationSeconds
        val attack = (progress / 0.08).coerceIn(0.0, 1.0)
        val release = ((1.0 - progress) / 0.55).coerceIn(0.0, 1.0)
        val envelope = attack * release * release
        val firstNote = sin(2.0 * PI * 659.25 * time)
        val secondNoteProgress = ((time - 0.075) / 0.025).coerceIn(0.0, 1.0)
        val secondNote = sin(2.0 * PI * 783.99 * time) * secondNoteProgress
        ((firstNote * 0.55 + secondNote * 0.45) * envelope * Short.MAX_VALUE * 0.18)
            .toInt()
            .toShort()
    }

    private val track: AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { it.write(samples, 0, samples.size) }
    }.getOrNull()

    fun play() {
        val audioTrack = track ?: return
        runCatching {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) audioTrack.pause()
            audioTrack.setPlaybackHeadPosition(0)
            audioTrack.play()
        }
    }

    fun release() {
        runCatching { track?.release() }
    }
}

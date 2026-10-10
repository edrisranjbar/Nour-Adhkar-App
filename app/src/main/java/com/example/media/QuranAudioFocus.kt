package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Audio focus for a Quran player, behaving like a normal media app: a permanent loss (another
 * player) pauses; a transient loss (call, assistant) pauses and resumes when focus returns; a
 * "can duck" loss (navigation prompt) lowers the volume.
 */
internal class QuranAudioFocus(
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onVolume: (Float) -> Unit
) : AudioManager.OnAudioFocusChangeListener {
    private var audioManager: AudioManager? = null
    private var request: AudioFocusRequest? = null
    private var held = false
    private var resumeOnGain = false

    /** True when playback may start. Safe to call repeatedly. */
    fun request(context: Context): Boolean {
        if (held) return true
        val manager = audioManager ?: (context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .also { audioManager = it }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .setWillPauseWhenDucked(false)
                .build()
                .also { request = it }
            manager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        held = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        resumeOnGain = false
        return held
    }

    fun abandon() {
        resumeOnGain = false
        if (!held) return
        held = false
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(this)
        }
    }

    /** The user paused: don't resume automatically after a call ends. */
    fun userPaused() {
        resumeOnGain = false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                onPause()
                abandon()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnGain = true
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onVolume(DUCK_VOLUME)
            AudioManager.AUDIOFOCUS_GAIN -> {
                held = true
                onVolume(1f)
                if (resumeOnGain) {
                    resumeOnGain = false
                    onResume()
                }
            }
        }
    }

    private companion object {
        const val DUCK_VOLUME = 0.2f
    }
}

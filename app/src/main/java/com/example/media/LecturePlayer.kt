package com.example.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LectureState(
    val lectureId: String? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: Float = 1f,
    val error: String? = null
)

/** Streams one lecture at a time; separate from the adhkar and Quran players. */
object LecturePlayer {
    private var player: MediaPlayer? = null
    private var prepared = false
    private val _state = MutableStateFlow(LectureState())
    val state: StateFlow<LectureState> = _state.asStateFlow()
    private const val NET_ERROR = "پخش انجام نشد. اتصال اینترنت را بررسی کنید."

    fun play(lectureId: String, url: String) {
        if (_state.value.lectureId == lectureId && player != null && _state.value.error == null) {
            resume(); return
        }
        release()
        val speed = _state.value.speed
        _state.value = LectureState(lectureId = lectureId, isLoading = true, speed = speed)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (player === it) {
                    prepared = true
                    applySpeed(it, speed)
                    it.start()
                    _state.value = _state.value.copy(isLoading = false, isPlaying = true, durationMs = it.duration)
                }
            }
            mp.setOnCompletionListener {
                if (player === it) _state.value = _state.value.copy(isPlaying = false, positionMs = it.duration)
            }
            mp.setOnErrorListener { it, _, _ ->
                if (player === it) {
                    release()
                    _state.value = LectureState(lectureId = lectureId, speed = speed, error = NET_ERROR)
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            release()
            _state.value = LectureState(lectureId = lectureId, speed = speed, error = NET_ERROR)
        }
    }

    fun togglePlayPause() {
        val mp = player ?: return
        if (!prepared) return
        if (mp.isPlaying) {
            mp.pause(); _state.value = _state.value.copy(isPlaying = false)
        } else resume()
    }

    private fun resume() {
        val mp = player ?: return
        if (!prepared) return
        if (mp.currentPosition >= mp.duration - 300) mp.seekTo(0)
        mp.start(); _state.value = _state.value.copy(isPlaying = true)
    }

    fun seekTo(ms: Int) {
        val mp = player ?: return
        if (!prepared) return
        mp.seekTo(ms.coerceIn(0, mp.duration)); _state.value = _state.value.copy(positionMs = ms)
    }

    fun skip(deltaMs: Int) = seekTo((player?.takeIf { prepared }?.currentPosition ?: return) + deltaMs)

    fun setSpeed(speed: Float) {
        _state.value = _state.value.copy(speed = speed)
        player?.takeIf { prepared }?.let { applySpeed(it, speed) }
    }

    /** Called by the UI on a timer while visible. */
    fun refreshPosition() {
        val mp = player ?: return
        if (prepared) _state.value = _state.value.copy(positionMs = mp.currentPosition)
    }

    fun stop() {
        release()
        _state.value = LectureState(speed = _state.value.speed)
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        runCatching {
            val wasPlaying = mp.isPlaying
            mp.playbackParams = PlaybackParams().setSpeed(speed)
            if (!wasPlaying) mp.pause() // setting params can auto-start playback
        }
    }

    private fun release() {
        prepared = false
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

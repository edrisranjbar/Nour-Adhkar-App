package com.example.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QuranAudioState(
    val surah: Int? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadPercent: Int? = null,
    val mobileConfirmationBytes: Long? = null,
    val reciterId: String? = null,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
)

/** Downloads once, then plays locally; each request owns its callbacks and cancellation. */
object QuranAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    @Volatile private var generation = 0L
    private var player: MediaPlayer? = null
    private var prepared = false
    private val _state = MutableStateFlow(QuranAudioState())
    val state: StateFlow<QuranAudioState> = _state.asStateFlow()

    fun play(context: Context, reciter: QuranReciter, surah: Int, allowMobile: Boolean = false) {
        require(surah in 1..114)
        if (_state.value.reciterId == reciter.id && _state.value.surah == surah &&
            _state.value.error == null && _state.value.mobileConfirmationBytes == null &&
            (player != null || job?.isActive == true)) {
            if (prepared && player != null && !_state.value.isPlaying) togglePlayPause()
            return
        }
        stop()
        val request = generation
        val app = context.applicationContext
        _state.value = QuranAudioState(surah = surah, reciterId = reciter.id, isLoading = true)
        job = scope.launch {
            try {
                val store = QuranAudioStore(app)
                val local = withContext(Dispatchers.IO) {
                    store.stored(reciter.id, surah) ?: store.download(reciter, surah, permitDownload = { bytes ->
                        val network = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val wifi = network.getNetworkCapabilities(network.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                        if (!wifi && !allowMobile) {
                            _state.update { if (request == generation) it.copy(isLoading = false, mobileConfirmationBytes = bytes) else it }
                            false
                        } else true
                    }, progress = { percent ->
                        _state.update { if (request == generation) it.copy(isLoading = true, isDownloading = true, downloadPercent = percent) else it }
                    })
                }
                ensureActive()
                if (request == generation && local != null) playLocal(local.absolutePath, surah, reciter.id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == generation) _state.value = QuranAudioState(surah = surah, reciterId = reciter.id, error =
                    if (error is AudioStorageFullException) "فضای کافی برای دانلود وجود ندارد. کمی فضا آزاد کنید."
                    else "این تلاوت برای بار اول به اینترنت نیاز دارد. اتصال را بررسی و دوباره تلاش کنید."
                )
            }
        }
    }

    private fun playLocal(path: String, surah: Int, reciterId: String) {
        release()
        _state.value = QuranAudioState(surah = surah, reciterId = reciterId, isLoading = true)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(path)
            mp.setOnPreparedListener {
                if (player === it) {
                    prepared = true
                    it.start()
                    _state.value = _state.value.copy(isLoading = false, isPlaying = true, durationMs = it.duration)
                }
            }
            mp.setOnCompletionListener { if (player === it) _state.value = _state.value.copy(isPlaying = false, positionMs = it.duration) }
            mp.setOnErrorListener { it, _, _ ->
                if (player === it) {
                    release()
                    _state.value = _state.value.copy(isLoading = false, isPlaying = false, error = "پخش انجام نشد. اتصال اینترنت را بررسی کنید.")
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            release()
            _state.value = _state.value.copy(isLoading = false, isPlaying = false, error = "پخش انجام نشد. اتصال اینترنت را بررسی کنید.")
        }
    }

    fun stop() {
        generation++
        job?.cancel()
        job = null
        release()
        _state.value = QuranAudioState()
    }

    fun clearError() {
        if (_state.value.error != null) stop()
    }

    fun togglePlayPause() {
        val mp = player ?: return
        if (!prepared) return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            if (mp.currentPosition >= mp.duration - 300) mp.seekTo(0)
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun seekTo(position: Int) {
        val mp = player ?: return
        if (!prepared) return
        val target = position.coerceIn(0, mp.duration)
        mp.seekTo(target)
        _state.value = _state.value.copy(positionMs = target)
    }

    fun skip(delta: Int) { if (prepared) player?.let { seekTo(it.currentPosition + delta) } }
    fun refreshPosition() { if (prepared) player?.let { _state.value = _state.value.copy(positionMs = it.currentPosition) } }

    private fun release() {
        prepared = false
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

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

/** A reciter whose complete per-surah recitations are streamed from mp3quran.net (`NNN.mp3`). */
data class QuranReciter(val id: String, val faName: String, val arName: String, val baseUrl: String) {
    fun surahUrl(surah: Int) = "$baseUrl/${surah.toString().padStart(3, '0')}.mp3"
}

/** Ten widely known reciters; each base URL was checked to serve 001.mp3 and 114.mp3. */
val QuranReciters = listOf(
    QuranReciter("afs", "مشاری راشد العفاسی", "مشاري راشد العفاسي", "https://server8.mp3quran.net/afs"),
    QuranReciter("basit", "عبدالباسط عبدالصمد", "عبد الباسط عبد الصمد", "https://server7.mp3quran.net/basit"),
    QuranReciter("sds", "عبدالرحمن السدیس", "عبد الرحمن السديس", "https://server11.mp3quran.net/sds"),
    QuranReciter("shur", "سعود الشریم", "سعود الشريم", "https://server7.mp3quran.net/shur"),
    QuranReciter("husr", "محمود خلیل الحصری", "محمود خليل الحصري", "https://server13.mp3quran.net/husr"),
    QuranReciter("minsh", "محمد صدیق المنشاوی", "محمد صديق المنشاوي", "https://server10.mp3quran.net/minsh"),
    QuranReciter("maher", "ماهر المعیقلی", "ماهر المعيقلي", "https://server12.mp3quran.net/maher"),
    QuranReciter("s_gmd", "سعد الغامدی", "سعد الغامدي", "https://server7.mp3quran.net/s_gmd"),
    QuranReciter("ajm", "احمد العجمی", "أحمد العجمي", "https://server10.mp3quran.net/ajm"),
    QuranReciter("yasser", "یاسر الدوسری", "ياسر الدوسري", "https://server11.mp3quran.net/yasser")
)

data class QuranAudioState(
    val surah: Int? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadPercent: Int? = null,
    val mobileConfirmationBytes: Long? = null,
    val reciterId: String? = null,
    val error: String? = null
)

/** Downloads once, then plays locally; each request owns its callbacks and cancellation. */
object QuranAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    @Volatile private var generation = 0L
    private var player: MediaPlayer? = null
    private val _state = MutableStateFlow(QuranAudioState())
    val state: StateFlow<QuranAudioState> = _state.asStateFlow()

    fun play(context: Context, reciter: QuranReciter, surah: Int, allowMobile: Boolean = false) {
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
                            if (request == generation) _state.value = QuranAudioState(
                                surah = surah, reciterId = reciter.id, mobileConfirmationBytes = bytes
                            )
                            false
                        } else true
                    }, progress = { percent ->
                        if (request == generation) _state.value = QuranAudioState(
                            surah = surah, reciterId = reciter.id, isLoading = true,
                            isDownloading = true, downloadPercent = percent
                        )
                    })
                }
                ensureActive()
                if (request == generation && local != null) playLocal(local.absolutePath, surah, reciter.id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == generation) _state.value = QuranAudioState(surah = surah, error =
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
                    it.start()
                    _state.value = QuranAudioState(surah = surah, reciterId = reciterId, isPlaying = true)
                }
            }
            mp.setOnCompletionListener { if (player === it) stop() }
            mp.setOnErrorListener { it, _, _ ->
                if (player === it) {
                    release()
                    _state.value = QuranAudioState(surah = surah, error = "پخش انجام نشد. اتصال اینترنت را بررسی کنید.")
                }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            release()
            _state.value = QuranAudioState(surah = surah, error = "پخش انجام نشد. اتصال اینترنت را بررسی کنید.")
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
        if (_state.value.error != null) _state.value = QuranAudioState()
    }

    private fun release() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One audio file in the play order; [bismillah] plays 1:1 before a surah's first verse. */
data class QuranAyahTrack(val surah: Int, val ayah: Int, val bismillah: Boolean = false)

data class QuranAyahState(
    val reciterId: String? = null,
    val track: QuranAyahTrack? = null,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val error: String? = null,
    /** A one-off message (a skipped verse); the reader shows it once and clears it. */
    val notice: String? = null
) {
    val active: Boolean get() = track != null
    /** The verse to highlight; none while the bismillah before a surah plays. */
    val verseId: String? get() = track?.takeUnless { it.bismillah }?.let { "${it.surah}:${it.ayah}" }
}

/** Play order across surah boundaries. [verseCounts] holds the 114 surahs' verse counts. */
class QuranAyahQueue(private val verseCounts: List<Int>) {
    init { require(verseCounts.size == 114) }

    fun first(surah: Int, ayah: Int): QuranAyahTrack {
        require(surah in 1..114 && ayah in 1..verseCounts[surah - 1])
        return QuranAyahTrack(surah, ayah, bismillah = ayah == 1 && surah != 1 && surah != 9)
    }

    fun next(track: QuranAyahTrack): QuranAyahTrack? = when {
        track.bismillah -> track.copy(bismillah = false)
        track.ayah < verseCounts[track.surah - 1] -> QuranAyahTrack(track.surah, track.ayah + 1)
        track.surah < 114 -> first(track.surah + 1, 1)
        else -> null
    }

    fun previous(track: QuranAyahTrack): QuranAyahTrack? = when {
        !track.bismillah && track.ayah > 1 -> QuranAyahTrack(track.surah, track.ayah - 1)
        track.surah > 1 -> QuranAyahTrack(track.surah - 1, verseCounts[track.surah - 2])
        else -> null
    }
}

/**
 * Downloads each verse once into the trimmable cache and plays it locally, prefetching the next.
 * Holds audio focus like a normal media app, keeps playing in the background through
 * [QuranPlaybackService], and skips a verse that still fails after one retry.
 */
object QuranAyahPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var player: MediaPlayer? = null
    private var prepared = false
    /** Set by any pause (user or audio focus); the next verse waits instead of starting. */
    private var pausedRequested = false
    private var reciter: QuranAyahReciter? = null
    private var queue: QuranAyahQueue? = null
    private var appContext: Context? = null
    private val focus = QuranAudioFocus(
        onPause = { pauseInternal() },
        onResume = { resumeInternal() },
        onVolume = { volume -> player?.setVolume(volume, volume) }
    )
    private val _state = MutableStateFlow(QuranAyahState())
    val state: StateFlow<QuranAyahState> = _state.asStateFlow()

    private const val RETRY_DELAY_MS = 1_000L
    /** Several missing verses in a row usually means no connection: stop with the error instead. */
    private const val MAX_CONSECUTIVE_FAILURES = 3
    const val SKIPPED_NOTICE = "این آیه دریافت نشد؛ آیهٔ بعدی پخش می‌شود."

    fun play(context: Context, voice: QuranAyahReciter, verseCounts: List<Int>, surah: Int, ayah: Int) {
        val order = QuranAyahQueue(verseCounts)
        start(context.applicationContext, voice, order, order.first(surah, ayah))
    }

    /** Restart the current verse with another voice; does nothing when idle. */
    fun changeReciter(context: Context, voice: QuranAyahReciter) {
        val order = queue ?: return
        val track = _state.value.track ?: return
        start(context.applicationContext, voice, order, track)
    }

    fun next(context: Context) = jump(context) { order, track -> order.next(track) }
    fun previous(context: Context) = jump(context) { order, track -> order.previous(track) }

    private fun jump(context: Context, pick: (QuranAyahQueue, QuranAyahTrack) -> QuranAyahTrack?) {
        val order = queue ?: return
        val voice = reciter ?: return
        val track = _state.value.track ?: return
        pick(order, track)?.let { start(context.applicationContext, voice, order, it) }
    }

    fun togglePlayPause() {
        if (_state.value.track == null) return
        if (_state.value.isPlaying) pause() else resumeInternal()
    }

    /** A user pause (screen, notification, headset): playback won't resume by itself after a call. */
    fun pause() {
        focus.userPaused()
        pauseInternal()
    }

    fun resume() = resumeInternal()

    fun stop() {
        job?.cancel()
        job = null
        release()
        focus.abandon()
        pausedRequested = false
        _state.value = QuranAyahState()
    }

    fun clearError() {
        if (_state.value.error != null) _state.value = QuranAyahState()
    }

    fun clearNotice() {
        if (_state.value.notice != null) _state.update { it.copy(notice = null) }
    }

    private fun pauseInternal() {
        if (_state.value.track == null) return
        pausedRequested = true
        player?.let { mp -> runCatching { if (mp.isPlaying) mp.pause() } }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun resumeInternal() {
        val context = appContext ?: return
        if (_state.value.track == null) return
        pausedRequested = false
        val mp = player
        // Between verses the next one starts by itself once it is ready.
        if (mp == null || !prepared) return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) return
        if (!focus.request(context)) {
            pausedRequested = true
            return
        }
        runCatching { mp.setVolume(1f, 1f); mp.start() }
        _state.update { it.copy(isPlaying = true) }
    }

    private fun start(app: Context, voice: QuranAyahReciter, order: QuranAyahQueue, from: QuranAyahTrack) {
        stop()
        // One Quran recitation at a time: verse-by-verse replaces a surah recitation.
        QuranAudioPlayer.stop()
        appContext = app
        reciter = voice
        queue = order
        val cache = QuranAyahCache(app)
        _state.value = QuranAyahState(reciterId = voice.id, track = from, isLoading = true)
        QuranPlaybackService.start(app)
        job = scope.launch {
            var track: QuranAyahTrack? = from
            var prefetched: Deferred<File?>? = null
            var failures = 0
            try {
                while (track != null) {
                    val current: QuranAyahTrack = track
                    val upcoming = order.next(current)
                    // A failed prefetch is retried here, in the foreground.
                    var file = prefetched?.await() ?: withContext(Dispatchers.IO) { cache.cached(voice, current) }
                    if (file == null) {
                        _state.update { it.copy(track = current, isLoading = true, isPlaying = false) }
                        file = fetchWithRetry(cache, voice, current)
                    }
                    prefetched = upcoming?.let { async(Dispatchers.IO) { runCatching { cache.fetch(voice, it) }.getOrNull() } }
                    val played = file != null && playWithRetry(file, cache, voice, current) { started ->
                        _state.update { it.copy(track = current, isLoading = false, isPlaying = started) }
                    }
                    if (played) {
                        failures = 0
                    } else {
                        failures++
                        if (failures >= MAX_CONSECUTIVE_FAILURES) throw IOException("verses unavailable")
                        if (upcoming != null) _state.update { it.copy(notice = SKIPPED_NOTICE) }
                    }
                    track = upcoming
                }
                release()
                focus.abandon()
                _state.value = QuranAyahState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                release()
                focus.abandon()
                _state.value = QuranAyahState(
                    reciterId = voice.id,
                    error = if (error is AudioStorageFullException) "فضای کافی برای پخش وجود ندارد. کمی فضا آزاد کنید."
                    else "پخش آیه انجام نشد. اتصال اینترنت را بررسی و دوباره تلاش کنید."
                )
            }
        }
    }

    /** Downloads a verse, trying once more after a short pause; null when it stays unavailable. */
    private suspend fun fetchWithRetry(cache: QuranAyahCache, voice: QuranAyahReciter, track: QuranAyahTrack): File? {
        repeat(2) { attempt ->
            try {
                return withContext(Dispatchers.IO) { cache.fetch(voice, track) }
            } catch (storage: AudioStorageFullException) {
                throw storage
            } catch (ignored: IOException) {
                if (attempt == 0) delay(RETRY_DELAY_MS)
            }
        }
        return null
    }

    /** A file Android cannot play is deleted and downloaded once more before the verse is skipped. */
    private suspend fun playWithRetry(
        file: File,
        cache: QuranAyahCache,
        voice: QuranAyahReciter,
        track: QuranAyahTrack,
        onReady: (started: Boolean) -> Unit
    ): Boolean {
        try {
            playFile(file, onReady)
            return true
        } catch (ignored: IOException) {
            file.delete()
        }
        val again = fetchWithRetry(cache, voice, track) ?: return false
        return try {
            playFile(again, onReady)
            true
        } catch (ignored: IOException) {
            again.delete()
            false
        }
    }

    private suspend fun playFile(file: File, onReady: (started: Boolean) -> Unit): Unit = suspendCancellableCoroutine { continuation ->
        release()
        val mp = MediaPlayer()
        player = mp
        continuation.invokeOnCancellation { if (player === mp) release() }
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // Keeps the CPU awake while a verse plays with the screen off.
            appContext?.let { mp.setWakeMode(it, android.os.PowerManager.PARTIAL_WAKE_LOCK) }
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                if (player === it && continuation.isActive) {
                    prepared = true
                    // A pause (or no audio focus) holds the verse ready until Play.
                    val start = !pausedRequested && (appContext?.let(focus::request) ?: true)
                    if (start) it.start() else pausedRequested = true
                    onReady(start)
                }
            }
            mp.setOnCompletionListener { if (player === it && continuation.isActive) continuation.resume(Unit) }
            mp.setOnErrorListener { it, _, _ ->
                if (player === it && continuation.isActive) continuation.resumeWithException(IOException("playback"))
                true
            }
            mp.prepareAsync()
        } catch (error: Exception) {
            if (continuation.isActive) continuation.resumeWithException(IOException("playback", error))
        }
    }

    private fun release() {
        prepared = false
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

/** `cacheDir/quran_ayah/<reciter>/<SSSAAA>.mp3`; Android may reclaim it and it is trimmed to a budget. */
class QuranAyahCache(
    context: Context,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val budgetBytes: Long = 200L * 1024 * 1024
) {
    private val root = File(context.cacheDir, "quran_ayah")

    fun file(voice: QuranAyahReciter, track: QuranAyahTrack): File {
        val (surah, ayah) = if (track.bismillah) 1 to 1 else track.surah to track.ayah
        return File(File(root, voice.id), "${surah.toString().padStart(3, '0')}${ayah.toString().padStart(3, '0')}.mp3")
    }

    fun cached(voice: QuranAyahReciter, track: QuranAyahTrack): File? =
        file(voice, track).takeIf { it.isFile && it.length() > 0 }?.also { it.setLastModified(System.currentTimeMillis()) }

    suspend fun fetch(voice: QuranAyahReciter, track: QuranAyahTrack): File {
        cached(voice, track)?.let { return it }
        val destination = file(voice, track)
        val urls = if (track.bismillah) voice.ayahUrls(1, 1) else voice.ayahUrls(track.surah, track.ayah)
        var failure: IOException = IOException("no source")
        for (url in urls) {
            try {
                download(URL(url), destination)
                trim()
                return destination
            } catch (storage: AudioStorageFullException) {
                throw storage
            } catch (error: IOException) {
                failure = error
            }
        }
        throw failure
    }

    private suspend fun download(url: URL, destination: File) {
        val folder = destination.parentFile!!
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("storage")
        val part = File(folder, "${destination.name}.${UUID.randomUUID()}.part")
        val connection = openConnection(url)
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept-Encoding", "identity")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("http")
            val type = connection.contentType.orEmpty().lowercase()
            if (type.contains("text") || type.contains("html") || type.contains("json")) throw IOException("not audio")
            val length = connection.getHeaderFieldLong("Content-Length", -1)
            if (length > 0 && folder.usableSpace < length + 1_048_576) throw AudioStorageFullException()
            var total = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                    }
                }
            }
            if (total == 0L || (length > 0 && total != length)) throw IOException("truncated")
            if (!part.renameTo(destination)) throw IOException("storage")
        } finally {
            connection.disconnect()
            part.delete()
        }
    }

    /** Deletes the least recently played verses once the cache exceeds its budget. */
    private fun trim() {
        val files = root.walkTopDown().filter { it.isFile && it.extension == "mp3" }.toList()
        var total = files.sumOf { it.length() }
        if (total <= budgetBytes) return
        val target = budgetBytes * 3 / 4
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= target) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}

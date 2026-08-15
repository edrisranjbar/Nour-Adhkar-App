package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.MainActivity
import com.example.R
import com.example.data.local.AdhkarDatabase
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AudioPlaybackState(
    val categoryId: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadPercent: Int? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val completionEventId: Long = 0L
)

class AdhkarPlaybackService : Service(), AudioManager.OnAudioFocusChangeListener {
    inner class LocalBinder : Binder() {
        fun getService(): AdhkarPlaybackService = this@AdhkarPlaybackService
    }

    private data class Track(
        val categoryId: String,
        val title: String,
        val fileName: String,
        val url: String
    )

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private lateinit var audioManager: AudioManager
    private lateinit var mediaSession: MediaSession
    private var mediaPlayer: MediaPlayer? = null
    private var downloadJob: Job? = null
    private var progressJob: Job? = null
    private var activeTrack: Track? = null
    private var isForeground = false
    private val preferences by lazy { PreferenceRepository(applicationContext) }
    private val repository by lazy {
        val database = AdhkarDatabase.getDatabase(applicationContext)
        AdhkarRepository(database.dhikrProgressDao(), database.tasbihSessionDao())
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        mediaSession = MediaSession(this, "NourAdhkarPlayback").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resumePlayback()
                override fun onPause() = pausePlayback()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onStop() = stopPlayback()
            })
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> intent.getStringExtra(EXTRA_CATEGORY_ID)?.let(::playOrToggle)
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safePosition = positionMs.coerceIn(0L, player.duration.toLong().coerceAtLeast(0L))
        player.seekTo(safePosition.toInt())
        publishState(positionMs = safePosition)
    }

    fun consumeCompletionEvent(eventId: Long) {
        if (_state.value.completionEventId == eventId) {
            _state.value = _state.value.copy(completionEventId = 0L)
        }
    }

    private fun playOrToggle(categoryId: String) {
        val track = trackFor(categoryId) ?: return
        if (activeTrack?.categoryId == categoryId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) pausePlayback() else resumePlayback()
            return
        }

        releasePlayer()
        downloadJob?.cancel()
        activeTrack = track
        publishState(
            categoryId = categoryId,
            isLoading = true,
            errorMessage = null,
            positionMs = 0L,
            durationMs = 0L
        )
        ensureForeground()

        val cachedFile = File(File(filesDir, AUDIO_DIRECTORY), track.fileName)
        if (cachedFile.isFile && cachedFile.length() > MIN_VALID_AUDIO_BYTES) {
            prepareAndPlay(cachedFile, track)
        } else {
            downloadJob = serviceScope.launch {
                downloadAndCache(track, cachedFile)
            }
        }
    }

    private suspend fun downloadAndCache(track: Track, destination: File) {
        publishState(isDownloading = true, downloadPercent = null, isLoading = true)
        updateNotification()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                destination.parentFile?.mkdirs()
                val temporary = File(destination.parentFile, "${destination.name}.part")
                if (temporary.exists()) temporary.delete()
                val connection = (URL(track.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "NourAdhkar/1.1.0")
                }
                try {
                    connection.connect()
                    check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                    val totalBytes = connection.contentLengthLong
                    connection.inputStream.buffered().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = 0L
                            var lastPercent = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    val percent = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        _state.value = _state.value.copy(downloadPercent = percent)
                                    }
                                }
                            }
                        }
                    }
                    check(temporary.length() > MIN_VALID_AUDIO_BYTES) { "Downloaded file is incomplete" }
                    if (destination.exists()) destination.delete()
                    check(temporary.renameTo(destination)) { "Could not save audio" }
                } finally {
                    connection.disconnect()
                }
            }
        }

        result.onSuccess {
            publishState(isDownloading = false, downloadPercent = 100)
            prepareAndPlay(destination, track)
        }.onFailure {
            publishState(
                isDownloading = false,
                isLoading = false,
                downloadPercent = null,
                errorMessage = "دریافت فایل صوتی انجام نشد. اتصال اینترنت را بررسی و دوباره تلاش کنید."
            )
            updateNotification()
            stopForegroundSafely()
        }
    }

    private fun prepareAndPlay(file: File, track: Track) {
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setWakeMode(this@AdhkarPlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(file.absolutePath)
            setOnPreparedListener {
                publishState(
                    isLoading = false,
                    isDownloading = false,
                    downloadPercent = null,
                    durationMs = it.duration.toLong(),
                    errorMessage = null
                )
                resumePlayback()
            }
            setOnCompletionListener {
                publishState(isPlaying = false, positionMs = state.value.durationMs)
                updateMediaSession()
                updateNotification()
                stopProgressUpdates()
                serviceScope.launch {
                    val alreadyCompletedToday = preferences.isAdhkarCompletedToday(track.categoryId)
                    repository.completeCategory(
                        categoryId = track.categoryId,
                        recordHistory = !alreadyCompletedToday
                    )
                    preferences.markAdhkarCompletedToday(track.categoryId)
                    publishState(completionEventId = System.nanoTime())
                }
            }
            setOnErrorListener { _, _, _ ->
                publishState(
                    isPlaying = false,
                    isLoading = false,
                    errorMessage = "پخش فایل صوتی با مشکل روبه‌رو شد."
                )
                updateNotification()
                true
            }
            prepareAsync()
        }
        mediaPlayer = player
        activeTrack = track
        updateMediaMetadata(track)
    }

    private fun resumePlayback() {
        val player = mediaPlayer ?: return
        if (!requestAudioFocus()) return
        if (player.currentPosition >= player.duration) player.seekTo(0)
        player.start()
        publishState(isPlaying = true, isLoading = false, errorMessage = null)
        ensureForeground()
        startProgressUpdates()
        updateMediaSession()
        updateNotification()
    }

    private fun pausePlayback() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        publishState(isPlaying = false)
        stopProgressUpdates()
        updateMediaSession()
        updateNotification()
    }

    private fun stopPlayback() {
        releasePlayer()
        downloadJob?.cancel()
        downloadJob = null
        activeTrack = null
        _state.value = AudioPlaybackState()
        mediaSession.isActive = false
        audioManager.abandonAudioFocus(this)
        stopForegroundSafely()
        stopSelf()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let { player ->
                    publishState(
                        positionMs = player.currentPosition.toLong(),
                        durationMs = player.duration.toLong().coerceAtLeast(0L)
                    )
                    updateMediaSession()
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun publishState(
        categoryId: String? = _state.value.categoryId,
        isPlaying: Boolean = _state.value.isPlaying,
        isLoading: Boolean = _state.value.isLoading,
        isDownloading: Boolean = _state.value.isDownloading,
        downloadPercent: Int? = _state.value.downloadPercent,
        positionMs: Long = _state.value.positionMs,
        durationMs: Long = _state.value.durationMs,
        errorMessage: String? = _state.value.errorMessage,
        completionEventId: Long = _state.value.completionEventId
    ) {
        _state.value = AudioPlaybackState(
            categoryId = categoryId,
            isPlaying = isPlaying,
            isLoading = isLoading,
            isDownloading = isDownloading,
            downloadPercent = downloadPercent,
            positionMs = positionMs,
            durationMs = durationMs,
            errorMessage = errorMessage,
            completionEventId = completionEventId
        )
    }

    private fun requestAudioFocus(): Boolean {
        @Suppress("DEPRECATION")
        return audioManager.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.25f, 0.25f)
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1f, 1f)
        }
    }

    private fun updateMediaMetadata(track: Track) {
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "مشاری راشد العفاسی")
                .build()
        )
    }

    private fun updateMediaSession() {
        val current = state.value
        val playbackState = when {
            current.isPlaying -> PlaybackState.STATE_PLAYING
            current.isLoading -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP
                )
                .setState(playbackState, current.positionMs, if (current.isPlaying) 1f else 0f)
                .build()
        )
        mediaSession.isActive = true
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
        } else {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (isForeground) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val current = state.value
        val track = activeTrack
        val toggleAction = if (current.isPlaying) ACTION_PAUSE else ACTION_RESUME
        val toggleIcon = if (current.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleLabel = if (current.isPlaying) "توقف موقت" else "پخش"
        val toggleIntent = PendingIntent.getService(
            this,
            201,
            Intent(this, AdhkarPlaybackService::class.java).setAction(toggleAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            202,
            Intent(this, AdhkarPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            203,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val status = when {
            current.isDownloading -> current.downloadPercent?.let { "در حال دریافت: $it٪" } ?: "در حال دریافت فایل صوتی"
            current.isLoading -> "در حال آماده‌سازی"
            else -> "مشاری راشد العفاسی"
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setContentTitle(track?.title ?: "اذکار نور")
            .setContentText(status)
            .setContentIntent(contentIntent)
            .setOngoing(current.isPlaying || current.isDownloading)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(toggleIcon, toggleLabel, toggleIntent).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "بستن", stopIntent).build())
            .setStyle(Notification.MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0))
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "پخش صوتی اذکار", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun stopForegroundSafely() {
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    private fun releasePlayer() {
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        releasePlayer()
        mediaSession.release()
        audioManager.abandonAudioFocus(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun trackFor(categoryId: String): Track? = when (categoryId) {
        "morning" -> Track(
            categoryId = "morning",
            title = "اذکار الصباح",
            fileName = "morning_adhkar_mishary.mp3",
            url = "https://archive.org/download/makkah-live.-net-athkar-01/MakkahLive.Net_athkar_03.mp3"
        )
        "evening" -> Track(
            categoryId = "evening",
            title = "اذکار المساء",
            fileName = "evening_adhkar_mishary.mp3",
            url = "https://archive.org/download/makkah-live.-net-athkar-01/MakkahLive.Net_athkar_04.mp3"
        )
        else -> null
    }

    companion object {
        const val ACTION_PLAY = "com.example.media.PLAY"
        const val ACTION_PAUSE = "com.example.media.PAUSE"
        const val ACTION_RESUME = "com.example.media.RESUME"
        const val ACTION_STOP = "com.example.media.STOP"
        const val EXTRA_CATEGORY_ID = "category_id"

        private const val CHANNEL_ID = "adhkar_audio_playback"
        private const val NOTIFICATION_ID = 2001
        private const val AUDIO_DIRECTORY = "adhkar_audio"
        private const val MIN_VALID_AUDIO_BYTES = 1_000_000L
    }
}

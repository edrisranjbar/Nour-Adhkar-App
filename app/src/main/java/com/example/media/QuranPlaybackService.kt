package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository
import com.example.notifications.AdhkarNotificationManager
import com.example.quran.QuranRepository
import com.example.ui.language.AppLanguage
import com.example.ui.language.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps a Quran recitation (whole surah or verse by verse) playing after its screen closes: a
 * media-style foreground notification with play/pause (and previous/next verse), a MediaSession
 * for the lock screen and headset buttons, a wake lock while audio plays or loads, and a pause when
 * headphones are unplugged. The players own playback; this service only mirrors their state.
 *
 * The service stays in the foreground for the whole session, paused or playing, so resuming from
 * the notification or after a call never needs a background foreground-service start.
 */
class QuranPlaybackService : Service() {
    // Not `immediate`: a player's stop-then-start (new reciter, next verse) settles before rendering.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: MediaSession
    private var foreground = false
    private var started = false
    private var teardown: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var noisyRegistered = false
    private var surahNames: Map<Int, String> = emptyMap()
    private var pageByVerse: Map<String, Int> = emptyMap()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pauseActive()
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        createChannel()
        session = MediaSession(this, "NourQuranPlayback").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resumeActive()
                override fun onPause() = pauseActive()
                override fun onStop() = stopActive()
                override fun onSkipToNext() = QuranAyahPlayer.next(this@QuranPlaybackService)
                override fun onSkipToPrevious() = QuranAyahPlayer.previous(this@QuranPlaybackService)
            })
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { QuranRepository.load(this@QuranPlaybackService) }.getOrNull()
            }?.let { corpus ->
                surahNames = corpus.surahs.associate { it.number to it.name }
                pageByVerse = corpus.verses.associate { it.id to it.pageNumber }
            }
            render()
        }
        scope.launch {
            combine(QuranAudioPlayer.state, QuranAyahPlayer.state) { _, _ -> }.collect { render() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() requires this promptly, even if playback has just ended.
        enterForeground()
        started = true
        when (intent?.action) {
            ACTION_TOGGLE -> if (nowPlaying()?.isPlaying == true) pauseActive() else resumeActive()
            ACTION_NEXT -> QuranAyahPlayer.next(this)
            ACTION_PREVIOUS -> QuranAyahPlayer.previous(this)
            ACTION_STOP -> stopActive()
        }
        render()
        return START_NOT_STICKY
    }

    private class NowPlaying(
        val verseMode: Boolean,
        val title: String,
        val reciter: String,
        val isPlaying: Boolean,
        val isLoading: Boolean,
        val page: Int?
    )

    private fun nowPlaying(): NowPlaying? {
        val language = PreferenceRepository(this).getAppLanguage()
        val ayah = QuranAyahPlayer.state.value
        val track = ayah.track
        if (track != null && ayah.error == null) {
            val voice = QuranAyahReciters.firstOrNull { it.id == ayah.reciterId }
            val verse = if (track.bismillah) language.text("بسم‌الله") else language.text("آیهٔ ${track.ayah}")
            val surahName = surahNames[track.surah] ?: language.text("سورهٔ ${track.surah}")
            return NowPlaying(
                verseMode = true,
                title = "$surahName · $verse",
                reciter = voice?.let { if (language == AppLanguage.ARABIC) it.arName else it.faName }.orEmpty(),
                isPlaying = ayah.isPlaying,
                isLoading = ayah.isLoading,
                page = pageByVerse["${track.surah}:${track.ayah}"]
            )
        }
        val surah = QuranAudioPlayer.state.value
        val number = surah.surah ?: return null
        val finished = !surah.isPlaying && !surah.isLoading && surah.durationMs > 0 && surah.positionMs >= surah.durationMs
        if (surah.error != null || surah.mobileConfirmationBytes != null || finished) return null
        val voice = QuranReciters.firstOrNull { it.id == surah.reciterId }
        val status = if (surah.isDownloading) {
            language.text("در حال دریافت تلاوت") + (surah.downloadPercent?.let { " · " + language.text("$it٪") } ?: "")
        } else null
        return NowPlaying(
            verseMode = false,
            title = surahNames[number]?.let { "${language.text("سورهٔ $number")} · $it" } ?: language.text("سورهٔ $number"),
            reciter = status ?: voice?.let { if (language == AppLanguage.ARABIC) it.arName else it.faName }.orEmpty(),
            isPlaying = surah.isPlaying,
            isLoading = surah.isLoading,
            page = null
        )
    }

    private fun render() {
        if (!started) return
        val now = nowPlaying()
        if (now == null) {
            // Wait a moment: a stop followed by a new start (next verse, new reciter) is not the end.
            if (teardown?.isActive != true) teardown = scope.launch {
                delay(TEARDOWN_GRACE_MS)
                if (nowPlaying() == null) shutDown()
            }
            return
        }
        teardown?.cancel()
        teardown = null
        updateSession(now)
        notify(buildNotification(now))
        holdWakeLock(now.isPlaying || now.isLoading)
        listenForNoisy(now.isPlaying)
    }

    private fun pauseActive() {
        if (QuranAyahPlayer.state.value.track != null) QuranAyahPlayer.pause() else QuranAudioPlayer.pause()
    }

    private fun resumeActive() {
        if (QuranAyahPlayer.state.value.track != null) QuranAyahPlayer.resume() else QuranAudioPlayer.resume()
    }

    private fun stopActive() {
        QuranAyahPlayer.stop()
        QuranAudioPlayer.stop()
    }

    private fun enterForeground() {
        if (foreground) return
        val notification = buildNotification(
            nowPlaying() ?: NowPlaying(false, PreferenceRepository(this).getAppLanguage().text("پخش قرآن"), "", false, true, null)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foreground = true
    }

    private fun notify(notification: Notification) {
        if (foreground) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun shutDown() {
        holdWakeLock(false)
        listenForNoisy(false)
        session.isActive = false
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        stopSelf()
    }

    private fun updateSession(now: NowPlaying) {
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, now.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, now.reciter)
                .build()
        )
        var actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP
        if (now.verseMode) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    when {
                        now.isPlaying -> PlaybackState.STATE_PLAYING
                        now.isLoading -> PlaybackState.STATE_BUFFERING
                        else -> PlaybackState.STATE_PAUSED
                    },
                    PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    if (now.isPlaying) 1f else 0f
                )
                .build()
        )
        session.isActive = true
    }

    private fun buildNotification(now: NowPlaying): Notification {
        val language = PreferenceRepository(this).getAppLanguage()
        fun action(icon: Int, label: String, action: String, code: Int) = Notification.Action.Builder(
            icon, language.text(label),
            PendingIntent.getService(this, code, Intent(this, QuranPlaybackService::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        ).build()
        val toggle = if (now.isPlaying) {
            action(android.R.drawable.ic_media_pause, "توقف موقت", ACTION_TOGGLE, 301)
        } else {
            action(android.R.drawable.ic_media_play, "پخش", ACTION_TOGGLE, 301)
        }
        val close = action(android.R.drawable.ic_menu_close_clear_cancel, "بستن", ACTION_STOP, 304)
        // Tapping opens the reader at the reciting page, or the app for a surah recitation.
        val open = PendingIntent.getActivity(this, 305,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                now.page?.let { putExtra(AdhkarNotificationManager.EXTRA_OPEN_QURAN_PAGE, it) }
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID)
        else Notification.Builder(this)
        builder
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_nour_adhkar_logo))
            .setColor(0xFFA79277.toInt())
            .setContentTitle(now.title)
            .setContentText(now.reciter)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        val style = Notification.MediaStyle().setMediaSession(session.sessionToken)
        if (now.verseMode) {
            builder.addAction(action(android.R.drawable.ic_media_previous, "آیهٔ قبل", ACTION_PREVIOUS, 302))
            builder.addAction(toggle)
            builder.addAction(action(android.R.drawable.ic_media_next, "آیهٔ بعد", ACTION_NEXT, 303))
            builder.addAction(close)
            style.setShowActionsInCompactView(0, 1, 2)
        } else {
            builder.addAction(toggle)
            builder.addAction(close)
            style.setShowActionsInCompactView(0)
        }
        return builder.setStyle(style).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, PreferenceRepository(this).getAppLanguage().text("پخش قرآن"),
                    NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            )
        }
    }

    /** Verse downloads continue with the screen off; released whenever playback pauses or stops. */
    private fun holdWakeLock(hold: Boolean) {
        if (hold) {
            val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NourAdhkar:QuranPlayback")
                .apply { setReferenceCounted(false) }
                .also { wakeLock = it }
            lock.acquire(WAKE_LOCK_TIMEOUT_MS)
        } else {
            wakeLock?.takeIf { it.isHeld }?.release()
        }
    }

    private fun listenForNoisy(listen: Boolean) {
        if (listen == noisyRegistered) return
        if (listen) {
            ContextCompat.registerReceiver(this, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            runCatching { unregisterReceiver(noisyReceiver) }
        }
        noisyRegistered = listen
    }

    override fun onDestroy() {
        running = false
        holdWakeLock(false)
        listenForNoisy(false)
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "quran_playback"
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_TOGGLE = "com.example.media.quran.TOGGLE"
        private const val ACTION_NEXT = "com.example.media.quran.NEXT"
        private const val ACTION_PREVIOUS = "com.example.media.quran.PREVIOUS"
        private const val ACTION_STOP = "com.example.media.quran.STOP"
        private const val TEARDOWN_GRACE_MS = 800L
        /** Re-acquired on every state change, so this only bounds a stuck session. */
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L

        @Volatile private var running = false

        /** Called by the players when a recitation starts; the service then follows their state. */
        fun start(context: Context) {
            if (running) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, QuranPlaybackService::class.java))
            }
        }
    }
}

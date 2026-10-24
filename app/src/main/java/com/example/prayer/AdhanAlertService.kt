package com.example.prayer
import com.example.ui.language.text

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays the adhan: at prayer time (from [AdhanAlarmReceiver]) and as the settings preview, so the
 * preview sounds exactly like the real adhan (alarm volume, the saved adhan volume, volume keys).
 */
class AdhanAlertService : Service() {
    private var player: MediaPlayer? = null
    private lateinit var audio: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { if (it < 0) stopSelf() }
    private var volumeSession: MediaSession? = null
    private var volumeReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        current = this
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        val preview = intent?.action == ACTION_PREVIEW
        val prayer = AdhanPrayer.entries.firstOrNull { it.name == intent?.getStringExtra("prayer") }
        val prefs = PreferenceRepository(this)
        val language = prefs.getAppLanguage()
        val sound = adhanRecordings.firstOrNull { it.id == prefs.getAdhanSound().id }
        if (sound == null || (!preview && (prayer == null || prayer !in prefs.getAdhanPrayers()))) {
            stopSelf(); return START_NOT_STICKY
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("adhan_playback", language.text("پخش اذان"), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val stop = PendingIntent.getService(this, 6000, Intent(this, AdhanAlertService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 6001, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val title = if (preview || prayer == null) language.text("پیش‌نمایش اذان")
            else language.text("اذان ${prayer.label}") + (prefs.getActivePlaceName()?.let { " · $it" } ?: "")
        startForeground(6000, NotificationCompat.Builder(this, "adhan_playback")
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setContentTitle(title)
            .setContentText(language.text(sound.title)).setContentIntent(open).setOngoing(true)
            .addAction(0, language.text("توقف"), stop).setDeleteIntent(stop).build())
        player?.release()
        player = null
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val focus = if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let(audio::abandonAudioFocusRequest)
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener).build().also {
                    focusRequest = it
                }.let(audio::requestAudioFocus)
        } else audio.requestAudioFocus(focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        if (focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stopSelf(); return START_NOT_STICKY }
        try {
            val instance = MediaPlayer()
            player = instance
            instance.setAudioAttributes(attributes)
            instance.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            resources.openRawResourceFd(sound.resource).use { instance.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            // The saved adhan volume scales the phone's alarm volume (Settings → adhan sound).
            prefs.getAdhanVolume().let { instance.setVolume(it, it) }
            instance.setOnPreparedListener { if (player === it) { it.start(); stopOnVolumeKeys() } }
            instance.setOnCompletionListener { stopSelf() }
            instance.setOnErrorListener { _, _, _ -> stopSelf(); true }
            instance.prepareAsync()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    /**
     * A volume key stops the adhan instead of changing a volume (issue #71). A media session with
     * its own volume control receives the keys while the adhan plays, in the background and on the
     * lock screen. Some devices still route the keys to a stream, so any volume change during the
     * adhan also stops it; the open app also handles the key itself (MainActivity). All of this is
     * removed in [onDestroy], so the keys work normally afterwards; a stopped adhan is not resumed,
     * and later scheduled adhans are unaffected.
     */
    private fun stopOnVolumeKeys() {
        _playing.value = true
        if (volumeSession != null) return
        volumeSession = MediaSession(this, "adhan").apply {
            // Required: the session delivers volume-provider calls through its callback handler,
            // which exists only once a callback is set. Without it the system's "adjust" for a
            // volume key was dropped silently and the adhan kept playing (seen on HyperOS).
            setCallback(object : MediaSession.Callback() {
                override fun onStop() { stopSelf() }
                override fun onPause() { stopSelf() }
            })
            // Any adjustment is a volume key or the volume panel: stop, whatever the direction (a
            // press that only shows the panel can arrive as 0). Verified on HyperOS with the app
            // open, another app in front and the screen locked: the key arrives here as ±1.
            setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_ABSOLUTE, 100, 50) {
                override fun onAdjustVolume(direction: Int) { stopSelf() }
                override fun onSetVolumeTo(volume: Int) { stopSelf() }
            })
            setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .setActions(PlaybackState.ACTION_STOP).build())
            isActive = true
        }
        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { stopSelf() }
        }.also {
            ContextCompat.registerReceiver(this, it, IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
                ContextCompat.RECEIVER_EXPORTED)
        }
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        _playing.value = false
        if (current === this) current = null
        volumeReceiver?.let { runCatching { unregisterReceiver(it) } }
        volumeReceiver = null
        volumeSession?.release()
        volumeSession = null
        player?.release()
        player = null
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let(audio::abandonAudioFocusRequest)
        else audio.abandonAudioFocus(focusListener)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "STOP"
        private const val ACTION_PREVIEW = "PREVIEW"

        private val _playing = MutableStateFlow(false)
        /** Whether an adhan (or its preview) is audible, for the settings play/stop button. */
        val playingState: StateFlow<Boolean> = _playing.asStateFlow()
        /** True while an adhan is audible, so the open app can stop it from its own key handling. */
        val playing: Boolean get() = _playing.value

        @Volatile private var current: AdhanAlertService? = null

        /** Plays the selected adhan now, exactly as at prayer time. */
        fun preview(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AdhanAlertService::class.java).setAction(ACTION_PREVIEW))
        }

        fun stop(context: Context) {
            if (playing) context.startService(Intent(context, AdhanAlertService::class.java).setAction(ACTION_STOP))
        }

        /** Applies a changed adhan volume to an adhan that is playing (the settings slider). */
        fun applyVolume(volume: Float) {
            current?.player?.runCatching { setVolume(volume, volume) }
        }
    }
}

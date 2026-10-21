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
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        val prayer = AdhanPrayer.entries.firstOrNull { it.name == intent?.getStringExtra("prayer") }
        val prefs = PreferenceRepository(this)
        val language = prefs.getAppLanguage()
        val sound = adhanRecordings.firstOrNull { it.id == prefs.getAdhanSound().id }
        if (prayer == null || prayer !in prefs.getAdhanPrayers() || sound == null) {
            stopSelf(); return START_NOT_STICKY
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("adhan_playback", language.text("پخش اذان"), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val stop = PendingIntent.getService(this, 6000, Intent(this, AdhanAlertService::class.java).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 6001, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        startForeground(6000, NotificationCompat.Builder(this, "adhan_playback")
            .setSmallIcon(R.drawable.ic_notification_adhkar)
            .setContentTitle(language.text("اذان ${prayer.label}") + (prefs.getActivePlaceName()?.let { " · $it" } ?: ""))
            .setContentText(language.text(sound.title)).setContentIntent(open).setOngoing(true)
            .addAction(0, language.text("توقف"), stop).setDeleteIntent(stop).build())
        player?.release()
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
            instance.setOnPreparedListener { if (player === it) { it.start(); stopOnVolumeKeys() } }
            instance.setOnCompletionListener { stopSelf() }
            instance.setOnErrorListener { _, _, _ -> stopSelf(); true }
            instance.prepareAsync()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    /**
     * A volume key stops the adhan instead of changing a volume (issue #71). A media session with
     * its own volume control receives the keys while the adhan plays, in the app, in the background
     * and on the lock screen. Some devices still route the keys to a stream, so any volume change
     * during the adhan also stops it. Both are removed in [onDestroy], so the keys work normally
     * afterwards; a stopped adhan is not resumed, and later scheduled adhans are unaffected.
     */
    private fun stopOnVolumeKeys() {
        if (volumeSession != null) return
        playing = true
        volumeSession = MediaSession(this, "adhan").apply {
            setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                override fun onAdjustVolume(direction: Int) { if (direction != 0) stopSelf() }
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
        playing = false
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
        /** True while an adhan is audible, so the open app can stop it from its own key handling. */
        @Volatile var playing = false
            private set

        fun stop(context: Context) {
            if (playing) context.startService(Intent(context, AdhanAlertService::class.java).setAction("STOP"))
        }
    }
}

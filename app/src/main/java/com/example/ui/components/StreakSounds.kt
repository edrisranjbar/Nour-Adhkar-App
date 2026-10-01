package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.R

/** Short celebration effects (synthesised by tools/sfx/streak_sfx.py). Follows the media volume. */
internal class StreakSounds(context: Context) {
    enum class Sfx { IGNITE, TICK, LAND, CHIME }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val ids = HashMap<Sfx, Int>()
    private val loaded = HashSet<Int>()
    private val pending = HashMap<Sfx, Float>()

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status != 0) return@setOnLoadCompleteListener
            synchronized(this) {
                loaded += id
                ids.entries.firstOrNull { it.value == id }?.key?.let { sfx ->
                    pending.remove(sfx)?.let { rate -> pool.play(id, 1f, 1f, 1, 0, rate) }
                }
            }
        }
        ids[Sfx.IGNITE] = pool.load(context, R.raw.sfx_streak_ignite, 1)
        ids[Sfx.TICK] = pool.load(context, R.raw.sfx_streak_tick, 1)
        ids[Sfx.LAND] = pool.load(context, R.raw.sfx_streak_land, 1)
        ids[Sfx.CHIME] = pool.load(context, R.raw.sfx_streak_chime, 1)
    }

    /** Plays now (pitch-shifted by [rate]), or once the clip finishes loading if the dialog opened first. */
    fun play(sfx: Sfx, rate: Float = 1f) {
        synchronized(this) {
            val id = ids[sfx] ?: return
            if (id in loaded) pool.play(id, 1f, 1f, 1, 0, rate) else pending[sfx] = rate
        }
    }

    fun release() = pool.release()
}

@Composable
internal fun rememberStreakSounds(): StreakSounds {
    val context = LocalContext.current.applicationContext
    val sounds = remember { StreakSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    return sounds
}

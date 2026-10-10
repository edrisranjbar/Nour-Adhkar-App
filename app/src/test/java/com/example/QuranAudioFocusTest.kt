package com.example

import android.media.AudioManager
import com.example.media.QuranAudioFocus
import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioFocusTest {
    private val events = mutableListOf<String>()
    private val focus = QuranAudioFocus(
        onPause = { events += "pause" },
        onResume = { events += "resume" },
        onVolume = { events += "volume $it" }
    )

    @Test fun callPausesAndResumesWhenItEnds() {
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(listOf("pause", "volume 1.0", "resume"), events)
    }

    @Test fun userPauseDuringCallIsNotUndone() {
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        focus.userPaused()
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(listOf("pause", "volume 1.0"), events)
    }

    @Test fun anotherPlayerPausesForGood() {
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(listOf("pause", "volume 1.0"), events)
    }

    @Test fun shortSoundsDuckInsteadOfPausing() {
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        focus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(listOf("volume 0.2", "volume 1.0"), events)
    }
}

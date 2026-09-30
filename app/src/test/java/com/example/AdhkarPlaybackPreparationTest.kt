package com.example

import android.media.MediaPlayer
import com.example.media.AdhkarPlaybackService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdhkarPlaybackPreparationTest {
    @Test
    @Config(sdk = [24])
    fun audioNotificationSupportsAndroidSeven() {
        val controller = Robolectric.buildService(AdhkarPlaybackService::class.java).create()
        try {
            val notification = AdhkarPlaybackService::class.java.getDeclaredMethod("buildNotification").apply {
                isAccessible = true
            }.invoke(controller.get())
            assertNotNull(notification)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun controlsDoNotAccessAnUnpreparedPlayer() {
        val controller = Robolectric.buildService(AdhkarPlaybackService::class.java).create()
        val service = controller.get()
        val player = MediaPlayer()
        AdhkarPlaybackService::class.java.getDeclaredField("mediaPlayer").apply {
            isAccessible = true
            set(service, player)
        }
        try {
            service.seekTo(1000)
            listOf("resumePlayback", "pausePlayback").forEach { name ->
                AdhkarPlaybackService::class.java.getDeclaredMethod(name).apply {
                    isAccessible = true
                    invoke(service)
                }
            }
            assertFalse(service.state.value.isPlaying)
        } finally {
            controller.destroy()
        }
    }
}

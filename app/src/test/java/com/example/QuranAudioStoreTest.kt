package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.media.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QuranAudioStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val voice = QuranReciters.first()
    @Before fun clean() { File(context.filesDir, "quran_audio").deleteRecursively() }

    private fun store(bytes: ByteArray, length: Long = bytes.size.toLong(), type: String = "audio/mpeg", code: Int = 200) =
        QuranAudioStore(context, openConnection = { url -> object : HttpURLConnection(url) {
            override fun connect() {}
            override fun disconnect() {}
            override fun usingProxy() = false
            override fun getResponseCode() = code
            override fun getContentType() = type
            override fun getHeaderFieldLong(name: String?, default: Long) = length
            override fun getInputStream() = ByteArrayInputStream(bytes)
        } }, validate = { if (!it.readBytes().contentEquals(bytes)) throw IOException() })

    @Test fun publishesCompleteFileAndSeparatesVoicesAndDeletesPrecisely() = runBlocking {
        val bytes = ByteArray(50000) { 1 }
        val store = store(bytes)
        val progress = mutableListOf<Int?>()
        val result = store.download(voice, 1, { true }, { progress.add(it) })!!
        assertArrayEquals(bytes, result.readBytes())
        assertEquals(100, progress.last())
        assertNull(store.stored(QuranReciters[1].id, 1))
        assertEquals(1, store.list().size)
        store.delete(QuranReciters[1].id)
        assertEquals(1, store.list().size)
        store.delete(voice.id, 1)
        assertTrue(store.list().isEmpty())
    }

    @Test fun rejectsTruncationHtmlHttpErrorsAndCleansPartialFiles() = runBlocking {
        for (store in listOf(store(byteArrayOf(1), 20), store(byteArrayOf(1), type = "text/html"), store(byteArrayOf(1), code = 404))) {
            assertTrue(runCatching { store.download(voice, 1, { true }, {}) }.isFailure)
            assertNull(store.stored(voice.id, 1))
            assertFalse(File(context.filesDir, "quran_audio").walkTopDown().any { it.name.endsWith(".part") })
        }
    }

    @Test fun completedDownloadIsReusedAcrossStoreInstancesWithoutNetwork() = runBlocking {
        val bytes = ByteArray(50000) { 2 }
        val first = store(bytes).download(voice, 1, { true }, {})!!
        val reopened = QuranAudioStore(context, openConnection = { throw AssertionError("Saved audio must not request the network") })
        val reused = reopened.download(voice, 1, { throw AssertionError("No download confirmation needed") }, {})!!
        assertEquals(first.absolutePath, reused.absolutePath)
        assertArrayEquals(bytes, reused.readBytes())
    }

    @Test fun cancellationAndDeclinedMobileDownloadNeverPublishAudio() = runBlocking {
        val store = store(ByteArray(100000))
        assertNull(store.download(voice, 1, { false }, {}))
        val job = launch {
            store.download(voice, 1, { true }, { cancel() })
        }
        job.join()
        assertNull(store.stored(voice.id, 1))
        assertFalse(File(context.filesDir, "quran_audio").walkTopDown().any { it.name.endsWith(".part") })
    }
}

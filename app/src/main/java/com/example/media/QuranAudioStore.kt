package com.example.media

import android.content.Context
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class StoredQuranAudio(val reciterId: String, val surah: Int, val bytes: Long)

/** Only atomically published, validated files count as downloads. Partial files are never listed. */
class QuranAudioStore(
    context: Context,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val validate: (File) -> Unit = ::validateQuranAudio
) {
    companion object { private val mutations = Mutex() }
    private val root = File(context.filesDir, "quran_audio")

    fun file(reciter: String, surah: Int): File {
        require(QuranReciters.any { it.id == reciter } && surah in 1..114)
        return File(File(root, reciter), "${surah.toString().padStart(3, '0')}.mp3")
    }

    fun stored(reciter: String, surah: Int): File? = file(reciter, surah).takeIf { it.isFile && it.length() > 0 }

    fun list(): List<StoredQuranAudio> = QuranReciters.flatMap { reciter ->
        (1..114).mapNotNull { surah -> stored(reciter.id, surah)?.let { StoredQuranAudio(reciter.id, surah, it.length()) } }
    }

    suspend fun delete(reciter: String? = null, surah: Int? = null) = mutations.withLock {
        val target = when {
            reciter == null -> root
            surah == null -> File(root, reciter.also { require(QuranReciters.any { r -> r.id == it }) })
            else -> file(reciter, surah)
        }
        if (target.exists() && !target.deleteRecursively()) throw IOException("delete failed")
    }

    suspend fun download(
        reciter: QuranReciter, surah: Int,
        permitDownload: (Long) -> Boolean,
        progress: (Int?) -> Unit
    ): File? = mutations.withLock {
        // No other store mutation runs concurrently; discard abandoned partials after process death.
        root.walkTopDown().filter { it.isFile && it.name.endsWith(".part") }.forEach { it.delete() }
        val destination = file(reciter.id, surah)
        if (!destination.parentFile!!.isDirectory && !destination.parentFile!!.mkdirs()) throw IOException("storage")
        val part = File(destination.parentFile, "${destination.name}.${UUID.randomUUID()}.part")
        val connection = openConnection(URL(reciter.surahUrl(surah)))
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept-Encoding", "identity")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("http")
            val type = connection.contentType.orEmpty().lowercase()
            if (type.contains("text") || type.contains("html") || type.contains("json")) throw IOException("not audio")
            val length = connection.getHeaderFieldLong("Content-Length", -1)
            currentCoroutineContext().ensureActive()
            if (!permitDownload(length)) return@withLock null
            if (length > 0 && destination.parentFile!!.usableSpace < length + 1_048_576) throw AudioStorageFullException()
            var total = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        progress(if (length > 0) ((total * 100 / length).coerceIn(0, 100)).toInt() else null)
                    }
                    output.fd.sync()
                }
            }
            currentCoroutineContext().ensureActive()
            if (total == 0L || (length > 0 && total != length)) throw IOException("truncated")
            // Reject HTML disguised as audio and files Android cannot decode before publishing them.
            validate(part)
            currentCoroutineContext().ensureActive()
            if (!part.renameTo(destination)) throw IOException("storage")
            destination
        } finally {
            connection.disconnect()
            part.delete()
        }
    }
}

private fun validateQuranAudio(file: File) {
    val metadata = MediaMetadataRetriever()
    try {
        metadata.setDataSource(file.absolutePath)
        if ((metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) <= 0) {
            throw IOException("invalid audio")
        }
    } finally { metadata.release() }
}

class AudioStorageFullException : IOException("storage full")

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
import java.security.MessageDigest

/** Persistent app storage: only complete, decodable audio is published for reuse. */
class LectureAudioStore(context: Context) {
    companion object { private val downloads = Mutex() }
    private val root = File(context.filesDir, "lecture_audio")

    private fun file(lectureId: String, url: String): File {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("$lectureId\n$url".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(root, "$key.audio")
    }

    suspend fun getOrDownload(lectureId: String, url: String, progress: (Int?) -> Unit): File = downloads.withLock {
        val destination = file(lectureId, url)
        if (destination.isFile && destination.length() > 0) return@withLock destination
        if (!root.isDirectory && !root.mkdirs()) throw IOException("storage")
        // Previous process termination can leave a partial file; it is never reused.
        val part = File(root, "${destination.name}.part")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept-Encoding", "identity")
        try {
            progress(null)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("http")
            val type = connection.contentType.orEmpty().lowercase()
            if (type.contains("text") || type.contains("html") || type.contains("json")) throw IOException("not audio")
            val length = connection.getHeaderFieldLong("Content-Length", -1)
            if (length > 0 && root.usableSpace < length + 1_048_576) throw AudioStorageFullException()
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
                        progress(if (length > 0) (total * 100 / length).coerceIn(0, 100).toInt() else null)
                    }
                    output.fd.sync()
                }
            }
            currentCoroutineContext().ensureActive()
            if (total == 0L || (length > 0 && total != length)) throw IOException("truncated")
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(part.absolutePath)
                if ((metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) <= 0) {
                    throw IOException("invalid audio")
                }
            } finally { metadata.release() }
            currentCoroutineContext().ensureActive()
            if (!part.renameTo(destination)) throw IOException("storage")
            destination
        } finally {
            connection.disconnect()
            part.delete()
        }
    }
}

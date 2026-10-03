package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class ProgressSyncHttpException(val status: Int) : IOException("Progress sync HTTP $status")
internal data class ProgressSyncResponse(val token: String, val records: JSONArray)

/** A rejected session is refreshed once; uploads are retried with the identical journal. */
internal class ProgressSyncApi(
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    suspend fun sync(token: String, records: JSONArray, refresh: suspend (String) -> String?): ProgressSyncResponse {
        try {
            return ProgressSyncResponse(token, request(token, records))
        } catch (failure: ProgressSyncHttpException) {
            if (failure.status != 401) throw failure
        }
        val refreshed = refresh(token) ?: throw ProgressSyncHttpException(401)
        return ProgressSyncResponse(refreshed, request(refreshed, records))
    }

    private fun request(token: String, records: JSONArray): JSONArray {
        val connection = openConnection(URL("https://api.adhkar.ir/api/progress/sync"))
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val payload = JSONObject().put("version", 1).put("records", records).toString().toByteArray(Charsets.UTF_8)
            if (payload.size > 4_000_000) throw ProgressSyncHttpException(413)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status !in 200..299) throw ProgressSyncHttpException(status)
            val body = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 4_000_000) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                if (bytes.size > 4_000_000) throw ProgressSyncHttpException(413)
                String(bytes, Charsets.UTF_8)
            }
            return JSONObject(body).getJSONArray("records")
        } finally { connection.disconnect() }
    }
}

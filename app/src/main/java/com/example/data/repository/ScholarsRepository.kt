package com.example.data.repository

import android.content.Context
import com.example.data.model.Lecture
import com.example.data.model.Scholar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Scholars and lectures managed from the admin panel: `GET /api/scholars`, cached for offline use.
 * Falls back to the bundled `assets/scholars.json` when nothing has been fetched yet (see docs/scholars.md).
 */
object ScholarsRepository {
    private const val URL_SCHOLARS = "https://api.adhkar.ir/api/scholars"
    private const val PREFS = "scholars"
    private const val KEY_CACHE = "scholars_json"

    /** Last fetched list, else the bundled one. Never throws. */
    fun cached(context: Context): List<Scholar> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CACHE, null)
            ?.let { runCatching { parse(JSONArray(it)) }.getOrNull() }
            ?: bundled(context)

    /** Fetches the admin-managed list and caches it. Returns null on any failure so the caller keeps what it has. */
    suspend fun refresh(context: Context): List<Scholar>? = withContext(Dispatchers.IO) {
        val connection = (URL(URL_SCHOLARS).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(body).getJSONArray("data")
            val parsed = parse(data)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CACHE, data.toString()).apply()
            parsed
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** The reviewed transcript of lecture [serverId], or null when it cannot be loaded. */
    suspend fun transcript(serverId: String): String? = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.adhkar.ir/api/lectures/$serverId/transcript").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).getJSONObject("data").optString("transcript").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun bundled(context: Context): List<Scholar> = runCatching {
        val root = context.assets.open("scholars.json").bufferedReader().use { JSONObject(it.readText()) }
        parse(root.getJSONArray("scholars"))
    }.getOrDefault(emptyList())

    private fun parse(scholars: JSONArray): List<Scholar> = (0 until scholars.length()).map { i ->
        val s = scholars.getJSONObject(i)
        val id = s.get("id").toString()
        val lectures = s.optJSONArray("lectures")
        Scholar(
            id = id,
            name = s.getString("name"),
            tagline = s.optString("tagline"),
            bio = s.optString("bio"),
            hue = s.optDouble("hue", 150.0).toFloat(),
            photoUrl = s.optString("photoUrl").takeIf { it.startsWith("http") },
            lectures = (0 until (lectures?.length() ?: 0)).map { j ->
                val l = lectures!!.getJSONObject(j)
                Lecture(
                    id = "$id-${l.opt("id") ?: j}",
                    title = l.getString("title"),
                    description = l.optString("description").takeIf { it != "null" }.orEmpty(),
                    audioUrl = l.getString("audioUrl"),
                    durationSec = l.optInt("durationSec", -1).takeIf { it > 0 },
                    summary = l.optString("summary").takeIf { it != "null" }.orEmpty(),
                    transcriptId = l.opt("id")?.toString()?.takeIf { l.optBoolean("hasTranscript") }
                )
            }
        )
    }
}

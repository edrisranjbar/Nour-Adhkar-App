package com.example.data.repository

import android.content.Context
import com.example.data.model.ADMIN_DHIKR_ID_OFFSET
import com.example.data.model.AdhkarData
import com.example.data.model.AdminCollection
import com.example.data.model.DhikrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Adhkar published from the admin panel («اذکار برنامه»), from `GET /api/app-adhkar`, shown next to
 * the built-in adhkar ([AdhkarData]).
 *
 * The app never waits for the network: the last response is kept on disk and applied at start
 * ([loadCached]), and [refresh] runs in the background at most once every [REFRESH_MILLIS], using
 * the ETag so an unchanged list costs a tiny 304. Failures keep the cached content.
 */
object AppAdhkarRepository {
    private const val URL_ADHKAR = "https://api.adhkar.ir/api/app-adhkar"
    private const val PREFS = "app_adhkar"
    private const val KEY_CACHE = "collections_json"
    private const val KEY_ETAG = "etag"
    private const val KEY_CHECKED_AT = "checked_at"
    private const val REFRESH_MILLIS = 6 * 60 * 60_000L

    @Volatile private var loaded = false

    /** Applies the cached admin adhkar once per process; cheap after the first call. */
    fun loadCached(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            prefs(context).getString(KEY_CACHE, null)
                ?.let { runCatching { parse(JSONArray(it)) }.getOrNull() }
                ?.let(AdhkarData::applyAdminCollections)
            loaded = true
        }
    }

    /** Background refresh at app start; never throws and skips the network when checked recently. */
    suspend fun refresh(context: Context): Unit = withContext(Dispatchers.IO) {
        loadCached(context)
        val prefs = prefs(context)
        if (System.currentTimeMillis() - prefs.getLong(KEY_CHECKED_AT, 0L) < REFRESH_MILLIS) return@withContext
        runCatching<Unit> {
            val connection = (URL(URL_ADHKAR).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/json")
                if (prefs.contains(KEY_CACHE)) prefs.getString(KEY_ETAG, null)?.let { setRequestProperty("If-None-Match", it) }
            }
            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> prefs.edit().putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply()
                    in 200..299 -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val data = JSONObject(body).getJSONArray("data")
                        val collections = parse(data)
                        prefs.edit()
                            .putString(KEY_CACHE, data.toString())
                            .putString(KEY_ETAG, connection.getHeaderField("ETag"))
                            .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                            .apply()
                        AdhkarData.applyAdminCollections(collections)
                    }
                    else -> Unit // keep the cache; retried on a later start
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Skips malformed entries instead of failing the whole list. */
    internal fun parse(data: JSONArray): List<AdminCollection> = (0 until data.length()).mapNotNull { i ->
        val collection = data.optJSONObject(i) ?: return@mapNotNull null
        val items = collection.optJSONArray("items") ?: JSONArray()
        AdminCollection(
            id = collection.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null,
            builtInKey = collection.text("key").ifBlank { null },
            title = collection.text("title"),
            description = collection.text("description"),
            iconName = collection.text("icon").ifBlank { "menu_book" },
            items = (0 until items.length()).mapNotNull { j ->
                val item = items.optJSONObject(j) ?: return@mapNotNull null
                val id = item.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
                val arabic = item.text("arabic").ifBlank { return@mapNotNull null }
                DhikrItem(
                    id = ADMIN_DHIKR_ID_OFFSET + id,
                    arabicText = arabic,
                    persianTranslation = item.text("translation"),
                    targetCount = item.optInt("count", 1).coerceIn(1, 10_000),
                    source = item.text("source")
                )
            }
        ).takeIf { it.builtInKey != null || it.title.isNotBlank() }
    }

    private fun JSONObject.text(name: String): String =
        if (isNull(name)) "" else optString(name).trim()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

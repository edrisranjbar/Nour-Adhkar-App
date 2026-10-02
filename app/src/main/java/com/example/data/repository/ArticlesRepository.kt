package com.example.data.repository

import android.content.Context
import androidx.core.text.HtmlCompat
import com.example.data.model.ArticleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Articles managed in the admin panel («مقالات»), from `GET /api/app-articles`.
 *
 * Built to open instantly: the last response is cached on disk and kept parsed in memory, the cache
 * is warmed in the background when the app starts ([prefetch]), and refreshes are conditional
 * (ETag), so an unchanged list costs a tiny 304 and no re-parsing.
 */
object ArticlesRepository {
    private const val URL_ARTICLES = "https://api.adhkar.ir/api/app-articles"
    private const val PREFS = "articles"
    private const val KEY_CACHE = "articles_json"
    private const val KEY_LEGACY_CACHE = "posts_json"
    private const val KEY_ETAG = "articles_etag"
    private const val KEY_CHECKED_AT = "articles_checked_at"
    /** A list checked this recently is shown as is, without contacting the server again. */
    private const val FRESH_MILLIS = 10 * 60_000L

    @Volatile private var memory: List<ArticleItem>? = null

    /** Cached articles (memory first, then disk), or empty before the first successful load. */
    fun cached(context: Context): List<ArticleItem> {
        memory?.let { return it }
        val prefs = prefs(context)
        val json = prefs.getString(KEY_CACHE, null) ?: prefs.getString(KEY_LEGACY_CACHE, null)
        return (json?.let { runCatching { parse(JSONArray(it)) }.getOrNull() }.orEmpty()).also { if (it.isNotEmpty()) memory = it }
    }

    /** Warms the cache in the background at app start; failures are ignored (the screen retries). */
    suspend fun prefetch(context: Context) {
        runCatching { refresh(context) }
    }

    /**
     * Returns up-to-date articles. Skips the network when the list was checked within
     * [FRESH_MILLIS] unless [force]. Throws [ArticlesException] with a friendly message.
     */
    suspend fun refresh(context: Context, force: Boolean = false): List<ArticleItem> = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val current = cached(context)
        val checkedAt = prefs.getLong(KEY_CHECKED_AT, 0L)
        if (!force && current.isNotEmpty() && System.currentTimeMillis() - checkedAt < FRESH_MILLIS) return@withContext current

        val connection = (URL(URL_ARTICLES).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            if (current.isNotEmpty()) prefs.getString(KEY_ETAG, null)?.let { setRequestProperty("If-None-Match", it) }
        }
        try {
            val code = try { connection.responseCode } catch (e: IOException) {
                throw ArticlesException("اتصال به اینترنت برقرار نشد. لطفاً اتصال را بررسی کنید.")
            }
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                prefs.edit().putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply()
                return@withContext current
            }
            if (code !in 200..299) throw ArticlesException("دریافت مقالات از سرور ممکن نشد. لطفاً بعداً دوباره تلاش کنید.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val data = runCatching { JSONObject(body).getJSONArray("data") }.getOrElse {
                throw ArticlesException("پاسخ سرور قابل خواندن نبود. لطفاً بعداً دوباره تلاش کنید.")
            }
            val parsed = parse(data)
            prefs.edit()
                .putString(KEY_CACHE, data.toString())
                .putString(KEY_ETAG, connection.getHeaderField("ETag"))
                .putLong(KEY_CHECKED_AT, System.currentTimeMillis())
                .remove(KEY_LEGACY_CACHE)
                .apply()
            memory = parsed
            parsed
        } catch (e: IOException) {
            throw ArticlesException("اتصال به اینترنت برقرار نشد. لطفاً اتصال را بررسی کنید.")
        } finally {
            connection.disconnect()
        }
    }

    /** Reads both the app-articles shape and the older /api/posts cache. */
    private fun parse(data: JSONArray): List<ArticleItem> = (0 until data.length()).map { i ->
        val post = data.getJSONObject(i)
        val content = post.optString("content").asPlainText()
        ArticleItem(
            id = post.optString("slug").takeIf { it.isNotBlank() && it != "null" } ?: post.optString("id"),
            title = post.optString("title").asPlainText(),
            summary = post.optString("excerpt").takeIf { it.isNotBlank() && it != "null" }?.asPlainText()
                ?: content.take(140),
            content = content,
            readTime = readTime(content),
            author = post.optJSONObject("user")?.optString("name")?.takeIf { it.isNotBlank() } ?: "اذکار نور"
        )
    }

    /** Older posts may be rich HTML; the app shows plain text with paragraph breaks. */
    private fun String.asPlainText(): String =
        if ('<' in this && '>' in this) HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        else trim()

    private fun readTime(content: String): String {
        val words = content.split(Regex("\\s+")).count { it.isNotBlank() }
        val minutes = (words / 180).coerceAtLeast(1)
        return "$minutes دقیقه مطالعه"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

class ArticlesException(message: String) : Exception(message)

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

/** Published articles from `GET /api/posts`, with the last good response cached for offline reading. */
object ArticlesRepository {
    private const val URL_POSTS = "https://api.adhkar.ir/api/posts"
    private const val PREFS = "articles"
    private const val KEY_CACHE = "posts_json"

    /** Articles saved from the last successful fetch, or empty. */
    fun cached(context: Context): List<ArticleItem> =
        prefs(context).getString(KEY_CACHE, null)?.let { runCatching { parse(JSONArray(it)) }.getOrNull() }.orEmpty()

    /** Fetches fresh articles and updates the cache. Throws [ArticlesException] with a friendly message. */
    suspend fun refresh(context: Context): List<ArticleItem> = withContext(Dispatchers.IO) {
        val connection = (URL(URL_POSTS).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = try { connection.responseCode } catch (e: IOException) {
                throw ArticlesException("اتصال به اینترنت برقرار نشد. لطفاً اتصال را بررسی کنید.")
            }
            if (code !in 200..299) throw ArticlesException("دریافت مقالات از سرور ممکن نشد. لطفاً بعداً دوباره تلاش کنید.")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val data = runCatching { JSONObject(body).getJSONArray("data") }.getOrElse {
                throw ArticlesException("پاسخ سرور قابل خواندن نبود. لطفاً بعداً دوباره تلاش کنید.")
            }
            prefs(context).edit().putString(KEY_CACHE, data.toString()).apply()
            parse(data)
        } catch (e: IOException) {
            throw ArticlesException("اتصال به اینترنت برقرار نشد. لطفاً اتصال را بررسی کنید.")
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(data: JSONArray): List<ArticleItem> = (0 until data.length()).map { i ->
        val post = data.getJSONObject(i)
        val content = post.optString("content").asPlainText()
        ArticleItem(
            id = post.optString("slug").ifBlank { post.optString("id") },
            title = post.optString("title").asPlainText(),
            summary = post.optString("excerpt").takeIf { it.isNotBlank() && it != "null" }?.asPlainText()
                ?: content.take(140),
            content = content,
            readTime = readTime(content),
            author = post.optJSONObject("user")?.optString("name")?.takeIf { it.isNotBlank() } ?: "اذکار نور"
        )
    }

    /** Admin posts may be rich HTML; the app shows plain text with paragraph breaks. */
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

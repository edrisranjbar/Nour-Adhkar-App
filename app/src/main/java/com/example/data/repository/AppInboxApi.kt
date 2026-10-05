package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class AppNotice(val id: Long, val title: String, val message: String, val date: String, val read: Boolean)

/** One of the signed-in user's own feedback messages, with the team's like and reply. */
data class MyFeedback(
    val id: Long,
    val type: String,
    val message: String,
    val reply: String?,
    val repliedAt: String?,
    val liked: Boolean,
    val createdAt: String
) {
    /** Changes when the reply is added or edited, so an edited reply counts as new again. */
    val replyKey: String? get() = repliedAt?.let { "$id@$it" }
}

class ServerException(val code: Int) : Exception("HTTP $code")

object AppInboxApi {
    private val _unreadCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Unread published notices for this installation; updated whenever notices are loaded or read. */
    val unreadCount: kotlinx.coroutines.flow.StateFlow<Int> = _unreadCount

    suspend fun refreshUnreadCount(context: Context) {
        runCatching { notices(context) }
    }

    /** User-facing reason: server faults are not blamed on the user's connection. */
    fun describe(e: Exception): String = when (e) {
        is ServerException -> "سرور پیام‌ها در حال حاضر پاسخ نمی‌دهد (خطای ${e.code}). بعداً دوباره تلاش کنید."
        is java.io.IOException -> "اتصال به سرور برقرار نشد. اینترنت را بررسی کنید."
        else -> "دریافت پیام‌ها ممکن نشد."
    }

    private const val base = "https://api.adhkar.ir/api"

    fun installationId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences("app_inbox", Context.MODE_PRIVATE)
        val existing = prefs.getString("installation_id", null)
        if (existing != null) return existing
        return UUID.randomUUID().toString().also { prefs.edit().putString("installation_id", it).apply() }
    }

    private suspend fun request(path: String, method: String = "GET", body: JSONObject? = null, token: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL("$base/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            if (connection.responseCode !in 200..299) throw ServerException(connection.responseCode)
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    /** Sends feedback as the signed-in user, so the admin panel shows who wrote it. */
    suspend fun sendFeedback(context: Context, type: String, message: String) {
        request("app-feedback", "POST", JSONObject().put("type", type).put("message", message),
            token = AccountRepository.token(context))
    }

    private val _newReplies = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Team replies to the user's feedback that they have not opened yet. */
    val newReplies: kotlinx.coroutines.flow.StateFlow<Int> = _newReplies

    /** The signed-in user's feedback, newest first. Requires sign-in. */
    suspend fun myFeedback(context: Context): List<MyFeedback> {
        val token = AccountRepository.token(context) ?: return emptyList()
        val array = request("app-feedback/mine", token = token).getJSONArray("data")
        fun org.json.JSONObject.text(key: String) = optString(key).takeIf { !isNull(key) && it.isNotBlank() && it != "null" }
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            MyFeedback(
                id = item.getLong("id"), type = item.optString("type"), message = item.optString("message"),
                reply = item.text("reply"), repliedAt = item.text("repliedAt"),
                liked = item.optBoolean("liked"), createdAt = item.optString("createdAt")
            )
        }.also { list -> _newReplies.value = list.count { it.replyKey != null && it.replyKey !in seenReplies(context) } }
    }

    /** Marks every currently shown reply as seen. */
    fun markRepliesSeen(context: Context, items: List<MyFeedback>) {
        val keys = items.mapNotNull { it.replyKey }.toSet()
        context.applicationContext.getSharedPreferences("app_inbox", Context.MODE_PRIVATE).edit()
            .putStringSet("seen_feedback_replies", keys + seenReplies(context)).apply()
        _newReplies.value = 0
    }

    fun seenReplies(context: Context): Set<String> =
        context.applicationContext.getSharedPreferences("app_inbox", Context.MODE_PRIVATE)
            .getStringSet("seen_feedback_replies", emptySet()).orEmpty()

    suspend fun notices(context: Context): List<AppNotice> {
        val id = installationId(context)
        val array = request("app-notices?installation_id=$id").getJSONArray("data")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            AppNotice(item.getLong("id"), item.getString("title"), item.getString("message"), item.getString("created_at"), !item.isNull("read_at"))
        }.also { list -> _unreadCount.value = list.count { !it.read } }
    }

    suspend fun markRead(context: Context, noticeId: Long) {
        request("app-notices/$noticeId/read", "POST", JSONObject().put("installation_id", installationId(context)))
        _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
    }
}

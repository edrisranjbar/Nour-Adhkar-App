package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AccountUser(val name: String, val email: String)

class AuthException(message: String) : Exception(message)

/** Result of login/register: signed in, or a 5-digit code was emailed and must be confirmed. */
sealed interface AuthResult {
    data class SignedIn(val user: AccountUser) : AuthResult
    data class VerificationRequired(val email: String, val retryAfterSeconds: Int) : AuthResult
}

/**
 * Optional account sign-in against the Nour Adhkar API. The app stays fully usable
 * without an account; this only stores the JWT and basic profile locally.
 * Requests carry `X-Nour-Client: android`, which makes the API require email verification.
 */
object AccountRepository {
    private const val base = "https://api.adhkar.ir/api"
    private const val PREFS = "account"

    private val _user = MutableStateFlow<AccountUser?>(null)
    val user: StateFlow<AccountUser?> = _user.asStateFlow()
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = prefs(context)
        val email = prefs.getString("email", null)
        if (prefs.getString("token", null) != null && email != null) {
            _user.value = AccountUser(prefs.getString("name", "").orEmpty(), email)
        }
    }

    suspend fun login(context: Context, email: String, password: String) = authenticate(
        context, "auth/login", JSONObject().put("email", email.trim()).put("password", password)
    )

    suspend fun register(context: Context, name: String, email: String, password: String) = authenticate(
        context, "auth/register",
        JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password)
    )

    /** Google accounts are already email-verified, so the API signs in (or creates) the user directly. */
    suspend fun loginWithGoogle(context: Context, idToken: String) = authenticate(
        context, "auth/google", JSONObject().put("id_token", idToken)
    )

    /** Confirms the emailed code; the password is re-sent so a code alone never grants access. */
    suspend fun verifyEmail(context: Context, email: String, password: String, code: String): AuthResult =
        authenticate(
            context, "auth/verify-email",
            JSONObject().put("email", email.trim()).put("password", password).put("code", code)
        )

    /** Emails a 5-digit password reset code. Returns seconds to wait before another request. */
    suspend fun requestPasswordReset(email: String): Int {
        val (_, json) = request(
            "auth/password-reset/request", JSONObject().put("email", email.trim()), null, allowStatus = setOf(429)
        )
        return json.optInt("retry_after", 60)
    }

    /** Sets a new password with the emailed code and signs in. */
    suspend fun confirmPasswordReset(context: Context, email: String, code: String, newPassword: String): AuthResult =
        authenticate(
            context, "auth/password-reset/confirm",
            JSONObject().put("email", email.trim()).put("code", code).put("password", newPassword)
        )

    /** Requests a new code. Returns seconds to wait before the next request. */
    suspend fun resendCode(email: String): Int {
        val (_, json) = request("auth/resend-code", JSONObject().put("email", email.trim()), null, allowStatus = setOf(429))
        return json.optInt("retry_after", 60)
    }

    /** Signs out locally right away; revoking the token on the server happens in the background. */
    fun logout(context: Context) {
        val token = prefs(context).getString("token", null)
        prefs(context).edit().clear().apply()
        _user.value = null
        if (token != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { request("auth/logout", JSONObject(), token) }
            }
        }
    }

    private suspend fun authenticate(context: Context, path: String, body: JSONObject): AuthResult {
        val (_, response) = request(path, body, null, allowStatus = setOf(403))
        if (response.optBoolean("verification_required")) {
            return AuthResult.VerificationRequired(
                email = response.optString("email", body.optString("email")),
                retryAfterSeconds = response.optInt("retry_after", 60)
            )
        }
        val token = response.optString("token").takeIf { it.isNotBlank() }
            ?: throw AuthException(friendlyMessage(200, response.optString("message")))
        val userJson = response.optJSONObject("user") ?: JSONObject()
        val user = AccountUser(userJson.optString("name"), userJson.optString("email"))
        prefs(context).edit()
            .putString("token", token)
            .putString("name", user.name)
            .putString("email", user.email)
            .apply()
        _user.value = user
        return AuthResult.SignedIn(user)
    }

    /**
     * POSTs JSON. Non-2xx responses throw [AuthException] with the server message, except
     * statuses in [allowStatus], whose body is returned for the caller to interpret
     * (403 = verification required on login; 429 = resend cooldown).
     */
    private suspend fun request(
        path: String,
        body: JSONObject,
        token: String?,
        allowStatus: Set<Int> = emptySet()
    ): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val connection = try {
            (URL("$base/$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Nour-Client", "android")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        } catch (e: Exception) {
            throw AuthException("اتصال به سرور برقرار نشد. اینترنت را بررسی کنید.")
        }
        try {
            val code = try { connection.responseCode } catch (e: Exception) {
                throw AuthException("اتصال به سرور برقرار نشد. اینترنت را بررسی کنید.")
            }
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            if (code !in 200..299 && code !in allowStatus) {
                throw AuthException(friendlyMessage(code, json.optString("message")))
            }
            code to json
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Shows the server's message only when it is a real Persian/Arabic sentence; raw framework
     * text such as "Server Error" or "Too Many Attempts." is replaced by a friendly message.
     */
    internal fun friendlyMessage(status: Int, serverMessage: String?): String {
        val message = serverMessage?.trim().orEmpty()
        // 503 carries a specific Persian reason from our API (e.g. Google sign-in temporarily unavailable).
        if ((status in 400..499 && status != 429 || status == 503) && isPersianSentence(message)) return message
        return when (status) {
            401 -> "ایمیل یا رمز عبور نادرست است."
            403 -> "دسترسی به این حساب ممکن نیست. لطفاً با پشتیبانی تماس بگیرید."
            404 -> "سرویس حساب کاربری در دسترس نیست. لطفاً بعداً تلاش کنید."
            409 -> "این ایمیل قبلاً تأیید شده است. با رمز عبور وارد شوید."
            422 -> "اطلاعات واردشده درست نیست. لطفاً دوباره بررسی کنید."
            429 -> "تعداد تلاش‌ها زیاد بود. چند دقیقه صبر کنید و دوباره امتحان کنید."
            503 -> "این روش ورود در حال حاضر روی سرور فعال نیست. لطفاً با ایمیل وارد شوید."
            in 500..599 ->"مشکلی در سرور پیش آمده است. لطفاً چند دقیقه دیگر دوباره تلاش کنید."
            else -> "ورود انجام نشد. لطفاً دوباره تلاش کنید."
        }
    }

    /**
     * True only for genuine Persian/Arabic text: no Latin letters at all (rejects mixed framework
     * text like "Server Error: خطا") and at least a few Arabic-script letters (rejects a stray
     * character or punctuation). Digits, spaces and punctuation are ignored.
     */
    internal fun isPersianSentence(message: String): Boolean {
        val letters = message.filter(Char::isLetter)
        if (letters.any { it in 'A'..'Z' || it in 'a'..'z' }) return false
        val arabicLetters = letters.count(::isArabicScriptLetter)
        return arabicLetters >= 3 && arabicLetters == letters.length
    }

    private fun isArabicScriptLetter(c: Char): Boolean =
        c in '؀'..'ۿ' || c in 'ݐ'..'ݿ' || c in 'ﭐ'..'﷿' || c in 'ﹰ'..'﻿'

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

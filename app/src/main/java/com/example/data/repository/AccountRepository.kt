package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** Confirms the emailed code; the password is re-sent so a code alone never grants access. */
    suspend fun verifyEmail(context: Context, email: String, password: String, code: String): AuthResult =
        authenticate(
            context, "auth/verify-email",
            JSONObject().put("email", email.trim()).put("password", password).put("code", code)
        )

    /** Requests a new code. Returns seconds to wait before the next request. */
    suspend fun resendCode(email: String): Int {
        val (_, json) = request("auth/resend-code", JSONObject().put("email", email.trim()), null, allowStatus = setOf(429))
        return json.optInt("retry_after", 60)
    }

    suspend fun logout(context: Context) {
        val token = prefs(context).getString("token", null)
        prefs(context).edit().clear().apply()
        _user.value = null
        if (token != null) runCatching { request("auth/logout", JSONObject(), token) }
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
            ?: throw AuthException(response.optString("message", "ورود ناموفق بود"))
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
                throw AuthException(json.optString("message").ifBlank { "خطای سرور ($code)" })
            }
            code to json
        } finally {
            connection.disconnect()
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

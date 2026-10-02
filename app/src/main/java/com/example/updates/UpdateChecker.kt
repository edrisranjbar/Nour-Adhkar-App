package com.example.updates

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val versionCode: Int,
    val isRequired: Boolean = false
)

object UpdateChecker {
    // Managed from the admin panel («نسخه‌های برنامه»).
    private const val VERSION_URL = "https://api.adhkar.ir/api/app-version"
    // Used only when the server cannot be reached or has no published version yet.
    private const val FALLBACK_VERSION_URL =
        "https://raw.githubusercontent.com/edrisranjbar/Nour-Adhkar-App/main/version.json"

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        val json = fetch(VERSION_URL) ?: fetch(FALLBACK_VERSION_URL) ?: return@withContext null
        runCatching {
            val versionCode = json.getInt("versionCode")
            val isRequired = BuildConfig.VERSION_CODE < json.optInt("minRequiredVersionCode", 0)
            AppUpdate(
                versionName = json.getString("versionName"),
                versionCode = versionCode,
                isRequired = isRequired
            ).takeIf { versionCode > BuildConfig.VERSION_CODE || isRequired }
        }.getOrNull()
    }

    private fun fetch(url: String): JSONObject? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

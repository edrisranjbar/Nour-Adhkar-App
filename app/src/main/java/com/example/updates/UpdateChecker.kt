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
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/edrisranjbar/Nour-Adhkar-App/main/version.json"

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(VERSION_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.useCaches = false
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val versionCode = json.getInt("versionCode")
            val isRequired = BuildConfig.VERSION_CODE < json.optInt("minRequiredVersionCode", 0)
            AppUpdate(
                versionName = json.getString("versionName"),
                versionCode = versionCode,
                isRequired = isRequired
            ).takeIf { versionCode > BuildConfig.VERSION_CODE || isRequired }
        }.getOrNull()
    }
}

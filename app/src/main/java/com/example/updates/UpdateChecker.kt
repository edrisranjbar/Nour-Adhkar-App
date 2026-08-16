package com.example.updates

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val versionName: String, val versionCode: Int)

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
            AppUpdate(
                versionName = json.getString("versionName"),
                versionCode = json.getInt("versionCode")
            ).takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }.getOrNull()
    }
}

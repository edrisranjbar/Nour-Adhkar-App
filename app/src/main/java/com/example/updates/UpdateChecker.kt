package com.example.updates

import com.example.BuildConfig
import com.example.store.StoreConfig
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
    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        // Each flavor owns its publication feed; never fall back to a competing store's version.
        val json = fetch(StoreConfig.UPDATE_URL)
            ?: StoreConfig.FALLBACK_UPDATE_URL.takeIf { it.isNotBlank() }?.let(::fetch)
            ?: return@withContext null
        parse(json)
    }

    internal fun parse(json: JSONObject, currentVersionCode: Int = BuildConfig.VERSION_CODE): AppUpdate? =
        runCatching {
            val channel = json.optString("store")
            if (channel != StoreConfig.CHANNEL &&
                !(channel.isBlank() && StoreConfig.ACCEPT_LEGACY_METADATA)) return@runCatching null
            if (!json.optBoolean("published", StoreConfig.ACCEPT_LEGACY_METADATA)) return@runCatching null
            val versionCode = json.getInt("versionCode")
            val versionName = json.getString("versionName")
            val minimum = json.optInt("minRequiredVersionCode", 0)
            if (versionCode <= 0 || versionName.isBlank() || minimum !in 0..versionCode) return@runCatching null
            val isRequired = currentVersionCode < minimum
            AppUpdate(
                versionName = versionName,
                versionCode = versionCode,
                isRequired = isRequired
            ).takeIf { versionCode > currentVersionCode || isRequired }
        }.getOrNull()

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

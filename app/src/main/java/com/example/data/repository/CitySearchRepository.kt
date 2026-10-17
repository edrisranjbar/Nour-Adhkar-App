package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class CitySearchResult(val label: String, val latitude: Double, val longitude: Double)
internal data class CitySearchResults(val cities: List<CitySearchResult>, val fallback: Boolean = false)

internal object CitySearchRepository {
    private val legacyExecutor = Executors.newFixedThreadPool(2)

    suspend fun search(context: Context, query: String, language: String): CitySearchResults {
        val native = try {
            nativeSearch(context, query, language)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
        if (native.isNotEmpty()) return CitySearchResults(native)
        return withContext(Dispatchers.IO) {
            val connection = URL(searchUrl(query, language)).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Accept", "application/json")
                check(connection.responseCode == HttpURLConnection.HTTP_OK) { "City search unavailable" }
                val cities = connection.inputStream.bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
                CitySearchResults(cities, fallback = true)
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun searchUrl(query: String, language: String): String {
        val name = URLEncoder.encode(query.trim().replace('،', ','), "UTF-8")
        val code = URLEncoder.encode(Locale.forLanguageTag(language).language.ifBlank { "en" }, "UTF-8")
        return "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=8&language=$code&format=json"
    }

    internal fun parse(json: String): List<CitySearchResult> {
        val root = JSONObject(json)
        check(!root.optBoolean("error")) { "City search unavailable" }
        val results = root.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val city = results.optJSONObject(index) ?: return@mapNotNull null
            val name = city.optString("name").trim()
            val lat = city.optDouble("latitude", Double.NaN)
            val lon = city.optDouble("longitude", Double.NaN)
            if (name.isEmpty() || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                return@mapNotNull null
            }
            val label = listOf(name, city.optString("admin1"), city.optString("country"))
                .filter { it.isNotBlank() }.distinct().joinToString("، ")
            CitySearchResult(label, lat, lon)
        }.distinctBy { Triple(it.label, it.latitude, it.longitude) }
    }

    private suspend fun nativeSearch(context: Context, query: String, language: String): List<CitySearchResult> {
        if (!Geocoder.isPresent()) return emptyList()
        val addresses = withTimeoutOrNull(4_000) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
                val geocoder = Geocoder(context, Locale.forLanguageTag(language))
                fun deliver(addresses: List<Address>) {
                    if (continuation.isActive) continuation.resume(addresses)
                }
                fun fail(message: String?) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocationName(query, 8, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) { deliver(addresses) }
                        override fun onError(errorMessage: String?) { fail(errorMessage) }
                    })
                } else {
                    val future = legacyExecutor.submit {
                        try {
                            @Suppress("DEPRECATION")
                            deliver(geocoder.getFromLocationName(query, 8).orEmpty())
                        } catch (failure: Exception) { fail(failure.message) }
                    }
                    continuation.invokeOnCancellation { future.cancel(true) }
                }
            }
        }.orEmpty()
        return addresses.filter { it.hasLatitude() && it.hasLongitude() }
            .mapNotNull { address ->
                val name = listOf(address.locality, address.subAdminArea, address.featureName)
                    .firstOrNull { !it.isNullOrBlank() } ?: return@mapNotNull null
                val label = listOfNotNull(name, address.adminArea, address.countryName)
                    .filter { it.isNotBlank() }.distinct().joinToString("، ")
                CitySearchResult(label, address.latitude, address.longitude)
            }.distinctBy { Triple(it.label, it.latitude, it.longitude) }
    }
}

package com.example.data.repository

import android.content.Context
import java.text.Normalizer
import java.util.Locale

/** Bundled GeoNames gazetteer: city selection remains usable when geocoding services fail. */
internal object OfflineCitySearch {
    private data class City(
        val name: String, val names: List<String>, val region: String, val country: String,
        val latitude: Double, val longitude: Double, val population: Long
    )
    @Volatile private var cities: List<City>? = null

    @Synchronized private fun load(context: Context): List<City> {
        cities?.let { return it }
        return context.assets.open("cities.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                val f = line.split('\t')
                if (f.size != 7) return@mapNotNull null
                val lat = f[4].toDoubleOrNull() ?: return@mapNotNull null
                val lon = f[5].toDoubleOrNull() ?: return@mapNotNull null
                if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapNotNull null
                City(f[0], f[1].split(','), f[2], f[3], lat, lon, f[6].toLongOrNull() ?: 0)
            }.toList()
        }.also { cities = it }
    }

    fun search(context: Context, query: String, language: String): List<CitySearchResult> {
        val parts = query.replace('،', ',').split(',')
        val name = normalize(parts.first())
        if (name.length < 2) return emptyList()
        val qualifier = normalize(parts.getOrElse(1) { "" })
        val locale = Locale.forLanguageTag(language)
        val countries = mutableMapOf<String, String>()
        fun country(code: String) = countries.getOrPut(code) { Locale("", code).getDisplayCountry(locale) }
        return load(context).asSequence().mapNotNull { city ->
            val exact = city.names.any { it == name }
            if (!exact && city.names.none { it.startsWith(name) }) return@mapNotNull null
            if (qualifier.isNotEmpty() && qualifier !in listOf(
                    normalize(city.country), normalize(country(city.country)),
                    normalize(Locale("", city.country).getDisplayCountry(Locale.ENGLISH)), normalize(city.region)
                )) return@mapNotNull null
            city to exact
        }.sortedWith(compareByDescending<Pair<City, Boolean>> { it.second }.thenByDescending { it.first.population })
            .take(8).map { (city, _) ->
                val display = if (locale.language in listOf("fa", "ar", "ur", "ckb")) {
                    city.names.firstOrNull { it.any { char -> char in '\u0600'..'\u06ff' } } ?: city.name
                } else city.name
                CitySearchResult(listOf(display, city.region, country(city.country)).filter { it.isNotBlank() }
                    .distinct().joinToString("، "), city.latitude, city.longitude)
            }.toList()
    }

    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
        .replace("\u200c", "").trim().lowercase(Locale.ROOT)
}

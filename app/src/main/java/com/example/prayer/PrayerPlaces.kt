package com.example.prayer

import org.json.JSONArray
import org.json.JSONObject

/** Most saved places the user can keep; the switcher sheet stays short. */
const val MAX_PRAYER_PLACES = 10

/**
 * A saved place for prayer times. Everything location-specific lives in [settings]: coordinates,
 * timezone, calculation method, Asr madhab and minute corrections. Adhan voice, enabled prayers and
 * reminders stay global.
 */
data class PrayerPlace(val id: String, val name: String, val settings: PrayerSettings)

/** The saved places and which one drives prayer times, adhan, reminders, the widget and Qibla. */
data class PrayerPlaces(val places: List<PrayerPlace>, val activeId: String?) {
    val active: PrayerPlace? get() = places.firstOrNull { it.id == activeId } ?: places.firstOrNull()

    fun activate(id: String): PrayerPlaces = if (places.any { it.id == id }) copy(activeId = id) else this

    /** Adds [place] and makes it active. Ignored when the list is already full. */
    fun add(place: PrayerPlace): PrayerPlaces =
        if (places.size >= MAX_PRAYER_PLACES) this else PrayerPlaces(places + place, place.id)

    fun update(place: PrayerPlace): PrayerPlaces =
        copy(places = places.map { if (it.id == place.id) place else it })

    /**
     * Removes a place. The last place cannot be removed. Removing the active place activates the
     * first remaining one.
     */
    fun remove(id: String): PrayerPlaces {
        if (places.size <= 1 || places.none { it.id == id }) return this
        val remaining = places.filterNot { it.id == id }
        return PrayerPlaces(remaining, if (activeId == id) remaining.first().id else activeId)
    }

    fun move(id: String, delta: Int): PrayerPlaces {
        val from = places.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in places.indices) return this
        return copy(places = places.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun toJson(): String = JSONArray().apply {
        places.forEach { place ->
            val s = place.settings
            put(JSONObject()
                .put("id", place.id).put("name", place.name)
                .put("location", s.location).put("lat", s.latitude).put("lon", s.longitude)
                .put("zone", s.zone).put("method", s.method).put("hanafi", s.hanafi)
                .put("automatic", s.automaticLocation).put("offsets", JSONArray(s.offsets)))
        }
    }.toString()

    companion object {
        val Empty = PrayerPlaces(emptyList(), null)

        /** Parses saved places, skipping any entry that is not a valid location. */
        fun fromJson(json: String?, activeId: String?): PrayerPlaces? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val array = JSONArray(json)
                val places = (0 until array.length()).mapNotNull { index ->
                    val o = array.getJSONObject(index)
                    val offsets = o.optJSONArray("offsets")?.let { a -> List(a.length()) { a.getInt(it) } } ?: List(6) { 0 }
                    val settings = PrayerSettings(
                        location = o.getString("location"),
                        latitude = o.getDouble("lat"),
                        longitude = o.getDouble("lon"),
                        zone = o.getString("zone"),
                        method = o.getString("method"),
                        hanafi = o.optBoolean("hanafi"),
                        automaticLocation = o.optBoolean("automatic"),
                        offsets = offsets
                    )
                    PrayerPlace(o.getString("id"), o.optString("name").ifBlank { settings.location }, settings)
                        .takeIf { settings.isValid() }
                }.take(MAX_PRAYER_PLACES)
                PrayerPlaces(places, activeId?.takeIf { id -> places.any { it.id == id } } ?: places.firstOrNull()?.id)
            }.getOrNull()
        }

        /** First run after the update: the single saved location becomes place #1, unchanged. */
        fun migrate(legacy: PrayerSettings, newId: () -> String): PrayerPlaces {
            if (!legacy.isValid()) return Empty
            val name = legacy.location.takeUnless { it == "موقعیت فعلی" } ?: "مکان من"
            val id = newId()
            return PrayerPlaces(listOf(PrayerPlace(id, name, legacy)), id)
        }
    }
}

package com.example.data.repository

import android.content.Context
import com.example.qaza.FastingState
import com.example.qaza.QazaLimits
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the missed-fast tracker as one JSON document in its own preferences file. The file is not
 * excluded from Android backup, so the counters survive a restore.
 */
class QazaRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): FastingState = parse(prefs.getString(KEY_STATE, null))

    fun save(state: FastingState) {
        prefs.edit().putString(KEY_STATE, serialize(state)).apply()
    }

    /** Loads the state, applies [change], saves the result and returns it. */
    fun update(change: (FastingState) -> FastingState): FastingState {
        val updated = change(load())
        save(updated)
        return updated
    }

    companion object {
        const val FILE = "qaza_tracker"
        private const val KEY_STATE = "state"
        private const val VERSION = 2

        fun serialize(state: FastingState): String = JSONObject().apply {
            put("v", VERSION)
            put("owed", state.owed)
            put("madeUp", state.madeUp)
            put("dates", JSONArray().also { array -> state.recentDates.forEach { array.put(it) } })
        }.toString()

        /** Reads a stored document, skipping anything missing or malformed instead of failing. */
        fun parse(json: String?): FastingState {
            if (json.isNullOrBlank()) return FastingState()
            return runCatching {
                val root = JSONObject(json)
                val owed = root.optInt("owed", 0).coerceIn(0, QazaLimits.MAX_FASTS)
                val madeUp = root.optInt("madeUp", 0).coerceIn(0, owed)
                val dates = buildList<Long> {
                    root.optJSONArray("dates")?.let { array ->
                        for (i in 0 until array.length()) add(array.optLong(i, 0L))
                    }
                }.takeLast(QazaLimits.RECENT_DATES)
                FastingState(owed = owed, madeUp = madeUp, recentDates = dates)
            }.getOrDefault(FastingState())
        }
    }
}

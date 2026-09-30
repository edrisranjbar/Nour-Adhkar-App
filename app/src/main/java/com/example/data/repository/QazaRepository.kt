package com.example.data.repository

import android.content.Context
import com.example.qaza.FastEntry
import com.example.qaza.PrayerDebt
import com.example.qaza.QazaAction
import com.example.qaza.QazaActionType
import com.example.qaza.QazaChange
import com.example.qaza.QazaLimits
import com.example.qaza.QazaPrayer
import com.example.qaza.QazaState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the missed-prayer and missed-fast tracker as one JSON document in its own preferences
 * file. The file is not excluded from Android backup, so the counters survive a restore.
 */
class QazaRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): QazaState = parse(prefs.getString(KEY_STATE, null))

    fun save(state: QazaState) {
        prefs.edit().putString(KEY_STATE, serialize(state)).apply()
    }

    /** Loads the state, applies [change], saves the result and returns it. */
    fun update(change: (QazaState) -> QazaState): QazaState {
        val updated = change(load())
        save(updated)
        return updated
    }

    companion object {
        const val FILE = "qaza_tracker"
        private const val KEY_STATE = "state"
        private const val VERSION = 1

        fun serialize(state: QazaState): String = JSONObject().apply {
            put("v", VERSION)
            put("witr", state.witrEnabled)
            put("sets", state.dailySets)
            put("prayers", JSONObject().also { obj ->
                state.prayers.forEach { (prayer, debt) ->
                    obj.put(prayer.id, JSONObject().put("o", debt.owed).put("m", debt.madeUp))
                }
            })
            put("history", JSONArray().also { array ->
                state.history.forEach { action ->
                    array.put(
                        JSONObject()
                            .put("t", action.type.name)
                            .put("at", action.time)
                            .put("c", JSONArray().also { changes ->
                                action.changes.forEach { change ->
                                    changes.put(
                                        JSONObject()
                                            .put("p", change.prayer.id)
                                            .put("bo", change.before.owed).put("bm", change.before.madeUp)
                                            .put("ao", change.after.owed).put("am", change.after.madeUp)
                                    )
                                }
                            })
                    )
                }
            })
            put("fasts", JSONArray().also { array ->
                state.fasts.forEach { fast ->
                    array.put(
                        JSONObject()
                            .put("id", fast.id).put("n", fast.count)
                            .put("l", fast.label).put("r", fast.reason)
                            .put("d", JSONArray().also { dates -> fast.madeUpDates.forEach { dates.put(it) } })
                    )
                }
            })
        }.toString()

        /** Reads a stored document, skipping anything missing or malformed instead of failing. */
        fun parse(json: String?): QazaState {
            if (json.isNullOrBlank()) return QazaState()
            return runCatching {
                val root = JSONObject(json)
                val prayers = buildMap<QazaPrayer, PrayerDebt> {
                    root.optJSONObject("prayers")?.let { obj ->
                        obj.keys().forEach { key ->
                            val prayer = QazaPrayer.fromId(key) ?: return@forEach
                            val item = obj.optJSONObject(key) ?: return@forEach
                            val owed = item.optInt("o", 0).coerceIn(0, Int.MAX_VALUE)
                            val made = item.optInt("m", 0).coerceIn(0, owed)
                            put(prayer, PrayerDebt(owed, made))
                        }
                    }
                }
                val history = buildList<QazaAction> {
                    root.optJSONArray("history")?.let { array ->
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val type = QazaActionType.values().firstOrNull { it.name == item.optString("t") } ?: continue
                            val changes = buildList<QazaChange> {
                                item.optJSONArray("c")?.let { list ->
                                    for (j in 0 until list.length()) {
                                        val c = list.optJSONObject(j) ?: continue
                                        val prayer = QazaPrayer.fromId(c.optString("p")) ?: continue
                                        add(
                                            QazaChange(
                                                prayer,
                                                PrayerDebt(c.optInt("bo", 0), c.optInt("bm", 0)),
                                                PrayerDebt(c.optInt("ao", 0), c.optInt("am", 0))
                                            )
                                        )
                                    }
                                }
                            }
                            if (changes.isNotEmpty()) add(QazaAction(type, item.optLong("at", 0L), changes))
                        }
                    }
                }.takeLast(QazaLimits.HISTORY_LIMIT)
                val fasts = buildList<FastEntry> {
                    root.optJSONArray("fasts")?.let { array ->
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val count = item.optInt("n", 0)
                            if (count <= 0) continue
                            val dates = buildList<Long> {
                                item.optJSONArray("d")?.let { list ->
                                    for (j in 0 until list.length()) add(list.optLong(j, 0L))
                                }
                            }
                            add(
                                FastEntry(
                                    id = item.optLong("id", (i + 1).toLong()),
                                    count = count.coerceAtMost(QazaLimits.MAX_FAST_COUNT),
                                    label = item.optString("l", ""),
                                    reason = item.optString("r", ""),
                                    madeUpDates = dates
                                )
                            )
                        }
                    }
                }
                QazaState(
                    prayers = prayers,
                    witrEnabled = root.optBoolean("witr", false),
                    dailySets = root.optInt("sets", 1).coerceIn(1, QazaLimits.MAX_DAILY_SETS),
                    history = history,
                    fasts = fasts
                )
            }.getOrDefault(QazaState())
        }
    }
}

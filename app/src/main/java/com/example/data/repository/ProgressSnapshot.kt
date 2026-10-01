package com.example.data.repository

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.security.MessageDigest

/** An explicit allowlist: never sync tokens, installation identity, location or notification settings. */
class ProgressSnapshot(private val context: Context) {
    private val prefs = context.getSharedPreferences("nour_adhkar_prefs", Context.MODE_PRIVATE)
    private val qaza = context.getSharedPreferences("qaza_tracker", Context.MODE_PRIVATE)
    private val database = AdhkarDatabase.getDatabase(context)
    private fun allowed(key: String) = key in setOf(
        "activity_day_keys", "favorite_dhikr_keys", "custom_dhikr", "tasbih_counts_map",
        "quran_last_read_page", "quran_highlights", "quran_notes", "quran_khatm_goal", "quran_khatm_daily_logs"
    ) || key.matches(Regex("daily_checklist_-?\\d+")) || key.endsWith("_completed_day")

    suspend fun read(): JSONObject {
        val result = JSONObject()
        prefs.all.filterKeys(::allowed).forEach { (key, value) ->
            if (value is Set<*>) value.filterIsInstance<String>().sorted().forEach { member ->
                result.put("s:$key:${encode(member)}", true)
            } else {
                val type = when (value) { is Int -> "int"; is Long -> "long"; is String -> "string"; else -> return@forEach }
                result.put("p:$key", JSONObject().put("type", type).put("value", value))
            }
        }
        qaza.getString("state", null)?.let { result.put("q:state", it) }
        database.withTransaction {
            database.dhikrProgressDao().getAllProgress().first().forEach { row ->
                result.put("d:${row.id}", JSONObject().put("category", row.categoryId).put("dhikr", row.dhikrId)
                    .put("count", row.currentCount).put("target", row.targetCount).put("updated", row.lastUpdated))
            }
            database.tasbihSessionDao().getAllSessions().forEach { row ->
                result.put("h:${historyKey(row)}", JSONObject().put("name", row.dhikrName).put("count", row.count).put("time", row.timestamp))
            }
        }
        return result
    }

    suspend fun apply(before: JSONObject, after: JSONObject) {
        val editor = prefs.edit()
        val sets = mutableMapOf<String, MutableSet<String>>()
        (before.keys().asSequence().toSet() + after.keys().asSequence().toSet()).forEach { key ->
            if (before.opt(key)?.toString() == after.opt(key)?.toString()) return@forEach
            when {
                key.startsWith("s:") -> {
                    val parts = key.split(':', limit = 3)
                    if (parts.size == 3 && allowed(parts[1])) {
                        val values = sets.getOrPut(parts[1]) { prefs.getStringSet(parts[1], emptySet()).orEmpty().toMutableSet() }
                        val member = decode(parts[2])
                        if ((member in values) != before.has(key)) return@forEach
                        if (after.has(key)) values.add(member) else values.remove(member)
                    }
                }
                key.startsWith("p:") && allowed(key.substring(2)) -> {
                    val name = key.substring(2)
                    val expected = before.optJSONObject(key)?.opt("value")
                    if (prefs.all[name]?.toString() != expected?.toString()) return@forEach
                    val value = after.optJSONObject(key)
                    if (value == null) editor.remove(name)
                    else when (value.getString("type")) {
                        "int" -> editor.putInt(name, value.getInt("value"))
                        "long" -> editor.putLong(name, value.getLong("value"))
                        "string" -> editor.putString(name, value.getString("value"))
                    }
                }
            }
        }
        sets.forEach { (key, value) -> editor.putStringSet(key, value) }
        editor.commit()
        if (qaza.getString("state", null) == before.optString("q:state", null)) {
            if (after.has("q:state")) qaza.edit().putString("state", after.getString("q:state")).commit()
            else if (before.has("q:state")) qaza.edit().remove("state").commit()
        }
        // Room merges in one transaction and retains unchanged row IDs.
        database.withTransaction {
            val progress = database.dhikrProgressDao()
            val history = database.tasbihSessionDao()
            val sessions = history.getAllSessions().associateBy(::historyKey)
            (before.keys().asSequence().toSet() + after.keys().asSequence().toSet()).forEach { key ->
                if (before.opt(key)?.toString() == after.opt(key)?.toString()) return@forEach
                when {
                    key.startsWith("d:") -> {
                        val current = progress.getProgressById(key.substring(2))
                        val expected = before.optJSONObject(key)
                        if ((current == null) != (expected == null) || (current != null && expected != null &&
                            (current.currentCount != expected.getInt("count") || current.lastUpdated != expected.getLong("updated")))) return@forEach
                        val row = after.optJSONObject(key)
                        if (row == null) progress.deleteProgressById(key.substring(2))
                        else progress.insertOrUpdateProgress(DhikrProgressEntity(key.substring(2), row.getString("category"),
                            row.getInt("dhikr"), row.getInt("count").coerceAtLeast(0), row.getInt("target"), row.getLong("updated")))
                    }
                    key.startsWith("h:") -> {
                        val row = after.optJSONObject(key)
                        val old = sessions[key.substring(2)]
                        if (row == null) old?.let { history.deleteSessionById(it.id) }
                        else if (old == null) history.insertSession(TasbihSessionEntity(dhikrName = row.getString("name"),
                            count = row.getInt("count"), timestamp = row.getLong("time")))
                    }
                }
            }
        }
    }

    private fun historyKey(row: TasbihSessionEntity): String = MessageDigest.getInstance("SHA-256")
        .digest("${row.timestamp}|${row.dhikrName}|${row.count}".toByteArray()).joinToString("") { "%02x".format(it) }
    private fun encode(value: String) = Base64.encodeToString(value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun decode(value: String) = String(Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
}

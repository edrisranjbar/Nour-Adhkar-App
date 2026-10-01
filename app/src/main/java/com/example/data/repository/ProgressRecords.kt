package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject

/** Per-item last-write-wins records retain deletions, so an older phone cannot resurrect them. */
object ProgressRecords {
    fun capture(previous: JSONObject, records: JSONObject, snapshot: JSONObject, device: String, now: Long): JSONObject {
        val result = JSONObject(records.toString())
        (previous.keys().asSequence().toSet() + snapshot.keys().asSequence().toSet()).forEach { key ->
            val before = previous.opt(key)
            val after = snapshot.opt(key)
            if (before?.toString() != after?.toString()) {
                val last = result.optJSONObject(key)?.optLong("modified", 0) ?: 0
                result.put(key, JSONObject().put("key", key).put("modified", maxOf(now, last + 1))
                    .put("device", device).put("deleted", after == null).put("value", after ?: JSONObject.NULL))
            }
        }
        return result
    }

    fun merge(local: JSONObject, remote: JSONArray): JSONObject {
        val result = JSONObject(local.toString())
        for (i in 0 until remote.length()) {
            val incoming = remote.getJSONObject(i)
            val key = incoming.getString("key")
            val old = result.optJSONObject(key)
            if (old == null || incoming.getLong("modified") > old.getLong("modified") ||
                (incoming.getLong("modified") == old.getLong("modified") && incoming.getString("device") > old.getString("device"))) {
                result.put(key, incoming)
            }
        }
        return result
    }

    fun snapshot(records: JSONObject): JSONObject = JSONObject().also { result ->
        records.keys().forEach { key ->
            val record = records.getJSONObject(key)
            if (!record.getBoolean("deleted")) result.put(key, record.get("value"))
        }
    }

    fun array(records: JSONObject): JSONArray = JSONArray().also { array ->
        records.keys().forEach { array.put(records.getJSONObject(it)) }
    }
}

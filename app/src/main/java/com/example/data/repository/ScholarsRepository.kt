package com.example.data.repository

import android.content.Context
import com.example.data.model.Lecture
import com.example.data.model.Scholar
import org.json.JSONObject

/** Scholars and lectures from the bundled `assets/scholars.json` (see docs/scholars.md). */
object ScholarsRepository {
    fun load(context: Context): List<Scholar> = runCatching {
        val root = context.assets.open("scholars.json").bufferedReader().use { JSONObject(it.readText()) }
        val scholars = root.getJSONArray("scholars")
        (0 until scholars.length()).map { i ->
            val s = scholars.getJSONObject(i)
            val id = s.getString("id")
            val lectures = s.optJSONArray("lectures")
            Scholar(
                id = id,
                name = s.getString("name"),
                tagline = s.optString("tagline"),
                bio = s.optString("bio"),
                hue = s.optDouble("hue", 150.0).toFloat(),
                lectures = (0 until (lectures?.length() ?: 0)).map { j ->
                    val l = lectures!!.getJSONObject(j)
                    Lecture(
                        id = "$id-${l.optString("id", j.toString())}",
                        title = l.getString("title"),
                        description = l.optString("description"),
                        audioUrl = l.getString("audioUrl"),
                        durationSec = l.optInt("durationSec", -1).takeIf { it > 0 }
                    )
                }
            )
        }
    }.getOrDefault(emptyList())
}

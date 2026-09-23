package com.example.quran

import android.content.Context
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.ceil

data class QuranKhatmGoal(
    val startedDayKey: Long,
    val targetDays: Int,
    val startPage: Int,
    val lastCompletedPage: Int,
    val reminderEnabled: Boolean,
    val reminderTime: String,
    val paused: Boolean = false,
    val completedDayKey: Long? = null,
    val pausedDayKey: Long? = null
) {
    val isComplete: Boolean get() = lastCompletedPage >= QuranRepository.PAGE_COUNT
}

data class QuranKhatmDailyLog(
    val dayKey: Long,
    val completedThroughPage: Int
)

data class QuranKhatmPlan(
    val dayNumber: Int,
    val targetDays: Int,
    val targetStartPage: Int,
    val targetEndPage: Int,
    val remainingDays: Int,
    val progress: Float,
    val status: QuranKhatmStatus,
    val deadlineDayKey: Long
)

enum class QuranKhatmStatus { AHEAD, ON_TRACK, BEHIND, COMPLETE }

object QuranKhatmPlanner {
    fun plan(goal: QuranKhatmGoal, todayDayKey: Long = dayKey()): QuranKhatmPlan {
        val effectiveToday = goal.pausedDayKey ?: todayDayKey
        val elapsedDays = daysBetween(goal.startedDayKey, effectiveToday).coerceAtLeast(0)
        val dayNumber = (elapsedDays + 1).coerceAtMost(goal.targetDays.coerceAtLeast(1))
        val remainingDays = (goal.targetDays - elapsedDays).coerceAtLeast(1)
        val nextPage = (goal.lastCompletedPage + 1).coerceIn(goal.startPage, QuranRepository.PAGE_COUNT)
        val remainingPages = (QuranRepository.PAGE_COUNT - goal.lastCompletedPage).coerceAtLeast(0)
        val quota = if (remainingPages == 0) 0 else ceil(remainingPages / remainingDays.toDouble()).toInt()
        val targetEnd = if (remainingPages == 0) QuranRepository.PAGE_COUNT
        else (nextPage + quota - 1).coerceAtMost(QuranRepository.PAGE_COUNT)
        val totalPages = (QuranRepository.PAGE_COUNT - goal.startPage + 1).coerceAtLeast(1)
        val completedPages = (goal.lastCompletedPage - goal.startPage + 1).coerceIn(0, totalPages)
        val expectedThroughYesterday = if (elapsedDays <= 0) {
            goal.startPage - 1
        } else {
            goal.startPage - 1 + ceil(totalPages * elapsedDays.coerceAtMost(goal.targetDays) / goal.targetDays.toDouble()).toInt()
        }
        val status = when {
            goal.isComplete -> QuranKhatmStatus.COMPLETE
            goal.lastCompletedPage > expectedThroughYesterday -> QuranKhatmStatus.AHEAD
            goal.lastCompletedPage < expectedThroughYesterday -> QuranKhatmStatus.BEHIND
            else -> QuranKhatmStatus.ON_TRACK
        }
        return QuranKhatmPlan(
            dayNumber = dayNumber,
            targetDays = goal.targetDays,
            targetStartPage = nextPage,
            targetEndPage = targetEnd,
            remainingDays = remainingDays,
            progress = completedPages / totalPages.toFloat(),
            status = status,
            deadlineDayKey = addDays(goal.startedDayKey, goal.targetDays - 1)
        )
    }

    fun dayKey(timeMillis: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(dayKey: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayKey
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    fun daysBetween(startDayKey: Long, endDayKey: Long): Int {
        if (endDayKey <= startDayKey) return 0
        val cursor = Calendar.getInstance().apply { timeInMillis = startDayKey }
        var days = 0
        while (cursor.timeInMillis < endDayKey && days < 10_000) {
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            days++
        }
        return days
    }
}

class QuranKhatmRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nour_adhkar_prefs",
        Context.MODE_PRIVATE
    )

    fun getGoal(): QuranKhatmGoal? = runCatching {
        val raw = prefs.getString(KEY_GOAL, null) ?: return null
        val value = JSONObject(raw)
        QuranKhatmGoal(
            startedDayKey = value.getLong("startedDayKey"),
            targetDays = value.getInt("targetDays").coerceAtLeast(1),
            startPage = value.getInt("startPage").coerceIn(1, QuranRepository.PAGE_COUNT),
            lastCompletedPage = value.getInt("lastCompletedPage").coerceIn(0, QuranRepository.PAGE_COUNT),
            reminderEnabled = value.optBoolean("reminderEnabled", true),
            reminderTime = value.optString("reminderTime", "20:00"),
            paused = value.optBoolean("paused", false),
            completedDayKey = value.optLong("completedDayKey").takeIf { value.has("completedDayKey") },
            pausedDayKey = value.optLong("pausedDayKey").takeIf { value.has("pausedDayKey") }
        )
    }.getOrNull()

    fun saveGoal(goal: QuranKhatmGoal) {
        val value = JSONObject()
            .put("startedDayKey", goal.startedDayKey)
            .put("targetDays", goal.targetDays.coerceAtLeast(1))
            .put("startPage", goal.startPage.coerceIn(1, QuranRepository.PAGE_COUNT))
            .put("lastCompletedPage", goal.lastCompletedPage.coerceIn(0, QuranRepository.PAGE_COUNT))
            .put("reminderEnabled", goal.reminderEnabled)
            .put("reminderTime", goal.reminderTime)
            .put("paused", goal.paused)
        goal.completedDayKey?.let { value.put("completedDayKey", it) }
        goal.pausedDayKey?.let { value.put("pausedDayKey", it) }
        prefs.edit().putString(KEY_GOAL, value.toString()).apply()
    }

    fun createGoal(
        targetDays: Int,
        startPage: Int,
        reminderEnabled: Boolean,
        reminderTime: String,
        todayDayKey: Long = QuranKhatmPlanner.dayKey()
    ): QuranKhatmGoal = QuranKhatmGoal(
        startedDayKey = todayDayKey,
        targetDays = targetDays.coerceIn(1, 3650),
        startPage = startPage.coerceIn(1, QuranRepository.PAGE_COUNT),
        lastCompletedPage = (startPage - 1).coerceAtLeast(0),
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime
    ).also {
        prefs.edit().remove(KEY_LOGS).apply()
        saveGoal(it)
    }

    fun recordProgress(page: Int, todayDayKey: Long = QuranKhatmPlanner.dayKey()): QuranKhatmGoal? {
        val current = getGoal() ?: return null
        if (current.paused || current.isComplete) return current
        if (page !in current.startPage..QuranRepository.PAGE_COUNT || page <= current.lastCompletedPage) return current
        val completedPage = page
        val updated = current.copy(
            lastCompletedPage = completedPage,
            completedDayKey = todayDayKey.takeIf { completedPage == QuranRepository.PAGE_COUNT }
        )
        saveGoal(updated)
        val logs = getDailyLogs().associateBy(QuranKhatmDailyLog::dayKey).toMutableMap()
        logs[todayDayKey] = QuranKhatmDailyLog(todayDayKey, completedPage)
        saveDailyLogs(logs.values.sortedBy(QuranKhatmDailyLog::dayKey))
        return updated
    }

    fun setPaused(paused: Boolean, todayDayKey: Long = QuranKhatmPlanner.dayKey()): QuranKhatmGoal? {
        val current = getGoal() ?: return null
        if (current.paused == paused || current.isComplete) return current
        val updated = if (paused) {
            current.copy(paused = true, pausedDayKey = todayDayKey)
        } else {
            val pausedDays = current.pausedDayKey?.let { QuranKhatmPlanner.daysBetween(it, todayDayKey) } ?: 0
            current.copy(
                startedDayKey = QuranKhatmPlanner.addDays(current.startedDayKey, pausedDays),
                paused = false,
                pausedDayKey = null
            )
        }
        saveGoal(updated)
        return updated
    }

    fun updateGoal(targetDays: Int, reminderEnabled: Boolean, reminderTime: String): QuranKhatmGoal? =
        getGoal()?.copy(
            targetDays = targetDays.coerceIn(1, 3650),
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime
        )?.also(::saveGoal)

    fun getDailyLogs(): List<QuranKhatmDailyLog> = runCatching {
        val value = JSONObject(prefs.getString(KEY_LOGS, "{}").orEmpty())
        buildList {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                add(QuranKhatmDailyLog(key.toLong(), value.getInt(key)))
            }
        }.sortedByDescending(QuranKhatmDailyLog::dayKey)
    }.getOrDefault(emptyList())

    fun clearGoal() {
        prefs.edit().remove(KEY_GOAL).remove(KEY_LOGS).apply()
    }

    private fun saveDailyLogs(logs: List<QuranKhatmDailyLog>) {
        val value = JSONObject()
        logs.forEach { value.put(it.dayKey.toString(), it.completedThroughPage) }
        prefs.edit().putString(KEY_LOGS, value.toString()).apply()
    }

    private companion object {
        const val KEY_GOAL = "quran_khatm_goal"
        const val KEY_LOGS = "quran_khatm_daily_logs"
    }
}

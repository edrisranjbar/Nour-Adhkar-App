package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class PreferenceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nour_adhkar_prefs",
        Context.MODE_PRIVATE
    )

    fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean("vibration_enabled", true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun getFontScale(): Float {
        return prefs.getFloat("font_scale", 1.0f)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun getSelectedFeeling(): String? {
        if (prefs.getLong("selected_feeling_day", Long.MIN_VALUE) != currentDayKey()) {
            clearSelectedFeeling()
            return null
        }
        return prefs.getString("selected_feeling", null)
    }

    fun setSelectedFeeling(feeling: String) {
        prefs.edit()
            .putString("selected_feeling", feeling)
            .putLong("selected_feeling_day", currentDayKey())
            .apply()
    }

    fun clearSelectedFeeling() {
        prefs.edit()
            .remove("selected_feeling")
            .remove("selected_feeling_day")
            .apply()
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun getMorningNotificationTime(): String {
        return prefs.getString("morning_notif_time", "07:00") ?: "07:00"
    }

    fun setMorningNotificationTime(time: String) {
        prefs.edit().putString("morning_notif_time", time).apply()
    }

    fun getEveningNotificationTime(): String {
        return prefs.getString("evening_notif_time", "18:00") ?: "18:00"
    }

    fun setEveningNotificationTime(time: String) {
        prefs.edit().putString("evening_notif_time", time).apply()
    }

    fun markAdhkarCompletedToday(categoryId: String) {
        prefs.edit().putLong(completionKey(categoryId), currentDayKey()).apply()
    }

    fun isAdhkarCompletedToday(categoryId: String): Boolean {
        return prefs.getLong(completionKey(categoryId), Long.MIN_VALUE) == currentDayKey()
    }

    fun getDailyChecklistCompletedIds(dayKey: Long = currentDayKey()): Set<String> {
        migrateLegacyDailyChecklistIfNeeded()
        return prefs.getStringSet(checklistKey(dayKey), emptySet())?.toSet().orEmpty()
    }

    fun setDailyChecklistItemCompleted(
        dayKey: Long,
        itemId: String,
        completed: Boolean
    ): Set<String> {
        val updated = getDailyChecklistCompletedIds(dayKey).toMutableSet().apply {
            if (completed) add(itemId) else remove(itemId)
        }
        prefs.edit()
            .putStringSet(checklistKey(dayKey), updated)
            .apply()
        return updated.toSet()
    }

    private fun migrateLegacyDailyChecklistIfNeeded() {
        val legacyDay = prefs.getLong("daily_checklist_day", Long.MIN_VALUE)
        val legacyItems = prefs.getStringSet("daily_checklist_completed", null)?.toSet()
        if (legacyItems != null && legacyDay == currentDayKey()) {
            prefs.edit().putStringSet(checklistKey(currentDayKey()), legacyItems).apply()
        }
        if (legacyDay != Long.MIN_VALUE || legacyItems != null) {
            prefs.edit()
                .remove("daily_checklist_completed")
                .remove("daily_checklist_day")
                .apply()
        }
    }

    private fun checklistKey(dayKey: Long) = "daily_checklist_$dayKey"

    private fun completionKey(categoryId: String) = "${categoryId}_completed_day"

    private fun currentDayKey(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

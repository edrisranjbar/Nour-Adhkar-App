package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class PreferenceRepository(context: Context) {

    fun getAppLanguage() = com.example.ui.language.AppLanguage.fromCode(prefs.getString("app_language", "fa"))

    fun setAppLanguage(language: com.example.ui.language.AppLanguage) {
        prefs.edit().putString("app_language", language.code).apply()
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nour_adhkar_prefs",
        Context.MODE_PRIVATE
    )

    fun getPrayerSettings(): com.example.prayer.PrayerSettings = runCatching {
        com.example.prayer.PrayerSettings(prefs.getString("prayer_location", "").orEmpty(),
            prefs.getString("prayer_lat", "0")!!.toDouble(), prefs.getString("prayer_lon", "0")!!.toDouble(),
            prefs.getString("prayer_zone", "Asia/Tehran")!!, prefs.getString("prayer_method", "MUSLIM_WORLD_LEAGUE")!!,
            prefs.getBoolean("prayer_hanafi", false),
            prefs.getBoolean("prayer_automatic_location", prefs.getString("prayer_location", "").orEmpty() in listOf("", "موقعیت فعلی")))
    }.getOrDefault(com.example.prayer.PrayerSettings())
    fun setPrayerSettings(value: com.example.prayer.PrayerSettings) {
        prefs.edit().putString("prayer_location", value.location).putString("prayer_lat", value.latitude.toString())
            .putString("prayer_lon", value.longitude.toString()).putString("prayer_zone", value.zone)
            .putString("prayer_method", value.method).putBoolean("prayer_hanafi", value.hanafi)
            .putBoolean("prayer_automatic_location", value.automaticLocation).apply()
    }

    fun getAdhanSound(): com.example.prayer.AdhanSound {
        val saved = com.example.prayer.AdhanSound(prefs.getString("adhan_sound_id", "").orEmpty())
        return saved.takeIf { it.isSelected } ?: com.example.prayer.AdhanSound()
    }

    fun setAdhanSound(sound: com.example.prayer.AdhanSound) {
        require(sound.id.isEmpty() || sound.isSelected)
        prefs.edit().putString("adhan_sound_id", sound.id)
            .remove("adhan_sound_uri").remove("adhan_sound_name").apply()
    }

    fun getAdhanPrayers(): Set<com.example.prayer.AdhanPrayer> =
        prefs.getStringSet("adhan_prayers", emptySet()).orEmpty().mapNotNull { id ->
            com.example.prayer.AdhanPrayer.entries.firstOrNull { it.name == id }
        }.toSet()

    fun setAdhanPrayer(prayer: com.example.prayer.AdhanPrayer, enabled: Boolean) {
        val selected = getAdhanPrayers().toMutableSet()
        if (enabled) selected.add(prayer) else selected.remove(prayer)
        prefs.edit().putStringSet("adhan_prayers", selected.map { it.name }.toSet()).apply()
    }

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

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean("dark_mode_enabled", false)

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    fun isVolumeKeyCountingEnabled(): Boolean {
        return prefs.getBoolean("volume_key_counting_enabled", false)
    }

    fun setVolumeKeyCountingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("volume_key_counting_enabled", enabled).apply()
    }

    fun getVolumeCountButton(): com.example.data.model.VolumeCountButton {
        val raw = prefs.getString("volume_count_button", com.example.data.model.VolumeCountButton.BOTH.id)
        return com.example.data.model.VolumeCountButton.fromId(raw)
    }

    fun setVolumeCountButton(button: com.example.data.model.VolumeCountButton) {
        prefs.edit().putString("volume_count_button", button.id).apply()
    }

    fun getCustomDhikr(): List<String> =
        prefs.getStringSet("custom_dhikr", emptySet()).orEmpty().sorted()

    fun addCustomDhikr(text: String): List<String> {
        val updated = getCustomDhikr().toMutableSet().apply { add(text.trim()) }
        prefs.edit().putStringSet("custom_dhikr", updated).apply()
        return updated.sorted()
    }

    fun removeCustomDhikr(text: String): List<String> {
        val updated = getCustomDhikr().toMutableSet().apply { remove(text) }
        prefs.edit().putStringSet("custom_dhikr", updated).apply()
        return updated.sorted()
    }

    fun getFavoriteDhikrKeys(): Set<String> =
        prefs.getStringSet("favorite_dhikr_keys", emptySet())?.toSet().orEmpty()

    fun toggleFavoriteDhikr(key: String): Set<String> {
        val updated = getFavoriteDhikrKeys().toMutableSet().apply {
            if (!add(key)) remove(key)
        }
        prefs.edit().putStringSet("favorite_dhikr_keys", updated).apply()
        return updated.toSet()
    }

    fun getActivityDayKeys(): Set<Long> =
        prefs.getStringSet("activity_day_keys", emptySet()).orEmpty().mapNotNull(String::toLongOrNull).toSet()

    fun markActivityToday(): Set<Long> {
        val updated = getActivityDayKeys().toMutableSet().apply { add(currentDayKey()) }
        prefs.edit().putStringSet("activity_day_keys", updated.map(Long::toString).toSet()).apply()
        return updated
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

    fun isFridayKahfReminderEnabled(): Boolean =
        prefs.getBoolean("friday_kahf_reminder_enabled", true)

    fun setFridayKahfReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("friday_kahf_reminder_enabled", enabled).apply()
    }

    fun getFridayKahfReminderTime(): String =
        prefs.getString("friday_kahf_reminder_time", "09:00") ?: "09:00"

    fun setFridayKahfReminderTime(time: String) {
        prefs.edit().putString("friday_kahf_reminder_time", time).apply()
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

    fun getChecklistCompletionCounts(days: Int): Map<Long, Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return buildMap {
            repeat(days) {
                val dayKey = calendar.timeInMillis
                put(dayKey, getDailyChecklistCompletedIds(dayKey).size)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
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

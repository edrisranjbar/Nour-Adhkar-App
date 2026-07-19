package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

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
}

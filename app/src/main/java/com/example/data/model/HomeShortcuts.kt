package com.example.data.model

data class HomeShortcut(val id: String, val title: String, val icon: String, val categoryId: String? = null)

/** Stable navigation IDs, not translated labels, are stored in the user's preferences. */
object HomeShortcuts {
    val pages = listOf(
        HomeShortcut("quran", "قرآن کریم", "book"),
        HomeShortcut("quran_audio", "قرآن صوتی", "audio"),
        HomeShortcut("tasbih", "تسبیح", "tasbih"),
        HomeShortcut("checklist", "چک‌لیست روزانه", "checklist"),
        HomeShortcut("calendar", "تقویم", "calendar"),
        HomeShortcut("qibla", "قبله‌نما", "qibla"),
        HomeShortcut("adhkar", "اذکار و ادعیه", "book"),
        HomeShortcut("favorites", "علاقه‌مندی‌ها", "favorite"),
        HomeShortcut("scholars", "علما و مشاهیر", "lectures"),
        HomeShortcut("articles", "مقالات", "article"),
        HomeShortcut("qaza", "قضای روزه", "fasting"),
        HomeShortcut("stats", "آمار من", "stats"),
        HomeShortcut("prayer_settings", "تنظیمات اوقات شرعی", "prayer"),
        HomeShortcut("settings", "تنظیمات", "settings")
    )
    val collections = AdhkarData.categories.filter { it.isEnabled }.map {
        HomeShortcut("collection:${it.id}", it.title, it.iconName, categoryId = it.id)
    }
    val all = pages + collections
    val defaults = listOf("quran_audio", "tasbih", "calendar", "collection:after_salah")
    fun normalize(ids: List<String>): List<String> = ids.filter { id -> all.any { it.id == id } }.distinct()
}

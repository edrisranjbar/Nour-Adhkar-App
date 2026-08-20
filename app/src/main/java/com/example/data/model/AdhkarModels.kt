package com.example.data.model

data class Category(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val count: Int,
    val isEnabled: Boolean = true
)

data class DhikrItem(
    val id: Int,
    val arabicText: String,
    val persianTranslation: String,
    val targetCount: Int,
    val source: String,
    val currentCount: Int = 0
)

data class ArticleItem(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val readTime: String,
    val author: String
)

data class AyahOfTheDay(
    val text: String,
    val translation: String,
    val reference: String
)

enum class UserFeeling(val storageKey: String, val title: String, val emoji: String) {
    ANXIOUS("anxious", "نگرانم", "😟"),
    SAD("sad", "غمگینم", "😔"),
    REGRETFUL("regretful", "پشیمانم", "😞"),
    ANGRY("angry", "عصبانی‌ام", "😠"),
    OVERWHELMED("overwhelmed", "خسته و تحت فشارم", "😫"),
    GRATEFUL("grateful", "آرام و شکرگزارم", "🙂");

    companion object {
        fun fromStorageKey(value: String?): UserFeeling? = entries.firstOrNull { it.storageKey == value }
    }
}

data class EmotionalAyah(
    val id: String,
    val feeling: UserFeeling,
    val text: String,
    val translation: String,
    val reference: String,
    val reflection: String,
    val translationSource: String = "ترجمه: تفسیر نور، دکتر مصطفی خرمدل"
)

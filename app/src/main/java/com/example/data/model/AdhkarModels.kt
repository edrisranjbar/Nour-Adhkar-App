package com.example.data.model

data class Category(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val count: Int
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

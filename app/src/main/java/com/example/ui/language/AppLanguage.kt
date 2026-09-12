package com.example.ui.language

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val label: String) {
    FARSI("fa", "فارسی"),
    ARABIC("ar", "العربية");

    val showPersianTranslation: Boolean get() = this == FARSI

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: FARSI
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FARSI }

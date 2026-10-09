package com.example.ui.language

import androidx.compose.runtime.staticCompositionLocalOf

/** [code] is the stable preference value; [localeTag] is the Android/BCP-47 locale. */
enum class AppLanguage(val code: String, val label: String, val localeTag: String) {
    FARSI("fa", "فارسی", "fa"),
    /** Afghan Dari: the Persian interface with Afghan month names and wording (see [DariCatalog]). */
    DARI("prs", "دری", "fa-AF"),
    /** Central Kurdish (Sorani): its own interface catalog ([KurdishCatalog]) and Sorani Quran translation. */
    KURDISH("ckb", "کوردی", "ckb-IR"),
    /** Urdu (Pakistan): its own interface catalog ([UrduCatalog]) and Urdu Quran translation. */
    URDU("ur", "اردو", "ur-PK"),
    ARABIC("ar", "العربية", "ar");

    /** Dari readers use the same Persian translations, tafsirs and adhkar meanings as Farsi. */
    val usesPersianContent: Boolean get() = this == FARSI || this == DARI
    val showPersianTranslation: Boolean get() = usesPersianContent

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: FARSI
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FARSI }

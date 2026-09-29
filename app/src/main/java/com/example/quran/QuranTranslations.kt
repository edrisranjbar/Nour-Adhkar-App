package com.example.quran

import android.content.Context
import com.example.ui.language.AppLanguage
import java.io.InputStream

/** A bundled, offline Sunni translation/tafsir of the Quran, one entry per verse. */
enum class QuranTranslation(
    val asset: String,
    /** Attribution line shown under the text. */
    val credit: String
) {
    /** Islamhouse.com Persian team, published on QuranEnc (Rowwad Translation Center). */
    PERSIAN_ISLAMHOUSE(
        asset = "quran/translation-fa-islamhouse.txt",
        credit = "ترجمهٔ فارسی گروه اسلام‌هاوس (QuranEnc)"
    ),

    /** al-Tafsir al-Muyassar, King Fahd Glorious Quran Printing Complex. */
    ARABIC_MUYASSAR(
        asset = "quran/translation-ar-muyassar.txt",
        credit = "التفسير الميسر — مجمع الملك فهد لطباعة المصحف الشريف"
    );

    companion object {
        fun forLanguage(language: AppLanguage): QuranTranslation = when (language) {
            AppLanguage.FARSI -> PERSIAN_ISLAMHOUSE
            AppLanguage.ARABIC -> ARABIC_MUYASSAR
        }
    }
}

/** Loads a translation lazily (only the one the app language needs) and keeps it in memory. */
object QuranTranslations {
    private val cache = mutableMapOf<QuranTranslation, Map<String, String>>()

    /** Verse id ("surah:ayah") → text. Call off the main thread on first use. */
    fun load(context: Context, translation: QuranTranslation): Map<String, String> {
        synchronized(cache) { cache[translation]?.let { return it } }
        val parsed = context.applicationContext.assets.open(translation.asset).use(::parse)
        check(parsed.size == QuranRepository.VERSE_COUNT) {
            "Expected ${QuranRepository.VERSE_COUNT} verses in ${translation.asset}, found ${parsed.size}."
        }
        return synchronized(cache) { cache.getOrPut(translation) { parsed } }
    }

    /** Format: `surah|ayah|text` per line; lines starting with `#` are attribution comments. */
    internal fun parse(input: InputStream): Map<String, String> {
        val result = HashMap<String, String>(QuranRepository.VERSE_COUNT)
        input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachLine
            val parts = line.split('|', limit = 3)
            if (parts.size == 3) result["${parts[0]}:${parts[1]}"] = parts[2]
        }
        return result
    }
}

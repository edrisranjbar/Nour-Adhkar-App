package com.example.quran

import android.content.Context
import com.example.ui.language.AppLanguage
import java.io.InputStream

/** A bundled, offline Sunni translation/tafsir of the Quran, one entry per verse. */
enum class QuranTranslation(
    /** Stable id saved in preferences; never rename. */
    val id: String,
    val language: AppLanguage,
    val asset: String,
    /** Name shown in the translation picker. */
    val title: String,
    /** Attribution line shown under the text. */
    val credit: String
) {
    /** Islamhouse.com Persian team, published on QuranEnc (Rowwad Translation Center). Persian default. */
    PERSIAN_ISLAMHOUSE(
        id = "fa_islamhouse",
        language = AppLanguage.FARSI,
        asset = "quran/translation-fa-islamhouse.txt",
        title = "ترجمهٔ گروه اسلام‌هاوس (QuranEnc)",
        credit = "ترجمهٔ فارسی گروه اسلام‌هاوس (QuranEnc)"
    ),

    /** Tafsir-e Nur by Dr. Mostafa Khorramdel (Tanzil fa.khorramdel). */
    PERSIAN_KHORRAMDEL(
        id = "fa_khorramdel",
        language = AppLanguage.FARSI,
        asset = "quran/translation-fa-khorramdel.txt",
        title = "تفسیر نور — دکتر مصطفی خرمدل",
        credit = "تفسیر نور، دکتر مصطفی خرمدل"
    ),

    /** Muhammad Ibrahim Junagarhi, published on QuranEnc. Urdu default. */
    URDU_JUNAGARHI(
        id = "ur_junagarhi",
        language = AppLanguage.URDU,
        asset = "quran/translation-ur-junagarhi.txt",
        title = "ترجمہ مولانا محمد جوناگڑھی (QuranEnc)",
        credit = "اردو ترجمہ: مولانا محمد ابراہیم جوناگڑھی (QuranEnc)"
    ),

    /** al-Tafsir al-Muyassar, King Fahd Glorious Quran Printing Complex. */
    ARABIC_MUYASSAR(
        id = "ar_muyassar",
        language = AppLanguage.ARABIC,
        asset = "quran/translation-ar-muyassar.txt",
        title = "التفسير الميسر",
        credit = "التفسير الميسر — مجمع الملك فهد لطباعة المصحف الشريف"
    );

    companion object {
        /** Dari readers get the Persian translations. */
        fun optionsFor(language: AppLanguage): List<QuranTranslation> {
            val textLanguage = if (language.usesPersianContent) AppLanguage.FARSI else language
            return entries.filter { it.language == textLanguage }
        }

        /** The saved choice if it belongs to [language], otherwise that language's default (first) option. */
        fun forLanguage(language: AppLanguage, savedId: String? = null): QuranTranslation {
            val options = optionsFor(language)
            return options.firstOrNull { it.id == savedId } ?: options.first()
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

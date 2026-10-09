package com.example.quran

import android.content.Context
import com.example.ui.language.AppLanguage
import java.io.InputStream

/**
 * Bundled, offline Sunni tafsirs. Each is stored per surah in `assets/quran/tafsir/<id>/<surah>.txt`
 * so tapping a verse reads one small file. See docs/quran.md for sources.
 */
enum class QuranTafsir(
    /** Stable id: asset folder name and saved preference value; never rename. */
    val id: String,
    /** Language the tafsir text is written in. */
    val textLanguage: AppLanguage,
    /** Short name for the picker chip. */
    val title: String,
    /** Attribution line shown under the text. */
    val credit: String
) {
    PERSIAN_MOKHTASAR(
        id = "fa_mokhtasar",
        textLanguage = AppLanguage.FARSI,
        title = "المختصر (فارسی)",
        credit = "المختصر فی تفسیر القرآن الکریم، ترجمهٔ فارسی — مرکز تفسیر للدراسات القرآنیة"
    ),
    PERSIAN_SAADI(
        id = "fa_saadi",
        textLanguage = AppLanguage.FARSI,
        title = "تفسیر سعدی (فارسی)",
        credit = "تیسیر الکریم الرحمن، عبدالرحمن بن ناصر السعدی — ترجمهٔ فارسی"
    ),
    ARABIC_SAADI(
        id = "ar_saadi",
        textLanguage = AppLanguage.ARABIC,
        title = "السعدي",
        credit = "تيسير الكريم الرحمن في تفسير كلام المنان — عبد الرحمن بن ناصر السعدي"
    ),
    ARABIC_MOKHTASAR(
        id = "ar_mokhtasar",
        textLanguage = AppLanguage.ARABIC,
        title = "المختصر",
        credit = "المختصر في تفسير القرآن الكريم — مركز تفسير للدراسات القرآنية"
    ),
    ARABIC_IBN_KATHIR(
        id = "ar_ibn_kathir",
        textLanguage = AppLanguage.ARABIC,
        title = "ابن كثير",
        credit = "تفسير القرآن العظيم — الحافظ ابن كثير"
    ),
    ARABIC_TABARI(
        id = "ar_tabari",
        textLanguage = AppLanguage.ARABIC,
        title = "الطبري",
        credit = "جامع البيان عن تأويل آي القرآن — الإمام ابن جرير الطبري"
    ),
    ARABIC_QURTUBI(
        id = "ar_qurtubi",
        textLanguage = AppLanguage.ARABIC,
        title = "القرطبي",
        credit = "الجامع لأحكام القرآن — الإمام القرطبي"
    ),
    ARABIC_BAGHAWI(
        id = "ar_baghawi",
        textLanguage = AppLanguage.ARABIC,
        title = "البغوي",
        credit = "معالم التنزيل — الإمام البغوي"
    ),
    ARABIC_JALALAYN(
        id = "ar_jalalayn",
        textLanguage = AppLanguage.ARABIC,
        title = "الجلالين",
        credit = "تفسير الجلالين — جلال الدين المحلي وجلال الدين السيوطي"
    );

    companion object {
        /**
         * Persian and Dari readers get the Persian tafsirs first, then the Arabic ones. Arabic, Urdu and
         * Kurdish readers get the Arabic ones (no Urdu or Kurdish tafsir is bundled yet).
         */
        fun optionsFor(language: AppLanguage): List<QuranTafsir> = when (language) {
            AppLanguage.FARSI, AppLanguage.DARI -> entries.sortedBy { it.textLanguage != AppLanguage.FARSI }
            AppLanguage.ARABIC, AppLanguage.URDU, AppLanguage.KURDISH -> entries.filter { it.textLanguage == AppLanguage.ARABIC }
        }

        /** The saved choice if offered for [language], otherwise its first option. */
        fun forLanguage(language: AppLanguage, savedId: String? = null): QuranTafsir {
            val options = optionsFor(language)
            return options.firstOrNull { it.id == savedId } ?: options.first()
        }
    }
}

/**
 * Commentary for one verse.
 * [fromAyah]..[toAyah] is the verse group the passage covers. [previousOnly] means the tafsir has no
 * entry for the requested verse and this is the passage of the nearest earlier verse.
 */
data class TafsirPassage(
    val text: String,
    val fromAyah: Int,
    val toAyah: Int,
    val previousOnly: Boolean
)

object QuranTafsirs {
    private const val CACHE_SIZE = 4
    // Most recently used surah files; a surah of Ibn Kathir can be ~1 MB of text.
    private val cache = LinkedHashMap<String, SurahTafsir>(CACHE_SIZE, 0.75f, true)

    /** Returns null if the tafsir has nothing for this verse or any earlier verse of the surah. Call off the main thread. */
    fun passage(context: Context, tafsir: QuranTafsir, surah: Int, ayah: Int): TafsirPassage? =
        surahTafsir(context, tafsir, surah).passage(ayah)

    private fun surahTafsir(context: Context, tafsir: QuranTafsir, surah: Int): SurahTafsir {
        val key = "${tafsir.id}/$surah"
        synchronized(cache) { cache[key]?.let { return it } }
        val parsed = context.applicationContext.assets.open("quran/tafsir/$key.txt").use(::parse)
        return synchronized(cache) {
            cache.getOrPut(key) { parsed }.also {
                while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
            }
        }
    }

    /**
     * Format per line: `ayah|text` (paragraph breaks written as `\n`), or `ayah|=owner` when the verse
     * shares the passage stored under verse `owner` of the same surah.
     */
    internal fun parse(input: InputStream): SurahTafsir {
        val texts = HashMap<Int, String>()
        val owners = sortedMapOf<Int, Int>()
        input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
            val separator = line.indexOf('|')
            if (separator <= 0) return@forEachLine
            val ayah = line.substring(0, separator).toIntOrNull() ?: return@forEachLine
            val body = line.substring(separator + 1)
            if (body.startsWith("=")) {
                owners[ayah] = body.substring(1).toInt()
            } else {
                texts[ayah] = body.replace("\\n", "\n")
                owners[ayah] = ayah
            }
        }
        return SurahTafsir(texts, owners)
    }
}

internal class SurahTafsir(
    private val texts: Map<Int, String>,
    /** Verse → verse whose passage it uses. */
    private val owners: java.util.SortedMap<Int, Int>
) {
    fun passage(ayah: Int): TafsirPassage? {
        val present = if (ayah in owners) ayah else owners.headMap(ayah).keys.lastOrNull() ?: return null
        val owner = owners.getValue(present)
        val text = texts[owner] ?: return null
        val group = owners.filterValues { it == owner }.keys
        return TafsirPassage(
            text = text,
            fromAyah = group.first(),
            toAyah = group.last(),
            previousOnly = present != ayah
        )
    }
}

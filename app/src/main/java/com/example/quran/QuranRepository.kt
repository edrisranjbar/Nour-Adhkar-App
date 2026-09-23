package com.example.quran

import android.content.Context
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

data class QuranVerse(
    val surahNumber: Int,
    val surahName: String,
    val verseNumber: Int,
    val text: String,
    val bismillah: String?,
    val pageNumber: Int
) {
    val id: String = "$surahNumber:$verseNumber"
}

data class QuranPage(
    val number: Int,
    val verses: List<QuranVerse>
)

data class QuranSurah(
    val number: Int,
    val name: String,
    val firstPage: Int,
    val verseCount: Int
)

data class QuranCorpus(
    val pages: List<QuranPage>,
    val verses: List<QuranVerse>,
    val surahs: List<QuranSurah>
)

/**
 * Loads the unchanged Tanzil Uthmani text and maps it to the 604 Madani pages.
 * The original text and its required copyright notice are bundled in assets.
 */
object QuranRepository {
    const val PAGE_COUNT = 604
    const val VERSE_COUNT = 6236

    @Volatile
    private var cachedCorpus: QuranCorpus? = null

    fun load(context: Context): QuranCorpus {
        cachedCorpus?.let { return it }
        return synchronized(this) {
            cachedCorpus ?: parse(context.applicationContext).also { cachedCorpus = it }
        }
    }

    private fun parse(context: Context): QuranCorpus {
        val pageByVerse = parsePageIndex(context)
        val verses = parseVerses(context, pageByVerse)
        check(verses.size == VERSE_COUNT) { "Expected $VERSE_COUNT Quran verses, found ${verses.size}." }

        val pages = (1..PAGE_COUNT).map { pageNumber ->
            QuranPage(pageNumber, verses.filter { it.pageNumber == pageNumber })
        }
        check(pages.all { it.verses.isNotEmpty() }) { "Every Quran page must contain at least one verse." }
        val surahs = verses
            .groupBy(QuranVerse::surahNumber)
            .toSortedMap()
            .map { (number, surahVerses) ->
                QuranSurah(
                    number = number,
                    name = surahVerses.first().surahName,
                    firstPage = surahVerses.first().pageNumber,
                    verseCount = surahVerses.size
                )
            }
        check(surahs.size == 114) { "Expected 114 Quran surahs, found ${surahs.size}." }
        return QuranCorpus(pages = pages, verses = verses, surahs = surahs)
    }

    private fun parsePageIndex(context: Context): Map<String, Int> {
        val jsonText = context.assets.open("quran/page-index.json").bufferedReader().use { it.readText() }
        val index = JSONObject(jsonText)
        val result = HashMap<String, Int>(VERSE_COUNT)
        val pageKeys = index.keys()
        while (pageKeys.hasNext()) {
            val pageNumber = pageKeys.next().toInt()
            val ranges = index.getJSONArray(pageNumber.toString())
            for (rangeIndex in 0 until ranges.length()) {
                val range = ranges.getJSONObject(rangeIndex)
                val surah = range.getInt("s")
                for (ayah in range.getInt("a1")..range.getInt("a2")) {
                    result["$surah:$ayah"] = pageNumber
                }
            }
        }
        return result
    }

    private fun parseVerses(context: Context, pageByVerse: Map<String, Int>): List<QuranVerse> {
        return context.assets.open("quran/tanzil-uthmani.xml").use { input ->
            val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
            parseXmlVerses(parser, pageByVerse)
        }
    }

    private fun parseXmlVerses(
        parser: XmlPullParser,
        pageByVerse: Map<String, Int>
    ): List<QuranVerse> {
        val verses = mutableListOf<QuranVerse>()
        var currentSurahNumber = 0
        var currentSurahName = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "sura" -> {
                        currentSurahNumber = parser.getAttributeValue(null, "index").toInt()
                        currentSurahName = parser.getAttributeValue(null, "name")
                    }

                    "aya" -> {
                        val verseNumber = parser.getAttributeValue(null, "index").toInt()
                        val id = "$currentSurahNumber:$verseNumber"
                        val pageNumber = requireNotNull(pageByVerse[id]) { "No page mapping for verse $id." }
                        verses += QuranVerse(
                            surahNumber = currentSurahNumber,
                            surahName = currentSurahName,
                            verseNumber = verseNumber,
                            text = parser.getAttributeValue(null, "text"),
                            bismillah = parser.getAttributeValue(null, "bismillah"),
                            pageNumber = pageNumber
                        )
                    }
                }
            }
            parser.next()
        }
        return verses
    }
}

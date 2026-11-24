package com.example

import com.example.data.model.AdhkarData
import com.example.data.model.UserFeeling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class EmotionalAyahDataTest {
    @Test
    fun everyFeelingHasExactlyTenSourcedAyat() {
        UserFeeling.entries.forEach { feeling ->
            val ayat = AdhkarData.emotionalAyat.filter { it.feeling == feeling }

            assertEquals("Unexpected verse count for ${feeling.storageKey}", 10, ayat.size)
            assertEquals(10, ayat.map { it.id }.distinct().size)
            assertTrue(ayat.all { it.translation.isNotBlank() })
            assertTrue(ayat.all { it.translationSource.contains("تفسیر نور") })
            assertTrue(ayat.all { it.translationSource.contains("مصطفی خرمدل") })
        }
    }

    @Test
    fun everyEntryContainsOneCompleteUneditedVerseAndItsCreditedTranslation() {
        val assets = listOf(File("src/main/assets/quran"), File("app/src/main/assets/quran"))
            .first { it.isDirectory }
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(assets, "tanzil-uthmani.xml"))
        val arabic = mutableMapOf<String, String>()
        val surahs = document.getElementsByTagName("sura")
        for (s in 0 until surahs.length) {
            val surah = surahs.item(s) as org.w3c.dom.Element
            val verses = surah.getElementsByTagName("aya")
            for (a in 0 until verses.length) {
                val verse = verses.item(a) as org.w3c.dom.Element
                arabic["${surah.getAttribute("index")}:${verse.getAttribute("index")}"] =
                    verse.getAttribute("text")
            }
        }
        val translations = File(assets, "translation-fa-khorramdel.txt")
            .readLines(Charsets.UTF_8)
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val parts = line.split('|', limit = 3)
                "${parts[0]}:${parts[1]}" to parts[2]
            }
        val references = AdhkarData.emotionalAyat.map { ayah ->
            val parts = ayah.id.split('-')
            assertEquals(3, parts.size)
            assertEquals(ayah.feeling.storageKey, parts[0])
            val reference = "${parts[1]}:${parts[2]}"
            assertTrue("Missing source verse $reference", reference in arabic)
            assertEquals("Arabic differs from source at $reference", arabic[reference], ayah.text)
            assertEquals("Translation differs from source at $reference", translations[reference], ayah.translation)
            assertTrue("Missing contextual reflection at $reference", ayah.reflection.isNotBlank())
            reference
        }
        assertEquals("Each entry must be a distinct single verse", 60, references.distinct().size)
    }
}

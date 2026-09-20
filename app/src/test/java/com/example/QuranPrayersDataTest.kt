package com.example

import com.example.data.model.AdhkarData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranPrayersDataTest {
    @Test
    fun quranPrayersContainMoreThanFifteenSourcedCards() {
        val category = AdhkarData.categories.single { it.id == "quran_prayers" }
        val prayers = AdhkarData.adhkarList.getValue(category.id)

        assertTrue(prayers.size > 15)
        assertEquals(prayers.size, category.count)
        assertEquals(prayers.size, prayers.map { it.id }.distinct().size)
        assertTrue(prayers.all { it.arabicText.isNotBlank() && it.persianTranslation.isNotBlank() })
        assertTrue(prayers.all { it.source.startsWith("سوره ") && it.source.contains("آیه") })
    }

    @Test
    fun sunnahPrayersContainSourcedCards() {
        val category = AdhkarData.categories.single { it.id == "sunnah_prayers" }
        val prayers = AdhkarData.adhkarList.getValue(category.id)

        assertTrue(category.isEnabled)
        assertTrue(prayers.size >= 100)
        assertEquals(prayers.size, category.count)
        assertEquals(prayers.size, prayers.map { it.id }.distinct().size)
        assertTrue(prayers.all { it.arabicText.isNotBlank() && it.persianTranslation.isNotBlank() && it.source.isNotBlank() })
    }
}

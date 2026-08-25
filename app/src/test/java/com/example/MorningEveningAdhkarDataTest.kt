package com.example

import com.example.data.model.AdhkarData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorningEveningAdhkarDataTest {

    @Test
    fun `morning and evening collections stay complete and use unique ids`() {
        val morning = AdhkarData.adhkarList.getValue("morning")
        val evening = AdhkarData.adhkarList.getValue("evening")

        assertEquals(22, morning.size)
        assertEquals(21, evening.size)
        assertEquals(morning.size, morning.map { it.id }.distinct().size)
        assertEquals(evening.size, evening.map { it.id }.distinct().size)
        assertTrue(morning.all { it.arabicText.isNotBlank() && it.persianTranslation.isNotBlank() })
        assertTrue(evening.all { it.arabicText.isNotBlank() && it.persianTranslation.isNotBlank() })
    }

    @Test
    fun `shared core adhkar exist in both collections with correct repetition`() {
        val morning = AdhkarData.adhkarList.getValue("morning")
        val evening = AdhkarData.adhkarList.getValue("evening")

        listOf(morning, evening).forEach { collection ->
            assertTrue(collection.any {
                it.arabicText.startsWith("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ") && it.targetCount == 100
            })
            assertTrue(collection.any {
                it.arabicText.startsWith("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ") && it.targetCount == 10
            })
            assertTrue(collection.any {
                it.arabicText.startsWith("اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ")
            })
            assertTrue(collection.any {
                it.arabicText.startsWith("اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ")
            })
        }
    }
}

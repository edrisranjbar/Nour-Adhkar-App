package com.example

import com.example.data.model.AdhkarData
import com.example.data.model.UserFeeling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionalAyahDataTest {
    @Test
    fun everyFeelingHasExactlyThreeSourcedAyat() {
        UserFeeling.entries.forEach { feeling ->
            val ayat = AdhkarData.emotionalAyat.filter { it.feeling == feeling }

            assertEquals("Unexpected verse count for ${feeling.storageKey}", 3, ayat.size)
            assertEquals(3, ayat.map { it.id }.distinct().size)
            assertTrue(ayat.all { it.translation.isNotBlank() })
            assertTrue(ayat.all { it.translationSource.contains("تفسیر نور") })
            assertTrue(ayat.all { it.translationSource.contains("مصطفی خرمدل") })
        }
    }
}

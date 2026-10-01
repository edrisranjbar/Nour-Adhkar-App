package com.example

import com.example.data.model.AdhkarData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AfterSalahAdhkarDataTest {

    @Test
    fun `after salah collection is complete and matches its category count`() {
        val items = AdhkarData.adhkarList.getValue("after_salah")
        val category = AdhkarData.categories.first { it.id == "after_salah" }

        assertEquals(category.count, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
        assertTrue(items.all {
            it.arabicText.isNotBlank() && it.persianTranslation.isNotBlank() &&
                it.source.isNotBlank() && it.targetCount > 0
        })
        assertEquals(listOf(3, 33, 33, 33, 1), items.filter { it.id != 2 }.map { it.targetCount })
    }
}

package com.example

import com.example.data.model.ADMIN_DHIKR_ID_OFFSET
import com.example.data.model.AdhkarData
import com.example.data.model.AdminCollection
import com.example.data.model.DhikrItem
import com.example.data.repository.AppAdhkarRepository
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAdhkarTest {
    private val builtInMorning = AdhkarData.adhkarList.getValue("morning")
    private val builtInCategoryCount = AdhkarData.categories.size

    @After fun restoreBuiltIn() = AdhkarData.applyAdminCollections(emptyList())

    private fun dhikr(serverId: Int) = DhikrItem(ADMIN_DHIKR_ID_OFFSET + serverId, "سُبْحَانَ اللَّهِ", "پاک است خدا", 3, "")

    @Test fun itemsForABuiltInCollectionAreAddedAfterItsOwn() {
        AdhkarData.applyAdminCollections(listOf(AdminCollection(1, "morning", "", "", "menu_book", listOf(dhikr(7)))))

        val morning = AdhkarData.adhkarList.getValue("morning")
        assertEquals(builtInMorning + dhikr(7), morning)
        assertEquals(builtInCategoryCount, AdhkarData.categories.size)
        val category = AdhkarData.categories.first { it.id == "morning" }
        assertEquals(builtInMorning.size + 1, AdhkarData.adhkarList.getValue(category.id).size)
    }

    @Test fun aNewCollectionIsAddedWithAStableId() {
        AdhkarData.applyAdminCollections(listOf(AdminCollection(5, null, "دعای باران", "هنگام باران", "mosque", listOf(dhikr(9)))))

        val category = AdhkarData.categories.last()
        assertEquals("app_5", category.id)
        assertEquals("دعای باران", category.title)
        assertEquals(1, category.count)
        assertEquals(listOf(dhikr(9)), AdhkarData.adhkarList.getValue("app_5"))
    }

    @Test fun emptyAndUnknownCollectionsAreIgnoredAndBuiltInContentCanBeRestored() {
        AdhkarData.applyAdminCollections(listOf(
            AdminCollection(2, null, "خالی", "", "menu_book", emptyList()),
            AdminCollection(3, "not_in_this_version", "", "", "menu_book", listOf(dhikr(1)))
        ))
        assertEquals(builtInCategoryCount, AdhkarData.categories.size)
        assertFalse("not_in_this_version" in AdhkarData.adhkarList)

        AdhkarData.applyAdminCollections(emptyList())
        assertEquals(builtInMorning, AdhkarData.adhkarList.getValue("morning"))
    }

    @Test fun parsesTheApiAndSkipsMalformedEntries() {
        val json = JSONArray("""
            [
              {"id": 4, "key": "evening", "title": null, "description": null, "icon": null,
               "items": [{"id": 11, "arabic": "  أَسْتَغْفِرُ اللَّهَ  ", "translation": "آمرزش می‌خواهم", "source": "صحیح مسلم", "count": 100},
                         {"id": 12, "arabic": "", "translation": "بدون متن عربی", "count": 1},
                         {"id": 13, "arabic": "اللَّهُ أَكْبَرُ", "count": 0}]},
              {"id": 6, "key": null, "title": "", "items": [{"id": 20, "arabic": "ذِكْر"}]},
              {"id": 0, "key": "morning", "items": []}
            ]
        """.trimIndent())

        val collections = AppAdhkarRepository.parse(json)

        assertEquals(1, collections.size)
        val evening = collections.single()
        assertEquals("evening", evening.builtInKey)
        assertEquals("menu_book", evening.iconName)
        assertEquals(listOf(ADMIN_DHIKR_ID_OFFSET + 11, ADMIN_DHIKR_ID_OFFSET + 13), evening.items.map { it.id })
        assertEquals("أَسْتَغْفِرُ اللَّهَ", evening.items[0].arabicText)
        assertEquals(100, evening.items[0].targetCount)
        assertEquals(1, evening.items[1].targetCount)
        assertTrue(evening.items[1].persianTranslation.isEmpty())
        assertNull(collections.firstOrNull { it.id == 6 })
    }
}

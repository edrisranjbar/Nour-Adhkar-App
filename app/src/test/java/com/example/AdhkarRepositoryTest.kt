package com.example

import com.example.data.local.DhikrProgressDao
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionDao
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.repository.AdhkarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhkarRepositoryTest {
    @Test
    fun `undo decrements a dhikr by exactly one`() = runTest {
        val progressDao = FakeProgressDao()
        val repository = AdhkarRepository(progressDao, FakeSessionDao())

        repeat(10) {
            repository.incrementDhikrCount("morning", 1, 10)
        }

        repository.decrementDhikrCount("morning", 1)

        assertEquals(9, progressDao.getProgressById("morning_1")?.currentCount)
    }

    @Test
    fun `undo never decrements below zero`() = runTest {
        val progressDao = FakeProgressDao()
        val repository = AdhkarRepository(progressDao, FakeSessionDao())

        repository.incrementDhikrCount("morning", 1, 10)
        repository.decrementDhikrCount("morning", 1)
        repository.decrementDhikrCount("morning", 1)

        assertEquals(null, progressDao.getProgressById("morning_1"))
    }

    @Test
    fun `sleep adhkar are recorded as one collection history entry`() = runTest {
        assertGroupedHistory("sleep", "اذکار خواب")
    }

    @Test
    fun `daily adhkar are recorded as one collection history entry`() = runTest {
        assertGroupedHistory("daily", "اذکار روزانه")
    }

    @Test
    fun `ramadan adhkar are recorded as one collection history entry`() = runTest {
        assertGroupedHistory("ramadan", "اذکار ماه رمضان")
    }

    @Test
    fun `every app adhkar category is recorded only as one whole collection`() = runTest {
        AdhkarData.categories.forEach { category ->
            val items = AdhkarData.adhkarList.getValue(category.id)
            if (items.isNotEmpty()) {
                assertGroupedHistory(
                    categoryId = category.id,
                    expectedTitle = if (category.id == "sleep") "اذکار خواب" else category.title
                )
            }
        }
    }

    @Test
    fun `completed collection is not added to history twice`() = runTest {
        val progressDao = FakeProgressDao()
        val sessionDao = FakeSessionDao()
        val repository = AdhkarRepository(progressDao, sessionDao)
        val item = AdhkarData.adhkarList.getValue("waking_up").single()

        repeat(item.targetCount + 1) {
            repository.incrementDhikrCount("waking_up", item.id, item.targetCount)
        }

        assertEquals(1, sessionDao.sessions.size)
    }

    private suspend fun assertGroupedHistory(categoryId: String, expectedTitle: String) {
        val progressDao = FakeProgressDao()
        val sessionDao = FakeSessionDao()
        val repository = AdhkarRepository(progressDao, sessionDao)
        val categoryAdhkar = AdhkarData.adhkarList.getValue(categoryId)

        categoryAdhkar.forEachIndexed { itemIndex, item ->
            repeat(item.targetCount) {
                repository.incrementDhikrCount(categoryId, item.id, item.targetCount)
            }
            if (itemIndex < categoryAdhkar.lastIndex) {
                assertTrue(sessionDao.sessions.isEmpty())
            }
        }

        assertEquals(1, sessionDao.sessions.size)
        assertEquals(expectedTitle, sessionDao.sessions.single().dhikrName)
        assertEquals(1, sessionDao.sessions.single().count)
    }
}

private class FakeProgressDao : DhikrProgressDao {
    private val progress = linkedMapOf<String, DhikrProgressEntity>()

    override fun getAllProgress(): Flow<List<DhikrProgressEntity>> = flowOf(progress.values.toList())

    override fun getProgressByCategory(categoryId: String): Flow<List<DhikrProgressEntity>> =
        flowOf(progress.values.filter { it.categoryId == categoryId })

    override suspend fun getProgressById(id: String): DhikrProgressEntity? = progress[id]

    override suspend fun insertOrUpdateProgress(progress: DhikrProgressEntity) {
        this.progress[progress.id] = progress
    }

    override suspend fun deleteProgressForCategory(categoryId: String) {
        progress.entries.removeAll { it.value.categoryId == categoryId }
    }

    override suspend fun deleteCompletedProgressForCategory(categoryId: String) {
        progress.entries.removeAll {
            it.value.categoryId == categoryId && it.value.currentCount >= it.value.targetCount
        }
    }

    override suspend fun deleteProgressById(id: String) {
        progress.remove(id)
    }

    override suspend fun deleteAllProgress() {
        progress.clear()
    }
}

private class FakeSessionDao : TasbihSessionDao {
    val sessions = mutableListOf<TasbihSessionEntity>()

    override fun getRecentSessions(): Flow<List<TasbihSessionEntity>> = flowOf(sessions)

    override suspend fun insertSession(session: TasbihSessionEntity) {
        sessions += session
    }

    override suspend fun deleteSessionById(id: Int) {
        sessions.removeAll { it.id == id }
    }

    override suspend fun clearHistory() {
        sessions.clear()
    }
}

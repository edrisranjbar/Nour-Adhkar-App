package com.example.data.repository

import com.example.data.local.DhikrProgressDao
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionDao
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.model.DhikrItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdhkarRepository(
    private val dhikrProgressDao: DhikrProgressDao,
    private val tasbihSessionDao: TasbihSessionDao
) {
    private val progressMutex = Mutex()
    private val groupedHistoryTitles = AdhkarData.categories.associate { it.id to it.title } + mapOf(
        "sleep" to "اذکار خواب"
    )

    fun getAdhkarByCategory(categoryId: String): Flow<List<DhikrItem>> {
        val staticList = AdhkarData.adhkarList[categoryId] ?: emptyList()
        return dhikrProgressDao.getProgressByCategory(categoryId).map { progressEntities ->
            val progressMap = progressEntities.associate { it.dhikrId to it.currentCount }
            staticList.map { dhikr ->
                dhikr.copy(currentCount = progressMap[dhikr.id] ?: 0)
            }
        }.catch {
            emit(staticList)
        }.onStart {
            emit(staticList)
        }
    }

    suspend fun incrementDhikrCount(categoryId: String, dhikrId: Int, targetCount: Int): Boolean = progressMutex.withLock {
        val id = "${categoryId}_${dhikrId}"
        val existingProgress = dhikrProgressDao.getProgressById(id)
        val categoryItems = AdhkarData.adhkarList[categoryId].orEmpty()
        val progressBeforeIncrement = dhikrProgressDao.getProgressByCategory(categoryId).first()
            .associateBy { it.dhikrId }
        val wasCategoryCompleted = categoryItems.isNotEmpty() && categoryItems.all { item ->
            (progressBeforeIncrement[item.id]?.currentCount ?: 0) >= item.targetCount
        }

        val newCount = (existingProgress?.currentCount ?: 0) + 1
        val progressEntity = DhikrProgressEntity(
            id = id,
            categoryId = categoryId,
            dhikrId = dhikrId,
            currentCount = newCount,
            targetCount = targetCount,
            lastUpdated = System.currentTimeMillis()
        )
        dhikrProgressDao.insertOrUpdateProgress(progressEntity)

        val progressById = dhikrProgressDao.getProgressByCategory(categoryId).first()
            .associateBy { it.dhikrId }
        val categoryCompleted = categoryItems.isNotEmpty() && categoryItems.all { item ->
            (progressById[item.id]?.currentCount ?: 0) >= item.targetCount
        }
        if (!wasCategoryCompleted && categoryCompleted) {
            recordGroupedCategoryCompletion(categoryId, System.currentTimeMillis())
        }
        categoryCompleted
    }

    suspend fun resetCategoryProgress(categoryId: String) {
        dhikrProgressDao.deleteProgressForCategory(categoryId)
    }

    suspend fun completeCategory(categoryId: String, recordHistory: Boolean) = progressMutex.withLock {
        val items = AdhkarData.adhkarList[categoryId].orEmpty()
        val now = System.currentTimeMillis()
        items.forEach { item ->
            dhikrProgressDao.insertOrUpdateProgress(
                DhikrProgressEntity(
                    id = "${categoryId}_${item.id}",
                    categoryId = categoryId,
                    dhikrId = item.id,
                    currentCount = item.targetCount,
                    targetCount = item.targetCount,
                    lastUpdated = now
                )
            )
        }

        if (recordHistory && categoryId in groupedHistoryTitles) {
            recordGroupedCategoryCompletion(categoryId, now)
        }
    }

    private suspend fun recordGroupedCategoryCompletion(categoryId: String, timestamp: Long) {
        val historyTitle = groupedHistoryTitles[categoryId] ?: return
        tasbihSessionDao.insertSession(
            TasbihSessionEntity(
                dhikrName = historyTitle,
                count = 1,
                timestamp = timestamp
            )
        )
    }

    suspend fun resetCompletedProgress(categoryId: String) {
        dhikrProgressDao.deleteCompletedProgressForCategory(categoryId)
    }

    suspend fun resetSingleDhikr(categoryId: String, dhikrId: Int) {
        val id = "${categoryId}_${dhikrId}"
        dhikrProgressDao.deleteProgressById(id)
    }

    suspend fun decrementDhikrCount(categoryId: String, dhikrId: Int) = progressMutex.withLock {
        val id = "${categoryId}_${dhikrId}"
        val existingProgress = dhikrProgressDao.getProgressById(id) ?: return@withLock
        val newCount = (existingProgress.currentCount - 1).coerceAtLeast(0)

        if (newCount == 0) {
            dhikrProgressDao.deleteProgressById(id)
        } else {
            dhikrProgressDao.insertOrUpdateProgress(
                existingProgress.copy(
                    currentCount = newCount,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun getAllProgress(): Flow<List<DhikrProgressEntity>> {
        return dhikrProgressDao.getAllProgress()
    }

    suspend fun resetAllProgress() {
        dhikrProgressDao.deleteAllProgress()
    }

    fun getRecentTasbihSessions(): Flow<List<TasbihSessionEntity>> {
        return tasbihSessionDao.getRecentSessions()
    }

    suspend fun saveTasbihSession(dhikrName: String, count: Int) {
        if (count > 0) {
            tasbihSessionDao.insertSession(
                TasbihSessionEntity(
                    dhikrName = dhikrName,
                    count = count
                )
            )
        }
    }

    suspend fun deleteTasbihSession(id: Int) {
        tasbihSessionDao.deleteSessionById(id)
    }

    suspend fun clearTasbihHistory() {
        tasbihSessionDao.clearHistory()
    }
}

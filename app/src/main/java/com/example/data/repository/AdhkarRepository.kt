package com.example.data.repository

import com.example.data.local.DhikrProgressDao
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.TasbihSessionDao
import com.example.data.local.TasbihSessionEntity
import com.example.data.model.AdhkarData
import com.example.data.model.DhikrItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AdhkarRepository(
    private val dhikrProgressDao: DhikrProgressDao,
    private val tasbihSessionDao: TasbihSessionDao
) {

    fun getAdhkarByCategory(categoryId: String): Flow<List<DhikrItem>> {
        val staticList = AdhkarData.adhkarList[categoryId] ?: emptyList()
        return dhikrProgressDao.getProgressByCategory(categoryId).map { progressEntities ->
            val progressMap = progressEntities.associate { it.dhikrId to it.currentCount }
            staticList.map { dhikr ->
                dhikr.copy(currentCount = progressMap[dhikr.id] ?: 0)
            }
        }
    }

    suspend fun incrementDhikrCount(categoryId: String, dhikrId: Int, targetCount: Int) {
        val id = "${categoryId}_${dhikrId}"
        val existingProgress = dhikrProgressDao.getProgressById(id)

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
    }

    suspend fun resetCategoryProgress(categoryId: String) {
        dhikrProgressDao.deleteProgressForCategory(categoryId)
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

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrProgressDao {
    @Query("SELECT * FROM dhikr_progress")
    fun getAllProgress(): Flow<List<DhikrProgressEntity>>

    @Query("SELECT * FROM dhikr_progress WHERE categoryId = :categoryId")
    fun getProgressByCategory(categoryId: String): Flow<List<DhikrProgressEntity>>

    @Query("SELECT * FROM dhikr_progress WHERE id = :id")
    suspend fun getProgressById(id: String): DhikrProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: DhikrProgressEntity)

    @Query("DELETE FROM dhikr_progress WHERE categoryId = :categoryId")
    suspend fun deleteProgressForCategory(categoryId: String)

    @Query("DELETE FROM dhikr_progress WHERE id = :id")
    suspend fun deleteProgressById(id: String)

    @Query("DELETE FROM dhikr_progress")
    suspend fun deleteAllProgress()
}

@Dao
interface TasbihSessionDao {
    @Query("SELECT * FROM tasbih_sessions ORDER BY timestamp DESC LIMIT 500")
    fun getRecentSessions(): Flow<List<TasbihSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TasbihSessionEntity)

    @Query("DELETE FROM tasbih_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM tasbih_sessions")
    suspend fun clearHistory()
}

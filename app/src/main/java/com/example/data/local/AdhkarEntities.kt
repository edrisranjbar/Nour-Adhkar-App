package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dhikr_progress")
data class DhikrProgressEntity(
    @PrimaryKey val id: String, // format: "category_id" (e.g. "morning_2")
    val categoryId: String,
    val dhikrId: Int,
    val currentCount: Int,
    val targetCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbih_sessions")
data class TasbihSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dhikrName: String,
    val count: Int,
    val timestamp: Long = System.currentTimeMillis()
)

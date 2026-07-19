package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DhikrProgressEntity::class, TasbihSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AdhkarDatabase : RoomDatabase() {

    abstract fun dhikrProgressDao(): DhikrProgressDao
    abstract fun tasbihSessionDao(): TasbihSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AdhkarDatabase? = null

        fun getDatabase(context: Context): AdhkarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AdhkarDatabase::class.java,
                    "nour_adhkar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

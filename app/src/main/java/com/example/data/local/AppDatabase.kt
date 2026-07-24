package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.NewsArticle
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserNotification

@Database(
    entities = [NewsArticle::class, UserNotification::class, SearchHistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gundem_ai_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

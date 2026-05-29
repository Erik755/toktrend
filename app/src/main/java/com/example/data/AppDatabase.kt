package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TikTokAccount::class, TikTokTrend::class, VideoPost::class],
    version = 2,
    exportSchema = false
)
abstract class TokTrendDatabase : RoomDatabase() {
    abstract fun accountDao(): TikTokAccountDao
    abstract fun trendDao(): TikTokTrendDao
    abstract fun postDao(): VideoPostDao

    companion object {
        @Volatile
        private var INSTANCE: TokTrendDatabase? = null

        fun getDatabase(context: Context): TokTrendDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TokTrendDatabase::class.java,
                    "tok_trend_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

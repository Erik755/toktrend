package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TikTokAccountDao {
    @Query("SELECT * FROM tiktok_accounts")
    fun getAllAccounts(): Flow<List<TikTokAccount>>

    @Query("SELECT * FROM tiktok_accounts WHERE username = :username LIMIT 1")
    suspend fun getAccountByUsername(username: String): TikTokAccount?

    @Query("SELECT * FROM tiktok_accounts WHERE isConnected = 1 LIMIT 1")
    suspend fun getConnectedAccount(): TikTokAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: TikTokAccount)

    @Update
    suspend fun updateAccount(account: TikTokAccount)

    @Query("UPDATE tiktok_accounts SET isConnected = 0")
    suspend fun disconnectAllAccounts()

    @Query("DELETE FROM tiktok_accounts WHERE username = :username")
    suspend fun deleteAccount(username: String)
}

@Dao
interface TikTokTrendDao {
    @Query("SELECT * FROM tiktok_trends ORDER BY score DESC, analyzedAt DESC")
    fun getAllTrends(): Flow<List<TikTokTrend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrend(trend: TikTokTrend): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrends(trends: List<TikTokTrend>)

    @Query("DELETE FROM tiktok_trends WHERE id = :id")
    suspend fun deleteTrendById(id: Int)

    @Query("DELETE FROM tiktok_trends")
    suspend fun clearAllTrends()
}

@Dao
interface VideoPostDao {
    @Query("SELECT * FROM video_posts ORDER BY scheduledTime ASC")
    fun getAllPosts(): Flow<List<VideoPost>>

    @Query("SELECT * FROM video_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Int): VideoPost?

    @Query("SELECT * FROM video_posts WHERE status = :status ORDER BY scheduledTime ASC")
    fun getPostsByStatus(status: String): Flow<List<VideoPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: VideoPost): Long

    @Update
    suspend fun updatePost(post: VideoPost)

    @Query("DELETE FROM video_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)
}

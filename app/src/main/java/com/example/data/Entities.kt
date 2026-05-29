package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tiktok_accounts")
data class TikTokAccount(
    @PrimaryKey val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isConnected: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiry: Long = 0,
    val connectedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tiktok_trends")
data class TikTokTrend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hashtag: String,
    val title: String,
    val description: String,
    val score: Int, // 0 to 100 representing trend strength
    val category: String, // e.g. "Tech", "Comedy", "Music", "Beauty"
    val visualStyle: String, // description of recommended vibe
    val musicRecommendation: String,
    val analyzedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_posts")
data class VideoPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trendId: Int?,
    val title: String,
    val caption: String,
    val hashtags: String, // comma-separated or space-separated
    val scheduledTime: Long, // epoch millis
    val status: String, // "DRAFT", "SCHEDULED", "PUBLISHING", "PUBLISHED", "FAILED"
    val scenesJson: String, // serialized scene data for structured video playback
    val videoFilePath: String? = null,
    val errorMessage: String? = null,
    val publishedVideoId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoScene(
    val sceneNumber: Int,
    val visualPrompt: String,
    val overlayText: String,
    val durationSeconds: Int,
    val voiceoverText: String
)

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class Video(
    @PrimaryKey val id: String, // YouTube Video ID (unique)
    val title: String,
    val url: String,
    val category: String = "Cartoons", // "Cartoons", "Music", "Learning", "Favorites"
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_status")
data class WatchStatus(
    @PrimaryKey val videoId: String,
    val title: String,
    val progressSeconds: Float = 0f,
    val durationSeconds: Float = 0f,
    val lastWatched: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val completed: Boolean = false
)

@Entity(tableName = "analytics_events")
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventType: String, // "PLAY", "PAUSE", "COMPLETE", "PROGRESS"
    val videoId: String,
    val videoTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSpentSeconds: Int = 0
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

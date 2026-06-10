package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedAt DESC")
    fun getAllVideos(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): Video?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<Video>)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)

    @Query("DELETE FROM videos")
    suspend fun clearVideos()
}

@Dao
interface WatchStatusDao {
    @Query("SELECT * FROM watch_status ORDER BY lastWatched DESC")
    fun getAllWatchStatuses(): Flow<List<WatchStatus>>

    @Query("SELECT * FROM watch_status WHERE videoId = :videoId")
    suspend fun getWatchStatusForVideo(videoId: String): WatchStatus?

    @Query("SELECT * FROM watch_status WHERE videoId = :videoId")
    fun getWatchStatusForVideoFlow(videoId: String): Flow<WatchStatus?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchStatus(status: WatchStatus)

    @Query("DELETE FROM watch_status")
    suspend fun clearWatchStatuses()
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC")
    fun getAllAnalyticsEvents(): Flow<List<AnalyticsEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvent(event: AnalyticsEvent)

    @Query("DELETE FROM analytics_events")
    suspend fun clearAnalyticsEvents()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSettingByKey(key: String): AppSetting?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun getSettingFlowByKey(key: String): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}

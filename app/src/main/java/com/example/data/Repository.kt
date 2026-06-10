package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KidsYoutubeRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val watchStatusDao: WatchStatusDao,
    private val analyticsDao: AnalyticsDao,
    private val appSettingDao: AppSettingDao
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var firebaseDatabase: FirebaseDatabase? = null
    private var activeSyncListener: ValueEventListener? = null

    val allVideos: Flow<List<Video>> = videoDao.getAllVideos()
    val allWatchStatuses: Flow<List<WatchStatus>> = watchStatusDao.getAllWatchStatuses()
    val allAnalyticsEvents: Flow<List<AnalyticsEvent>> = analyticsDao.getAllAnalyticsEvents()

    init {
        // Boostrap the system
        coroutineScope.launch {
            // Check if we already have videos, if not inject safe defaults so app works instantly
            val currentVideos = videoDao.getAllVideos().first()
            if (currentVideos.isEmpty()) {
                videoDao.insertVideos(AppDatabase.getPreloadedVideos())
            }
            
            // Try initializing Firebase from stored settings
            tryAutoInitializeFirebase()
        }
    }

    suspend fun getFirebaseConfig(): Map<String, String> {
        val url = appSettingDao.getSettingByKey("firebase_db_url")?.value ?: ""
        val apiKey = appSettingDao.getSettingByKey("firebase_api_key")?.value ?: ""
        val projectId = appSettingDao.getSettingByKey("firebase_project_id")?.value ?: ""
        val appId = appSettingDao.getSettingByKey("firebase_app_id")?.value ?: ""
        val enabled = appSettingDao.getSettingByKey("firebase_enabled")?.value ?: "false"
        
        return mapOf(
            "url" to url,
            "apiKey" to apiKey,
            "projectId" to projectId,
            "appId" to appId,
            "enabled" to enabled
        )
    }

    suspend fun saveFirebaseConfig(
        url: String,
        apiKey: String,
        projectId: String,
        appId: String,
        enabled: Boolean
    ): Boolean {
        appSettingDao.insertSetting(AppSetting("firebase_db_url", url))
        appSettingDao.insertSetting(AppSetting("firebase_api_key", apiKey))
        appSettingDao.insertSetting(AppSetting("firebase_project_id", projectId))
        appSettingDao.insertSetting(AppSetting("firebase_app_id", appId))
        appSettingDao.insertSetting(AppSetting("firebase_enabled", enabled.toString()))

        return if (enabled && url.isNotEmpty()) {
            setupFirebase(url, apiKey, projectId, appId)
        } else {
            disableFirebaseSync()
            true
        }
    }

    private suspend fun tryAutoInitializeFirebase() {
        val config = getFirebaseConfig()
        if (config["enabled"] == "true" && !config["url"].isNullOrEmpty()) {
            setupFirebase(
                config["url"] ?: "",
                config["apiKey"] ?: "",
                config["projectId"] ?: "",
                config["appId"] ?: ""
            )
        }
    }

    private fun setupFirebase(
        url: String,
        apiKey: String,
        projectId: String,
        appId: String
    ): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            val firebaseApp = if (apps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(if (appId.isEmpty()) "1:999:android:999" else appId)
                    .setApiKey(if (apiKey.isEmpty()) "AIzaSyDummyKey" else apiKey)
                    .setProjectId(if (projectId.isEmpty()) "dummy-project" else projectId)
                    .setDatabaseUrl(url)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                apps[0]
            }

            firebaseDatabase = FirebaseDatabase.getInstance(firebaseApp, url)
            startFirebaseSync()
            Log.d("FirebaseSync", "Firebase dynamically initialized successfully")
            true
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Failed to initialize Firebase dynamically", e)
            false
        }
    }

    private fun disableFirebaseSync() {
        try {
            activeSyncListener?.let {
                firebaseDatabase?.getReference("videos")?.removeEventListener(it)
            }
            activeSyncListener = null
            firebaseDatabase = null
            Log.d("FirebaseSync", "Firebase sync detached successfully")
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error disabling sync", e)
        }
    }

    private fun startFirebaseSync() {
        val db = firebaseDatabase ?: return
        
        // Remove existing listener if any
        activeSyncListener?.let {
            db.getReference("videos").removeEventListener(it)
        }

        val videosRef = db.getReference("videos")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch {
                    val remoteVideos = mutableListOf<Video>()
                    
                    // Direct support for structured children
                    if (snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            val id = child.child("id").getValue(String::class.java)
                                ?: child.key // Fallback to key
                                ?: continue
                            
                            val title = child.child("title").getValue(String::class.java) ?: "Cool Kid Video"
                            val url = child.child("url").getValue(String::class.java) ?: ""
                            val category = child.child("category").getValue(String::class.java) ?: "Cartoons"
                            
                            if (url.isNotEmpty()) {
                                remoteVideos.add(Video(id = id, title = title, url = url, category = category))
                            }
                        }
                    } else {
                        // Direct support if they store a simple list of links
                        val rawData = snapshot.value
                        if (rawData is List<*>) {
                            rawData.forEachIndexed { index, item ->
                                val link = item?.toString() ?: ""
                                if (link.isNotEmpty()) {
                                    val videoId = extractYoutubeId(link)
                                    if (videoId != null) {
                                        remoteVideos.add(
                                            Video(
                                                id = videoId,
                                                title = "Remote Video #${index + 1}",
                                                url = link,
                                                category = "Cartoons"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (remoteVideos.isNotEmpty()) {
                        videoDao.clearVideos()
                        videoDao.insertVideos(remoteVideos)
                        Log.d("FirebaseSync", "Downloaded ${remoteVideos.size} videos from Firebase remote")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Firebase remote sync cancelled", error.toException())
            }
        }

        activeSyncListener = listener
        videosRef.addValueEventListener(listener)
    }

    // Pushes local additions or changes to Firebase Realtime Database
    suspend fun addVideo(video: Video) {
        videoDao.insertVideo(video)
        pushVideosToFirebase()
    }

    suspend fun deleteVideo(videoId: String) {
        videoDao.deleteVideoById(videoId)
        pushVideosToFirebase()
    }

    private suspend fun pushVideosToFirebase() {
        val db = firebaseDatabase ?: return
        val videos = videoDao.getAllVideos().first()
        withContext(Dispatchers.IO) {
            db.getReference("videos").setValue(videos)
        }
    }

    // Tracks watch status both locally and on Firebase
    suspend fun saveWatchStatus(status: WatchStatus) {
        watchStatusDao.insertWatchStatus(status)
        
        // Push progress to firebase if enabled so parent can monitor from console/elsewhere
        val db = firebaseDatabase ?: return
        withContext(Dispatchers.IO) {
            val deviceId = getDeviceId()
            // Clean paths for Firebase nodes
            val cleanId = status.videoId.replace(".", "_").replace("$", "_")
            db.getReference("watch_status").child(deviceId).child(cleanId).setValue(status)
        }
    }

    suspend fun clearWatchStatus() {
        watchStatusDao.clearWatchStatuses()
        val db = firebaseDatabase ?: return
        withContext(Dispatchers.IO) {
            db.getReference("watch_status").child(getDeviceId()).removeValue()
        }
    }

    // Logs analytics events both locally and to Firebase Database metrics
    suspend fun logAnalyticsEvent(event: AnalyticsEvent) {
        analyticsDao.insertAnalyticsEvent(event)
        
        // Dynamic push to firebase database for real-time live analytics dashboards
        val db = firebaseDatabase ?: return
        withContext(Dispatchers.IO) {
            val deviceId = getDeviceId()
            val eventRef = db.getReference("analytics").child(deviceId).push()
            eventRef.setValue(event)
        }
    }

    suspend fun clearAnalytics() {
        analyticsDao.clearAnalyticsEvents()
        val db = firebaseDatabase ?: return
        withContext(Dispatchers.IO) {
            db.getReference("analytics").child(getDeviceId()).removeValue()
        }
    }

    private fun getDeviceId(): String {
        return context.getSharedPreferences("app_device", Context.MODE_PRIVATE)
            .getString("device_id", null) ?: run {
                val newId = "device_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                context.getSharedPreferences("app_device", Context.MODE_PRIVATE)
                    .edit().putString("device_id", newId).apply()
                newId
            }
    }

    fun extractYoutubeId(url: String): String? {
        val cleanUrl = url.trim()
        return when {
            cleanUrl.contains("youtu.be/") -> {
                cleanUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            }
            cleanUrl.contains("youtube.com/watch") -> {
                val queryParams = cleanUrl.substringAfter("watch?").split("&")
                queryParams.firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
            }
            cleanUrl.contains("youtube.com/shorts/") -> {
                cleanUrl.substringAfter("youtube.com/shorts/").substringBefore("?").substringBefore("/")
            }
            cleanUrl.contains("youtube.com/embed/") -> {
                cleanUrl.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("/")
            }
            else -> {
                if (cleanUrl.length == 11 && !cleanUrl.contains("/")) cleanUrl else null
            }
        }
    }
}

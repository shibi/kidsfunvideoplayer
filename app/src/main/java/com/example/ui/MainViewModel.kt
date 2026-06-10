package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParentChallenge(
    val question: String,
    val answer: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = KidsYoutubeRepository(
        context = application,
        videoDao = database.videoDao(),
        watchStatusDao = database.watchStatusDao(),
        analyticsDao = database.analyticsDao(),
        appSettingDao = database.appSettingDao()
    )

    // Observable states from Local Database + Sync Engine
    val videos: StateFlow<List<Video>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchStatuses: StateFlow<List<WatchStatus>> = repository.allWatchStatuses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val analyticsEvents: StateFlow<List<AnalyticsEvent>> = repository.allAnalyticsEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected Video Flow
    private val _currentPlayingVideo = MutableStateFlow<Video?>(null)
    val currentPlayingVideo: StateFlow<Video?> = _currentPlayingVideo.asStateFlow()

    // Filter Category: "All", "Cartoons", "Music", "Learning"
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Filtered Video State
    val filteredVideos: StateFlow<List<Video>> = combine(videos, _selectedCategory) { list, category ->
        if (category == "All") list else list.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Parental security states
    var isParentGateShowing by mutableStateOf(false)
        private set
    var isParentModeActive by mutableStateOf(false)
        private set
    var parentChallenge by mutableStateOf<ParentChallenge?>(null)
        private set
    var parentGateError by mutableStateOf(false)
        private set

    // Active Firebase config bindings inside Parent view, updated reactively on loading
    private val _firebaseConfigState = MutableStateFlow<Map<String, String>>(emptyMap())
    val firebaseConfigState = _firebaseConfigState.asStateFlow()

    init {
        loadFirebaseConfig()
    }

    private fun loadFirebaseConfig() {
        viewModelScope.launch {
            _firebaseConfigState.value = repository.getFirebaseConfig()
        }
    }

    // Playback control
    fun playVideo(video: Video) {
        _currentPlayingVideo.value = video
        logSessionEvent("PLAY", video)
        
        // Update local play count in WatchStatus
        viewModelScope.launch {
            val status = database.watchStatusDao().getWatchStatusForVideo(video.id)
            val updatedStatus = WatchStatus(
                videoId = video.id,
                title = video.title,
                progressSeconds = status?.progressSeconds ?: 0f,
                durationSeconds = status?.durationSeconds ?: 0f,
                playCount = (status?.playCount ?: 0) + 1,
                lastWatched = System.currentTimeMillis(),
                completed = status?.completed ?: false
            )
            repository.saveWatchStatus(updatedStatus)
        }
    }

    fun stopVideo() {
        val video = _currentPlayingVideo.value
        _currentPlayingVideo.value = null
        if (video != null) {
            logSessionEvent("PAUSE", video)
        }
    }

    // Update ongoing video watch statuses
    fun updateProgress(currentTime: Float, duration: Float) {
        val video = _currentPlayingVideo.value ?: return
        if (duration <= 0f) return
        
        viewModelScope.launch {
            val status = database.watchStatusDao().getWatchStatusForVideo(video.id)
            val isCompleted = currentTime >= (duration * 0.95f) || (status?.completed ?: false)
            
            val updatedStatus = WatchStatus(
                videoId = video.id,
                title = video.title,
                progressSeconds = currentTime,
                durationSeconds = duration,
                playCount = status?.playCount ?: 1,
                lastWatched = System.currentTimeMillis(),
                completed = isCompleted
            )
            repository.saveWatchStatus(updatedStatus)

            // Trigger complete log only if progress changes state
            if (isCompleted && !(status?.completed ?: false)) {
                logSessionEvent("COMPLETE", video)
            }
        }
    }

    private fun logSessionEvent(type: String, video: Video) {
        viewModelScope.launch {
            repository.logAnalyticsEvent(
                AnalyticsEvent(
                    eventType = type,
                    videoId = video.id,
                    videoTitle = video.title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Parent controls
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun showParentGate() {
        isParentGateShowing = true
        parentGateError = false
        generateParentGateChallenge()
    }

    fun closeParentGate() {
        isParentGateShowing = false
        parentChallenge = null
        parentGateError = false
    }

    fun exitParentMode() {
        isParentModeActive = false
    }

    private fun generateParentGateChallenge() {
        val firstNum = (3..9).random()
        val secondNum = (3..9).random()
        val operation = if (firstNum > secondNum && (firstNum % secondNum == 0)) "/" else "x"
        
        val questionText: String
        val answerText: String
        
        if (operation == "x") {
            questionText = "$firstNum  $operation  $secondNum = ?"
            answerText = (firstNum * secondNum).toString()
        } else {
            questionText = "$firstNum  +  $secondNum = ?"
            answerText = (firstNum + secondNum).toString()
        }
        
        parentChallenge = ParentChallenge(questionText, answerText)
    }

    fun submitParentGateAnswer(answer: String) {
        val challenge = parentChallenge
        if (challenge != null && answer.trim() == challenge.answer) {
            isParentModeActive = true
            isParentGateShowing = false
            parentChallenge = null
            parentGateError = false
            loadFirebaseConfig() // reload settings to populate form fields
        } else {
            parentGateError = true
        }
    }

    // Curation Management
    fun addNewVideo(title: String, url: String, category: String): String? {
        val extractedId = repository.extractYoutubeId(url)
            ?: return "Invalid YouTube Link. Please provide a standard YouTube or Shorts watch link."
        
        val cleanTitle = title.trim().ifEmpty { "Fun Kids Video" }
        val newVideo = Video(
            id = extractedId,
            title = cleanTitle,
            url = url.trim(),
            category = category
        )

        viewModelScope.launch {
            repository.addVideo(newVideo)
        }
        return null // success
    }

    fun removeVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideo(videoId)
        }
    }

    // Save developer dashboard inputs
    fun saveSettings(url: String, apiKey: String, projectId: String, appId: String, enabled: Boolean) {
        viewModelScope.launch {
            val syncSuccess = repository.saveFirebaseConfig(
                url = url.trim(),
                apiKey = apiKey.trim(),
                projectId = projectId.trim(),
                appId = appId.trim(),
                enabled = enabled
            )
            if (syncSuccess) {
                loadFirebaseConfig()
            }
        }
    }

    fun clearLocalStats() {
        viewModelScope.launch {
            repository.clearWatchStatus()
            repository.clearAnalytics()
        }
    }
}

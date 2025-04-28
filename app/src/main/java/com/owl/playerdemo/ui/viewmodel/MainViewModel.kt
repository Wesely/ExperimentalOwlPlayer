package com.owl.playerdemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owl.playerdemo.data.repository.VideoDownloadRepository
import com.owl.playerdemo.data.service.PexelsService
import com.owl.playerdemo.data.util.NetworkConnectivityManager
import com.owl.playerdemo.model.DownloadedVideo
import com.owl.playerdemo.model.User
import com.owl.playerdemo.model.VideoFile
import com.owl.playerdemo.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pexelsService: PexelsService,
    private val networkConnectivityManager: NetworkConnectivityManager,
    private val downloadRepository: VideoDownloadRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    // Track downloads in progress (videoId to progress percentage 0-100)
    private val _downloadsInProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadsInProgress: StateFlow<Map<Int, Float>> = _downloadsInProgress

    init {
        observeNetworkConnectivity()
        observeDownloadProgress()
    }
    
    private fun observeNetworkConnectivity() {
        viewModelScope.launch {
            networkConnectivityManager.isNetworkAvailable.collectLatest { isAvailable ->
                _isNetworkAvailable.value = isAvailable
                if (isAvailable && _videos.value.isEmpty()) {
                    // If network becomes available and we don't have any videos, fetch them
                    fetchVideos()
                }
            }
        }
    }
    
    /**
     * Observe download progress from repository
     */
    private fun observeDownloadProgress() {
        viewModelScope.launch {
            downloadRepository.downloadProgress.collectLatest { progressMap ->
                _downloadsInProgress.value = progressMap
            }
        }
    }
    
    /**
     * Download a video using Fetch2 via repository
     */
    fun downloadVideo(videoId: Int, videoUrl: String, filePath: String, fileName: String) {
        downloadRepository.downloadVideo(videoId, videoUrl, filePath)
    }
    
    /**
     * Check if a video is downloaded
     */
    fun isVideoDownloaded(videoId: Int): Boolean {
        return downloadRepository.isVideoDownloaded(videoId)
    }
    
    /**
     * Get the local path for a downloaded video
     */
    fun getLocalVideoPath(videoId: Int): String? {
        return downloadRepository.getLocalVideoPath(videoId)
    }
    
    /**
     * Remove a downloaded video
     */
    fun removeDownloadedVideo(videoId: Int) {
        downloadRepository.removeDownloadedVideo(videoId)
    }
    
    /**
     * Get a list of all downloaded videos
     */
    fun getAllDownloadedVideos(): List<DownloadedVideo> {
        return downloadRepository.getAllDownloadedVideos()
    }
    
    /**
     * Clean up any resources
     */
    fun cleanupDownloads() {
        downloadRepository.cleanup()
    }

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            if (!_isNetworkAvailable.value) {
                _errorMessage.value = "No internet connection available"
                _isLoading.value = false
                return@launch
            }
            
            try {
                val response = pexelsService.searchVideos(
                    query = "nature",
                    perPage = 10
                )
                
                // Check if response or videos list is valid
                if (response.videos.isEmpty()) {
                    _errorMessage.value = "No videos found"
                    _videos.value = emptyList()
                } else {
                    // Log for debugging
                    println("Got ${response.videos.size} videos from API")
                    
                    // Transform the API response to our VideoItem model
                    val videoList = response.videos.mapNotNull { pexelsVideo ->
                        try {
                            // Create video files list safely
                            val videoFiles = pexelsVideo.videoFiles?.map { file ->
                                VideoFile(
                                    id = file.id,
                                    quality = file.quality,
                                    width = file.width,
                                    height = file.height,
                                    link = file.link,
                                    size = file.size
                                )
                            } ?: emptyList()
                            
                            // Create the video item
                            VideoItem(
                                id = pexelsVideo.id,
                                imageUrl = pexelsVideo.image,
                                duration = pexelsVideo.duration,
                                user = User(
                                    id = pexelsVideo.user.id,
                                    name = pexelsVideo.user.name,
                                    url = pexelsVideo.user.url
                                ),
                                videoFiles = videoFiles
                            )
                        } catch (e: Exception) {
                            println("Error mapping video: ${e.message}")
                            null
                        }
                    }
                    
                    println("Mapped ${videoList.size} videos successfully")
                    _videos.value = videoList
                }
            } catch (e: Exception) {
                println("API error: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Failed to load videos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun retryFetchVideos() {
        fetchVideos()
    }
} 
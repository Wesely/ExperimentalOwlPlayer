package com.owl.playerdemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owl.playerdemo.data.service.PexelsService
import com.owl.playerdemo.data.util.NetworkConnectivityManager
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
    private val networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(getSampleVideos())
    val videos: StateFlow<List<VideoItem>> = _videos
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        observeNetworkConnectivity()
    }
    
    private fun observeNetworkConnectivity() {
        viewModelScope.launch {
            networkConnectivityManager.isNetworkAvailable.collectLatest { isAvailable ->
                _isNetworkAvailable.value = isAvailable
                if (isAvailable && _videos.value == getSampleVideos()) {
                    // If network becomes available and we're showing sample data, fetch real data
                    fetchVideos()
                }
            }
        }
    }

    fun fetchVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            if (!_isNetworkAvailable.value) {
                _errorMessage.value = "No internet connection available"
                _videos.value = getSampleVideos()
                _isLoading.value = false
                return@launch
            }
            
            try {
                val response = pexelsService.searchVideos(
                    query = "nature",
                    perPage = 10
                )
                
                // Transform the API response to our VideoItem model
                val videoList = response.videos.map { pexelsVideo ->
                    VideoItem(
                        id = pexelsVideo.id,
                        title = pexelsVideo.url.split("/").last().replace("-", " "),
                        imageUrl = pexelsVideo.image,
                        duration = pexelsVideo.duration,
                        user = User(
                            id = pexelsVideo.user.id,
                            name = pexelsVideo.user.name,
                            url = pexelsVideo.user.url
                        ),
                        videoFiles = pexelsVideo.videoFiles.map { file ->
                            VideoFile(
                                id = file.id,
                                quality = file.quality,
                                width = file.width,
                                height = file.height,
                                link = file.link
                            )
                        }
                    )
                }
                
                _videos.value = videoList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load videos: ${e.message}"
                if (_videos.value.isEmpty()) {
                    _videos.value = getSampleVideos()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun retryFetchVideos() {
        fetchVideos()
    }
    
    private fun getSampleVideos(): List<VideoItem> {
        val sampleVideoFiles = listOf(
            VideoFile(
                id = 1,
                quality = "hd",
                width = 1920,
                height = 1080,
                link = "https://example.com/video.mp4"
            )
        )
        
        return listOf(
            VideoItem(
                id = 1,
                title = "Beautiful Ocean Waves",
                imageUrl = "https://images.pexels.com/videos/3571264/free-video-3571264.jpg?auto=compress&cs=tinysrgb&fit=crop&h=630&w=1200",
                duration = 30,
                user = User(
                    id = 1,
                    name = "Nature Explorer",
                    url = "https://example.com/user"
                ),
                videoFiles = sampleVideoFiles
            ),
            VideoItem(
                id = 2,
                title = "Mountain Landscape",
                imageUrl = "https://images.pexels.com/photos/1366919/pexels-photo-1366919.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
                duration = 45,
                user = User(
                    id = 2,
                    name = "Mountain View",
                    url = "https://example.com/user2"
                ),
                videoFiles = sampleVideoFiles
            ),
            VideoItem(
                id = 3,
                title = "Sunset at the Beach",
                imageUrl = "https://images.pexels.com/photos/1032650/pexels-photo-1032650.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
                duration = 20,
                user = User(
                    id = 3,
                    name = "Sunset Photographer",
                    url = "https://example.com/user3"
                ),
                videoFiles = sampleVideoFiles
            )
        )
    }
} 
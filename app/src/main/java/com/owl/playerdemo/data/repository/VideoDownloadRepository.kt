package com.owl.playerdemo.data.repository

import android.content.Context
import com.owl.playerdemo.model.DownloadedVideo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for managing downloaded videos
 */
@Singleton
class VideoDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Constants for SharedPreferences
    private companion object {
        const val PREFS_NAME = "owl_player_prefs"
        const val KEY_DOWNLOADED_VIDEOS = "downloaded_videos"
    }

    private val _downloadedVideos = MutableStateFlow<Map<Int, DownloadedVideo>>(emptyMap())
    val downloadedVideos: StateFlow<Map<Int, DownloadedVideo>> = _downloadedVideos

    init {
        loadDownloadedVideos()
    }

    /**
     * Loads saved video download information from SharedPreferences
     */
    private fun loadDownloadedVideos() {
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString(KEY_DOWNLOADED_VIDEOS, null)
            
            if (jsonString != null) {
                val videoMap = Json.decodeFromString<Map<Int, DownloadedVideo>>(jsonString)
                _downloadedVideos.value = videoMap
            }
        } catch (e: Exception) {
            // If there's an error reading from SharedPreferences, initialize with empty map
            _downloadedVideos.value = emptyMap()
            e.printStackTrace()
        }
    }

    /**
     * Check if a video is downloaded
     * @param videoId The ID of the video to check
     * @return true if the video is downloaded, false otherwise
     */
    fun isVideoDownloaded(videoId: Int): Boolean {
        return _downloadedVideos.value.containsKey(videoId)
    }
    
    /**
     * Get the local path for a downloaded video
     * @param videoId The ID of the video
     * @return The local file path or null if not downloaded
     */
    fun getLocalVideoPath(videoId: Int): String? {
        return _downloadedVideos.value[videoId]?.localFilePath
    }
    
    /**
     * Save information for a newly downloaded video
     * @param videoId ID of the downloaded video
     * @param localFilePath Path where the video is stored locally
     * @param fileName Name of the video file
     */
    fun saveDownloadedVideo(videoId: Int, localFilePath: String, fileName: String) {
        val downloadedVideo = DownloadedVideo(
            videoId = videoId,
            localFilePath = localFilePath,
            fileName = fileName
        )
        
        // Update the state with the new downloaded video
        val updatedMap = _downloadedVideos.value.toMutableMap()
        updatedMap[videoId] = downloadedVideo
        _downloadedVideos.value = updatedMap
        
        // Persist to SharedPreferences
        persistDownloadedVideos()
    }
    
    /**
     * Remove a downloaded video from tracking
     * @param videoId ID of the video to remove
     */
    fun removeDownloadedVideo(videoId: Int) {
        val updatedMap = _downloadedVideos.value.toMutableMap()
        updatedMap.remove(videoId)
        _downloadedVideos.value = updatedMap
        
        // Update SharedPreferences
        persistDownloadedVideos()
    }
    
    /**
     * Get a list of all downloaded videos
     * @return List of downloaded videos
     */
    fun getAllDownloadedVideos(): List<DownloadedVideo> {
        return _downloadedVideos.value.values.toList()
    }
    
    /**
     * Persist downloaded videos data to SharedPreferences
     */
    private fun persistDownloadedVideos() {
        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = Json.encodeToString(_downloadedVideos.value)
            sharedPrefs.edit().putString(KEY_DOWNLOADED_VIDEOS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 
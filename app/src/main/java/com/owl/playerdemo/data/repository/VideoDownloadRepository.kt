package com.owl.playerdemo.data.repository

import android.content.Context
import android.util.Log
import com.owl.playerdemo.model.DownloadedVideo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository responsible for managing downloaded videos using OkHttp
 */
@Singleton
class VideoDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Constants for SharedPreferences
    private companion object {
        const val PREFS_NAME = "owl_player_prefs"
        const val KEY_DOWNLOADED_VIDEOS = "downloaded_videos"
        private const val TAG = "VideoDownloadRepo"
    }

    private val _downloadedVideos = MutableStateFlow<Map<Int, DownloadedVideo>>(emptyMap())
    val downloadedVideos: StateFlow<Map<Int, DownloadedVideo>> = _downloadedVideos
    
    // Track download progress (videoId to progress percentage 0-100)
    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress
    
    // Map to track active downloads (videoId to call)
    private val activeDownloads = mutableMapOf<Int, Call>()
    
    // OkHttp client for downloads
    private val okHttpClient = OkHttpClient.Builder()
        .build()
        
    // CoroutineScope for download operations
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
     * Download a video using OkHttp
     * @param videoId ID of the video to download
     * @param url URL to download from
     * @param destinationPath Full path where the video should be saved
     * @return The request ID
     */
    fun downloadVideo(videoId: Int, url: String, destinationPath: String): Int {
        // Check if we're already downloading this video
        if (activeDownloads.containsKey(videoId)) {
            Log.d(TAG, "Already downloading video $videoId")
            return videoId
        }
        
        Log.d(TAG, "Starting download for video $videoId from URL: $url")
        Log.d(TAG, "Destination path: $destinationPath")
        
        // Create the destination file
        val destinationFile = File(destinationPath)
        
        // Create parent directories if they don't exist
        val parentCreated = destinationFile.parentFile?.mkdirs() ?: false
        Log.d(TAG, "Parent directories created: $parentCreated")
        
        // Build the request
        val request = Request.Builder()
            .url(url)
            .build()
        
        // Extract filename from path
        val fileName = destinationFile.name
        Log.d(TAG, "Filename: $fileName")
        
        // Create the download call
        val call = okHttpClient.newCall(request)
        
        // Store the call for potential cancellation
        activeDownloads[videoId] = call
        
        // Update progress to indicate download started
        updateDownloadProgress(videoId, 0f)
        Log.d(TAG, "Download progress updated to 0%")
        
        // Start the download in a coroutine
        downloadScope.launch {
            try {
                // Execute the network call
                Log.d(TAG, "Enqueueing download request")
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Handle failure
                        Log.e(TAG, "Download failed for video $videoId: ${e.message}", e)
                        downloadScope.launch {
                            activeDownloads.remove(videoId)
                            updateDownloadProgress(videoId, -1f) // Negative value indicates error
                        }
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d(TAG, "Got response for video $videoId: ${response.code}")
                        
                        if (!response.isSuccessful) {
                            // Handle unsuccessful response
                            Log.e(TAG, "Download request unsuccessful: ${response.code}")
                            downloadScope.launch {
                                activeDownloads.remove(videoId)
                                updateDownloadProgress(videoId, -1f)
                            }
                            return
                        }

                        // Get content length for progress tracking
                        val contentLength = response.body?.contentLength() ?: -1L
                        Log.d(TAG, "Content length: $contentLength bytes")
                        var bytesDownloaded = 0L
                        
                        try {
                            // Create output stream to save the file
                            Log.d(TAG, "Creating output stream to save file")
                            FileOutputStream(destinationFile).use { outputStream ->
                                response.body?.let { body ->
                                    body.byteStream().use { inputStream ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        
                                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                            // Write to file
                                            outputStream.write(buffer, 0, bytesRead)
                                            
                                            // Update progress
                                            bytesDownloaded += bytesRead
                                            if (contentLength > 0) {
                                                val progress = bytesDownloaded.toFloat() / contentLength.toFloat() * 100f
                                                updateDownloadProgress(videoId, progress)
                                                
                                                // Log progress every 10%
                                                if (progress % 10 < 1) {
                                                    Log.d(TAG, "Download progress: ${progress.toInt()}%")
                                                }
                                            }
                                        }
                                        
                                        outputStream.flush()
                                    }
                                }
                                
                                // Download completed successfully
                                Log.d(TAG, "Download completed for video $videoId")
                                downloadScope.launch {
                                    // Save download info
                                    saveDownloadedVideo(videoId, destinationPath, fileName)
                                    Log.d(TAG, "Video info saved to preferences")
                                    
                                    // Remove from active downloads
                                    activeDownloads.remove(videoId)
                                    
                                    // Set progress to 100%
                                    updateDownloadProgress(videoId, 100f)
                                    Log.d(TAG, "Final progress updated to 100%")
                                }
                            }
                        } catch (e: IOException) {
                            // Handle file I/O errors
                            Log.e(TAG, "File I/O error for video $videoId: ${e.message}", e)
                            downloadScope.launch {
                                activeDownloads.remove(videoId)
                                updateDownloadProgress(videoId, -1f)
                            }
                            e.printStackTrace()
                            
                            // Clean up partial file if it exists
                            if (destinationFile.exists()) {
                                val deleted = destinationFile.delete()
                                Log.d(TAG, "Partial file deleted: $deleted")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e(TAG, "Unexpected error for video $videoId: ${e.message}", e)
                activeDownloads.remove(videoId)
                updateDownloadProgress(videoId, -1f)
                e.printStackTrace()
            }
        }
        
        return videoId
    }
    
    /**
     * Cancel an active download
     * @param videoId ID of the video download to cancel
     */
    fun cancelDownload(videoId: Int) {
        val call = activeDownloads[videoId]
        call?.let {
            if (!it.isCanceled()) {
                it.cancel()
            }
            activeDownloads.remove(videoId)
            updateDownloadProgress(videoId, -1f)
        }
    }
    
    /**
     * Update the download progress
     * @param videoId ID of the video
     * @param progress Progress percentage (0-100)
     */
    private fun updateDownloadProgress(videoId: Int, progress: Float) {
        val updatedMap = _downloadProgress.value.toMutableMap()
        
        if (progress < 0 || progress >= 100) {
            // Remove progress for completed or failed downloads
            updatedMap.remove(videoId)
            Log.d(TAG, "Removing progress tracking for video $videoId (progress: $progress)")
        } else {
            updatedMap[videoId] = progress
        }
        
        _downloadProgress.value = updatedMap
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
        
        Log.d(TAG, "Video $videoId added to downloaded videos map")
        
        // Persist to SharedPreferences
        persistDownloadedVideos()
    }
    
    /**
     * Remove a downloaded video from tracking
     * @param videoId ID of the video to remove
     */
    fun removeDownloadedVideo(videoId: Int) {
        // Cancel download if active
        cancelDownload(videoId)
        
        // Get the local file path
        val localFilePath = _downloadedVideos.value[videoId]?.localFilePath
        
        // Remove from state
        val updatedMap = _downloadedVideos.value.toMutableMap()
        updatedMap.remove(videoId)
        _downloadedVideos.value = updatedMap
        
        // Update SharedPreferences
        persistDownloadedVideos()
        
        // Delete the local file if it exists
        localFilePath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
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
            Log.d(TAG, "Saved ${_downloadedVideos.value.size} videos to SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save downloads to SharedPreferences", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Clean up any resources when the repository is no longer needed
     */
    fun cleanup() {
        // Cancel all active downloads
        activeDownloads.forEach { (_, call) ->
            if (!call.isCanceled()) {
                call.cancel()
            }
        }
        activeDownloads.clear()
        
        // Clear download progress
        _downloadProgress.value = emptyMap()
        
        // Cancel the download scope
        downloadScope.cancel()
    }
} 
package com.owl.playerdemo.data.repository

import android.content.Context
import com.owl.playerdemo.model.DownloadedVideo
import com.tonyofrancis.fetch2.*
import com.tonyofrancis.fetch2.fetch.FetchConfiguration
import com.tonyofrancis.fetch2.fetch.FetchListener
import com.tonyofrancis.fetch2okhttp.OkHttpDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for managing downloaded videos using Fetch2 library
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
    
    // Fetch instance for downloads
    private val fetch: Fetch
    
    // Map to track Fetch Request IDs to our Video IDs
    private val requestToVideoIdMap = mutableMapOf<Int, Int>()

    init {
        // Configure and initialize Fetch
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .enableLogging(true)
            .setHttpDownloader(OkHttpDownloader(OkHttpClient.Builder().build()))
            .build()
            
        fetch = Fetch.Impl.getInstance(fetchConfiguration)
        
        // Register listener for download events
        fetch.addListener(object : FetchListener {
            override fun onAdded(download: Download) {
                println("Download added: ${download.id}")
            }

            override fun onCancelled(download: Download) {
                println("Download cancelled: ${download.id}")
                val videoId = requestToVideoIdMap[download.id]
                if (videoId != null) {
                    removeDownloadedVideo(videoId)
                }
            }

            override fun onCompleted(download: Download) {
                val videoId = requestToVideoIdMap[download.id]
                if (videoId != null) {
                    // Update the download status to indicate it's fully downloaded
                    val currentDownload = _downloadedVideos.value[videoId]
                    if (currentDownload != null) {
                        val updatedDownload = currentDownload.copy(
                            localFilePath = download.file
                        )
                        val updatedMap = _downloadedVideos.value.toMutableMap()
                        updatedMap[videoId] = updatedDownload
                        _downloadedVideos.value = updatedMap
                        persistDownloadedVideos()
                    }
                }
                println("Download completed: ${download.id}, file: ${download.file}")
            }

            override fun onDeleted(download: Download) {
                println("Download deleted: ${download.id}")
                val videoId = requestToVideoIdMap[download.id]
                if (videoId != null) {
                    removeDownloadedVideo(videoId)
                    requestToVideoIdMap.remove(download.id)
                }
            }

            override fun onDownloadBlockUpdated(
                download: Download,
                downloadBlock: DownloadBlock,
                totalBlocks: Int
            ) {
                // Optional: Track individual download blocks if needed
            }

            override fun onError(download: Download, error: Error, throwable: Throwable?) {
                println("Download error: ${download.id}, error: ${error.name}, message: ${throwable?.message}")
            }

            override fun onPaused(download: Download) {
                println("Download paused: ${download.id}")
            }

            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long
            ) {
                println("Download progress: ${download.id}, progress: ${download.progress}%, ETA: ${etaInMilliSeconds/1000}s, speed: ${downloadedBytesPerSecond/1024} KB/s")
                
                // We can optionally update our state if needed to track progress
                val videoId = requestToVideoIdMap[download.id]
                videoId?.let {
                    // You could track progress here if needed
                }
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                println("Download queued: ${download.id}, waiting on network: $waitingOnNetwork")
            }

            override fun onRemoved(download: Download) {
                println("Download removed: ${download.id}")
                val videoId = requestToVideoIdMap[download.id]
                if (videoId != null) {
                    requestToVideoIdMap.remove(download.id)
                }
            }

            override fun onResumed(download: Download) {
                println("Download resumed: ${download.id}")
            }

            override fun onStarted(
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int
            ) {
                println("Download started: ${download.id}")
            }

            override fun onWaitingNetwork(download: Download) {
                println("Download waiting for network: ${download.id}")
            }
        })
        
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
     * Download a video using Fetch
     * @param videoId ID of the video to download
     * @param url URL to download from
     * @param destinationPath Full path where the video should be saved
     * @param fileName Name of the file
     * @return The request ID from Fetch
     */
    fun downloadVideo(videoId: Int, url: String, destinationPath: String, fileName: String): Int {
        // Create a new Fetch request
        val request = Request(url, destinationPath)
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL
        
        // Start the download
        val fetchRequestId = fetch.enqueue(request)
        requestToVideoIdMap[fetchRequestId] = videoId
        
        // Save download info
        saveDownloadedVideo(videoId, destinationPath, fileName)
        
        return fetchRequestId
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
    
    /**
     * Clean up any resources when the repository is no longer needed
     */
    fun cleanup() {
        fetch.close()
    }
} 
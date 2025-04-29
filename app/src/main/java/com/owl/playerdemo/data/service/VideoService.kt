package com.owl.playerdemo.data.service

import android.content.Context
import android.os.Environment
import android.util.Log
import com.owl.playerdemo.data.repository.VideoDownloadRepository
import com.owl.playerdemo.model.VideoItem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoService @Inject constructor(
    private val downloadRepository: VideoDownloadRepository
) {
    private companion object {
        private const val TAG = "VideoService"
    }
    
    /**
     * Download a video
     * @return True if download started successfully, false otherwise
     */
    fun downloadVideo(context: Context, video: VideoItem): Boolean {
        val bestVideo = video.videoFiles.firstOrNull()
        
        if (bestVideo == null) {
            Log.e(TAG, "No video files available to download for video ID: ${video.id}")
            return false
        }
        
        // Check if already downloading or downloaded
        if (isVideoDownloaded(video.id)) {
            Log.d(TAG, "Video ${video.id} is already downloaded")
            return false
        }
        
        Log.d(TAG, "Preparing to download video ${video.id} with URL: ${bestVideo.link}")
        
        try {
            // Create download directory if it doesn't exist
            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OwlPlayer")
            if (!downloadDir.exists()) {
                val created = downloadDir.mkdirs()
                Log.d(TAG, "Created download directory: $created")
            } else {
                Log.d(TAG, "Download directory already exists at: ${downloadDir.absolutePath}")
            }
            
            // Create unique filename based on video ID and quality
            val fileName = "video_${video.id}_${bestVideo.quality}.mp4"
            val localFilePath = "${downloadDir.absolutePath}/$fileName"
            
            Log.d(TAG, "Starting download to path: $localFilePath")
            
            // Start the download
            val downloadId = downloadRepository.downloadVideo(video.id, bestVideo.link, localFilePath)
            Log.d(TAG, "Download started with ID: $downloadId")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting download for video ${video.id}: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Check if a video is downloaded
     */
    fun isVideoDownloaded(videoId: Int): Boolean {
        val isDownloaded = downloadRepository.isVideoDownloaded(videoId)
        Log.d(TAG, "Checking if video $videoId is downloaded: $isDownloaded")
        return isDownloaded
    }
    
    /**
     * Get the local path for a downloaded video
     */
    fun getLocalVideoPath(videoId: Int): String? {
        val path = downloadRepository.getLocalVideoPath(videoId)
        Log.d(TAG, "Getting local path for video $videoId: $path")
        return path
    }
    
    /**
     * Remove a downloaded video
     */
    fun removeDownloadedVideo(videoId: Int) {
        Log.d(TAG, "Removing downloaded video $videoId")
        downloadRepository.removeDownloadedVideo(videoId)
    }
    
    /**
     * Get the formatted size of a file
     * @param filePath Path to the file
     * @return Formatted size string (e.g. "10.5 MB")
     */
    fun getFormattedFileSize(filePath: String?): String {
        if (filePath == null) return "Unknown"
        
        val file = File(filePath)
        if (!file.exists()) return "Unknown"
        
        val bytes = file.length()
        val mb = bytes / 1024.0 / 1024.0
        return String.format("%.1f MB", mb)
    }
    
    /**
     * Calculate total storage used by downloaded videos
     */
    fun calculateTotalStorageUsed(downloadedVideos: Map<Int, com.owl.playerdemo.model.DownloadedVideo>): Long {
        return downloadedVideos.values.sumOf { 
            val file = File(it.localFilePath)
            if (file.exists()) file.length() else 0L
        }
    }
    
    /**
     * Format storage size
     */
    fun formatStorageSize(bytes: Long): String {
        val mb = bytes / 1024.0 / 1024.0
        return String.format("%.1f MB", mb)
    }
} 
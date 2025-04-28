package com.owl.playerdemo.data.service

import android.content.Context
import android.os.Environment
import com.owl.playerdemo.data.repository.VideoDownloadRepository
import com.owl.playerdemo.model.VideoItem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoService @Inject constructor(
    private val downloadRepository: VideoDownloadRepository
) {
    /**
     * Download a video
     * @return True if download started successfully, false otherwise
     */
    fun downloadVideo(context: Context, video: VideoItem): Boolean {
        val bestVideo = video.videoFiles.firstOrNull() ?: return false
        
        // Create download directory if it doesn't exist
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OwlPlayer")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // Create unique filename based on video ID and quality
        val fileName = "video_${video.id}_${bestVideo.quality}.mp4"
        val localFilePath = "${downloadDir.absolutePath}/$fileName"
        
        // Check if already downloading or downloaded
        if (isVideoDownloaded(video.id)) {
            return false
        }
        
        // Start the download
        downloadRepository.downloadVideo(video.id, bestVideo.link, localFilePath)
        return true
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
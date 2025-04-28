package com.owl.playerdemo.util

import android.content.Context
import android.widget.Toast
import com.owl.playerdemo.data.service.VideoService
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.player.PlayerActivity

/**
 * Utility class to handle video playback operations
 */
object PlayerUtils {
    /**
     * Play a locally downloaded video
     * @return True if playback started successfully, false otherwise
     */
    fun playVideo(context: Context, video: VideoItem, videoService: VideoService): Boolean {
        // Check if the video is downloaded
        if (!videoService.isVideoDownloaded(video.id)) {
            Toast.makeText(context, "Please download the video first", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Get local file path
        val videoPath = videoService.getLocalVideoPath(video.id)
        
        if (videoPath == null) {
            Toast.makeText(context, "Cannot find downloaded video", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Create title from video information
        val videoTitle = "${video.user.name}'s video"
        
        // Launch player activity
        val intent = PlayerActivity.createIntent(context, videoPath, videoTitle)
        context.startActivity(intent)
        return true
    }
} 
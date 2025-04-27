package com.owl.playerdemo.model

import kotlinx.serialization.Serializable

/**
 * Model representing a downloaded video
 * @param videoId The ID of the video from Pexels API
 * @param localFilePath The local path where the video is stored
 * @param fileName The name of the file
 * @param downloadDate The timestamp when the video was downloaded
 */
@Serializable
data class DownloadedVideo(
    val videoId: Int,
    val localFilePath: String,
    val fileName: String,
    val downloadDate: Long = System.currentTimeMillis()
) 
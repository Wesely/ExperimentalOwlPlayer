package com.owl.playerdemo.data.model

import com.google.gson.annotations.SerializedName

data class PexelsVideo(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val image: String,
    val duration: Int,
    val user: PexelsUser,
    
    @SerializedName("video_files")
    val videoFiles: List<VideoFile>? = null,
    
    @SerializedName("video_pictures")
    val videoPictures: List<VideoPicture>? = null
)

data class PexelsUser(
    val id: Int,
    val name: String,
    val url: String
)

data class VideoFile(
    val id: Int,
    
    @SerializedName("quality")
    val quality: String,
    
    @SerializedName("file_type")
    val fileType: String,
    
    val width: Int,
    val height: Int,
    val fps: Double,
    val link: String,
    val size: Long
)

data class VideoPicture(
    val id: Int,
    val picture: String,
    val nr: Int
) 
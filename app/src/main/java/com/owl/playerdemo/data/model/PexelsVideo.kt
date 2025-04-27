package com.owl.playerdemo.data.model

data class PexelsVideo(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val image: String,
    val duration: Int,
    val user: PexelsUser,
    val videoFiles: List<VideoFile>,
    val videoPictures: List<VideoPicture>
)

data class PexelsUser(
    val id: Int,
    val name: String,
    val url: String
)

data class VideoFile(
    val id: Int,
    val quality: String,
    val fileType: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val link: String
)

data class VideoPicture(
    val id: Int,
    val picture: String,
    val nr: Int
) 
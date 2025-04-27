package com.owl.playerdemo.model

data class VideoItem(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val duration: Int,
    val user: User,
    val videoFiles: List<VideoFile>
)

data class User(
    val id: Int,
    val name: String,
    val url: String
)

data class VideoFile(
    val id: Int,
    val quality: String,
    val width: Int,
    val height: Int,
    val link: String
) 
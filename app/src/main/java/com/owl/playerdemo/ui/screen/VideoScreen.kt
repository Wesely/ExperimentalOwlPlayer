package com.owl.playerdemo.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owl.playerdemo.data.service.VideoService
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.components.ErrorView
import com.owl.playerdemo.ui.components.VideoList
import com.owl.playerdemo.ui.viewmodel.MainViewModel
import com.owl.playerdemo.util.PlayerUtils

@Composable
fun VideoScreen(viewModel: MainViewModel, videoService: VideoService) {
    val context = LocalContext.current
    val videos: List<VideoItem> by viewModel.videos.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val downloadsInProgress by viewModel.downloadsInProgress.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    
    // Calculate total storage used by downloaded videos
    val totalStorageUsed = remember(downloadedVideos) {
        videoService.calculateTotalStorageUsed(downloadedVideos)
    }
    
    // Format storage size
    val formattedStorage = remember(totalStorageUsed) {
        videoService.formatStorageSize(totalStorageUsed)
    }
    
    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoItem?>(null) }
    
    // State for storage info dialog
    var showStorageInfoDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.fetchVideos()
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with info icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Trending Nature Videos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showStorageInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Storage Information",
                    tint = Color.DarkGray
                )
            }
        }

        // Network Status Indicator
        if (!isNetworkAvailable) {
            Text(
                text = "⚠️ No Internet Connection - Cannot Load Videos",
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        // Storage Usage Dialog
        if (showStorageInfoDialog) {
            AlertDialog(
                onDismissRequest = { showStorageInfoDialog = false },
                title = { Text("Storage Information") },
                text = { 
                    Text("Storage Used: $formattedStorage (${downloadedVideos.size} videos)") 
                },
                confirmButton = {
                    TextButton(onClick = { showStorageInfoDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Loading, Error or Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null && videos.isEmpty() -> {
                    ErrorView(
                        message = errorMessage ?: "Unknown error occurred",
                        onRetry = { viewModel.retryFetchVideos() }
                    )
                }
                videos.isNotEmpty() -> {
                    // Videos
                    VideoList(
                        videos = videos,
                        onDownloadClick = { video ->
                            val result = videoService.downloadVideo(context, video)
                            if (result) {
                                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "This video is already downloaded", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPlayClick = { video ->
                            PlayerUtils.playVideo(context, video, videoService)
                        },
                        onDeleteClick = { video ->
                            // Show delete confirmation dialog
                            videoToDelete = video
                            showDeleteDialog = true
                        },
                        modifier = Modifier
                    )
                }
                else -> {
                    Text(
                        text = "No videos available",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && videoToDelete != null) {
        val video = videoToDelete!!
        
        // Get actual file size from the local file
        val localFilePath = videoService.getLocalVideoPath(video.id)
        val fileSize = videoService.getFormattedFileSize(localFilePath)
        
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Downloaded Video") },
            text = { 
                Text("Do you want to remove this video from local storage? This will free ($fileSize) storage but you will need to download it again to play it.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Delete the video
                        videoService.removeDownloadedVideo(video.id)
                        Toast.makeText(context, "Video deleted. Freed $fileSize of storage.", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        videoToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        videoToDelete = null 
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 
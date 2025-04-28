package com.owl.playerdemo

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.components.VideoList
import com.owl.playerdemo.ui.player.PlayerActivity
import com.owl.playerdemo.ui.theme.OwlPlayerDemoTheme
import com.owl.playerdemo.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            viewModel = viewModel()
            
            OwlPlayerDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoScreen(viewModel)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources
        viewModel.cleanupDownloads()
    }
}

@Composable
fun VideoScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val videos: List<VideoItem> by viewModel.videos.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val downloadsInProgress by viewModel.downloadsInProgress.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    
    // Calculate total storage used by downloaded videos
    val totalStorageUsed = remember(downloadedVideos) {
        downloadedVideos.values.sumOf { 
            val file = File(it.localFilePath)
            if (file.exists()) file.length() else 0L
        }
    }
    
    // Format storage size
    val formattedStorage = remember(totalStorageUsed) {
        val mb = totalStorageUsed / 1024.0 / 1024.0
        String.format("%.1f MB", mb)
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
                            downloadVideo(context, video, viewModel)
                        },
                        onPlayClick = { video ->
                            playVideo(context, video, viewModel)
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
        val localFilePath = viewModel.getLocalVideoPath(video.id)
        val fileSize = getFormattedFileSize(localFilePath)
        
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
                        deleteVideo(context, video, viewModel)
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

/**
 * Download a video using OkHttp via ViewModel
 */
private fun downloadVideo(context: Context, video: VideoItem, viewModel: MainViewModel) {
    val bestVideo = video.videoFiles.firstOrNull() ?: run {
        Toast.makeText(context, "No video files available", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Create download directory if it doesn't exist
    val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OwlPlayer")
    if (!downloadDir.exists()) {
        downloadDir.mkdirs()
    }
    
    // Create unique filename based on video ID and quality
    val fileName = "video_${video.id}_${bestVideo.quality}.mp4"
    val localFilePath = "${downloadDir.absolutePath}/$fileName"
    
    // Check if already downloading
    if (viewModel.isVideoDownloaded(video.id)) {
        Toast.makeText(context, "This video is already downloaded", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Start the download
    viewModel.downloadVideo(video.id, bestVideo.link, localFilePath, fileName)
    
    Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
}

/**
 * Play a locally downloaded video
 */
private fun playVideo(context: Context, video: VideoItem, viewModel: MainViewModel) {
    // Check if the video is downloaded
    if (!viewModel.isVideoDownloaded(video.id)) {
        Toast.makeText(context, "Please download the video first", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Get local file path
    val videoPath = viewModel.getLocalVideoPath(video.id)
    
    if (videoPath == null) {
        Toast.makeText(context, "Cannot find downloaded video", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Create title from video information
    val videoTitle = "${video.user.name}'s video"
    
    // Launch player activity
    val intent = PlayerActivity.createIntent(context, videoPath, videoTitle)
    context.startActivity(intent)
}

/**
 * Get the formatted size of a file
 * @param filePath Path to the file
 * @return Formatted size string (e.g. "10.5 MB")
 */
private fun getFormattedFileSize(filePath: String?): String {
    if (filePath == null) return "Unknown"
    
    val file = File(filePath)
    if (!file.exists()) return "Unknown"
    
    val bytes = file.length()
    val mb = bytes / 1024.0 / 1024.0
    return String.format("%.1f MB", mb)
}

/**
 * Delete a downloaded video
 */
private fun deleteVideo(context: Context, video: VideoItem, viewModel: MainViewModel) {
    if (viewModel.isVideoDownloaded(video.id)) {
        // Get the local file path
        val localFilePath = viewModel.getLocalVideoPath(video.id)
        
        // Get the actual file size before deletion for confirmation message
        val fileSize = getFormattedFileSize(localFilePath)
        
        // Delete the video
        viewModel.removeDownloadedVideo(video.id)
        
        // Show success message with the freed space
        Toast.makeText(context, "Video deleted. Freed $fileSize of storage.", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Video is not downloaded", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OwlPlayerDemoTheme {
        Greeting("Android")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
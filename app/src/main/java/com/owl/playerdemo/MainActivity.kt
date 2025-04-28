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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    
    LaunchedEffect(Unit) {
        viewModel.fetchVideos()
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Trending Nature Videos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        // Network Status Indicator
        if (!isNetworkAvailable) {
            Text(
                text = "⚠️ No Internet Connection - Cannot Load Videos",
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        // Loading, Error or Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
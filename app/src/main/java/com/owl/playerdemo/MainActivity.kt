package com.owl.playerdemo

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.components.VideoList
import com.owl.playerdemo.ui.theme.OwlPlayerDemoTheme
import com.owl.playerdemo.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    private var downloadManager: DownloadManager? = null
    private val downloadsInProgress = mutableMapOf<Long, Int>() // downloadId to videoId mapping
    
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    val videoId = downloadsInProgress[downloadId] ?: return
                    
                    // Check if download was successful
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager?.query(query)
                    
                    if (cursor?.moveToFirst() == true) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)
                            
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    // Download completed successfully
                                    viewModel.updateDownloadProgress(videoId, 100f)
                                    downloadsInProgress.remove(downloadId)
                                    
                                    println("Download completed successfully for video $videoId")
                                    Toast.makeText(context, "Download completed", Toast.LENGTH_SHORT).show()
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    // Download failed
                                    viewModel.removeDownloadProgress(videoId)
                                    downloadsInProgress.remove(downloadId)
                                    
                                    // Get the reason for failure
                                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                                    
                                    println("Download failed for video $videoId, reason: $reason")
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    
                    cursor?.close()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Register download complete receiver with appropriate flags for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                downloadCompleteReceiver, 
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                downloadCompleteReceiver, 
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        
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
        
        // Start a coroutine to periodically check download progress
        startDownloadProgressTracking()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
    }
    
    private fun startDownloadProgressTracking() {
        Thread {
            while (!isFinishing) {
                try {
                    // For each download in progress, check its status
                    downloadsInProgress.forEach { (downloadId, videoId) ->
                        updateDownloadProgress(downloadId, videoId)
                    }
                    
                    // Sleep for a bit to avoid too frequent updates
                    Thread.sleep(500)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
    
    private fun updateDownloadProgress(downloadId: Long, videoId: Int) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager?.query(query) ?: return
            
            if (cursor.moveToFirst()) {
                val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                
                if (totalSizeIndex != -1 && downloadedSizeIndex != -1 && statusIndex != -1) {
                    val totalSize = cursor.getLong(totalSizeIndex)
                    val downloadedSize = cursor.getLong(downloadedSizeIndex)
                    val status = cursor.getInt(statusIndex)
                    
                    if (totalSize > 0) {
                        val progress = (downloadedSize * 100f / totalSize)
                        viewModel.updateDownloadProgress(videoId, progress)
                        
                        // Check if download is completed or failed
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            viewModel.updateDownloadProgress(videoId, 100f)
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            viewModel.removeDownloadProgress(videoId)
                            downloadsInProgress.remove(downloadId)
                        }
                    }
                }
            }
            
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun downloadVideo(video: VideoItem) {
        val bestVideo = video.videoFiles.firstOrNull() ?: run {
            Toast.makeText(this, "No video files available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create download directory if it doesn't exist
        val downloadDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "OwlPlayer")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // Create unique filename based on video ID and quality
        val fileName = "video_${video.id}_${bestVideo.quality}.mp4"
        val localFilePath = "${downloadDir.absolutePath}/$fileName"
        
        // Check if already downloading
        if (viewModel.isVideoDownloaded(video.id)) {
            Toast.makeText(this, "This video is already downloaded", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Start tracking download progress in ViewModel
        viewModel.startTrackingDownload(video.id)
        
        // Create download request
        val request = DownloadManager.Request(Uri.parse(bestVideo.link))
            .setTitle("Downloading Video")
            .setDescription("Downloading ${video.user.name}'s video")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(File(localFilePath)))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        
        // Add additional settings for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
        }
        
        // Queue the download and get the download ID
        val downloadId = downloadManager?.enqueue(request) ?: return
        
        // Add to downloads in progress map
        downloadsInProgress[downloadId] = video.id
        
        // Save download info to repository (but don't remove from in-progress)
        viewModel.saveDownloadedVideo(video.id, localFilePath, fileName)
        
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun VideoScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val videos: List<VideoItem> by viewModel.videos.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
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
                            (context as? MainActivity)?.downloadVideo(video) 
                                ?: Toast.makeText(context, "Cannot start download", Toast.LENGTH_SHORT).show()
                        },
                        onPlayClick = { video ->
                            // Show toast for now (we'll implement playback later)
                            Toast.makeText(context, "Playing ${video.user.name}'s video", Toast.LENGTH_SHORT).show()
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
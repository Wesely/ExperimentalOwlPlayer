package com.owl.playerdemo.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.owl.playerdemo.ui.theme.OwlPlayerDemoTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract file path from intent
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video"
        
        setContent {
            val viewModel: PlayerViewModel = viewModel()
            
            // Initialize the video
            DisposableEffect(videoPath) {
                viewModel.initializeWithVideo(videoPath, videoTitle)
                onDispose { }
            }
            
            OwlPlayerDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoPlayerScreen(viewModel)
                }
            }
        }
    }
    
    companion object {
        private const val EXTRA_VIDEO_PATH = "extra_video_path"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        
        fun createIntent(context: Context, videoPath: String, videoTitle: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    val videoState by viewModel.playerState.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (videoState) {
            is PlayerState.Loading -> {
                CircularProgressIndicator()
            }
            is PlayerState.Ready -> {
                val playerState = videoState as PlayerState.Ready
                
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = playerState.player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { playerView ->
                        // Update player if needed
                        playerView.player = playerState.player
                    }
                )
                
                // Handle lifecycle events to properly release player
                DisposableEffect(lifecycle, playerState.player) {
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                playerState.player.pause()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                playerState.player.play()
                            }
                            Lifecycle.Event.ON_DESTROY -> {
                                playerState.player.release()
                            }
                            else -> {}
                        }
                    }
                    
                    lifecycle.addObserver(lifecycleObserver)
                    onDispose {
                        lifecycle.removeObserver(lifecycleObserver)
                    }
                }
            }
            is PlayerState.Error -> {
                val errorState = videoState as PlayerState.Error
                Text(
                    text = "Error: ${errorState.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
} 
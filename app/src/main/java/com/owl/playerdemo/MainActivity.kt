package com.owl.playerdemo

import android.os.Bundle
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
import androidx.compose.material3.Text
import androidx.tv.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owl.playerdemo.model.VideoItem
import com.owl.playerdemo.ui.components.VideoList
import com.owl.playerdemo.ui.theme.OwlPlayerDemoTheme
import com.owl.playerdemo.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OwlPlayerDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    VideoScreen()
                }
            }
        }
    }
}

@Composable
fun VideoScreen() {
    val viewModel: MainViewModel = viewModel()
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
                text = "⚠️ No Internet Connection - Showing Offline Content",
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
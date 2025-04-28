package com.owl.playerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owl.playerdemo.data.service.VideoService
import com.owl.playerdemo.ui.screen.VideoScreen
import com.owl.playerdemo.ui.theme.OwlPlayerDemoTheme
import com.owl.playerdemo.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    
    @Inject
    lateinit var videoService: VideoService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            viewModel = viewModel()
            
            OwlPlayerDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoScreen(viewModel, videoService)
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
package com.owl.playerdemo.ui.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val playerState: StateFlow<PlayerState> = _playerState
    
    // Keep reference to release in onCleared
    private var exoPlayer: ExoPlayer? = null
    
    /**
     * Initialize player with a local video file
     */
    fun initializeWithVideo(videoPath: String, videoTitle: String) {
        viewModelScope.launch {
            try {
                _playerState.value = PlayerState.Loading
                
                // Validate file exists
                val file = File(videoPath)
                if (!file.exists()) {
                    _playerState.value = PlayerState.Error("Video file not found")
                    return@launch
                }
                
                // Create ExoPlayer
                val player = ExoPlayer.Builder(context).build()
                
                // Set up player
                val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                
                // Save reference for cleanup
                exoPlayer = player
                
                _playerState.value = PlayerState.Ready(player)
            } catch (e: Exception) {
                _playerState.value = PlayerState.Error("Failed to play video: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
} 
package com.owl.playerdemo.ui.player

import androidx.media3.exoplayer.ExoPlayer

/**
 * Sealed class representing the different states of the video player
 */
sealed class PlayerState {
    object Loading : PlayerState()
    data class Ready(val player: ExoPlayer) : PlayerState()
    data class Error(val message: String) : PlayerState()
} 
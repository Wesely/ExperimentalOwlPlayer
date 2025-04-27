package com.owl.playerdemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owl.playerdemo.data.service.PexelsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pexelsService: PexelsService
) : ViewModel() {

    fun fetchVideos() {
        viewModelScope.launch {
            try {
                val response = pexelsService.searchVideos(
                    query = "nature",
                    perPage = 1
                )
                println("Pexels API Response: $response")
            } catch (e: Exception) {
                println("Error fetching videos: ${e.message}")
            }
        }
    }
} 
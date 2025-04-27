package com.owl.playerdemo.data.repository

import com.owl.playerdemo.data.model.PexelsSearchResponse
import com.owl.playerdemo.data.model.PexelsVideo
import com.owl.playerdemo.data.service.PexelsService
import javax.inject.Inject

interface PexelsRepository {
    suspend fun searchVideos(query: String, perPage: Int = 15, page: Int = 1): Result<PexelsSearchResponse>
    suspend fun getVideoById(id: Int): Result<PexelsVideo>
}

class PexelsRepositoryImpl @Inject constructor(
    private val service: PexelsService
) : PexelsRepository {
    
    override suspend fun searchVideos(query: String, perPage: Int, page: Int): Result<PexelsSearchResponse> {
        return try {
            val response = service.searchVideos(query, perPage, page)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVideoById(id: Int): Result<PexelsVideo> {
        return try {
            val response = service.getVideoById(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 
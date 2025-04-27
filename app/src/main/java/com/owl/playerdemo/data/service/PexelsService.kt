package com.owl.playerdemo.data.service

import com.owl.playerdemo.data.model.PexelsSearchResponse
import com.owl.playerdemo.data.model.PexelsVideo
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service interface for interacting with the Pexels API.
 * 
 * Usage for infinite scroll/grid:
 * 1. Initial load:
 *    - Start with page = 1
 *    - Use perPage = 15 (or adjust based on your UI)
 *    - Store totalResults from response to know total available items
 * 
 * 2. Pagination:
 *    - Increment page number when user scrolls near the end
 *    - Check if (currentPage * perPage) < totalResults before loading more
 *    - Handle loading states in UI (initial, loading more, error)
 * 
 * Example implementation in ViewModel:
 * ```
 * class VideoListViewModel @Inject constructor(private val service: PexelsService) {
 *     private var currentPage = 1
 *     private var canLoadMore = true
 *     
 *     fun loadNextPage(query: String) {
 *         if (!canLoadMore) return
 *         viewModelScope.launch {
 *             service.searchVideos(
 *                 query = query,
 *                 page = currentPage
 *             ).let { response ->
 *                 // Append videos to your list
 *                 currentPage++
 *                 canLoadMore = (currentPage * 15) < response.totalResults
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface PexelsService {
    /**
     * Search for videos with pagination support.
     * 
     * @param query Search term (e.g., "nature", "city", "abstract")
     * @param perPage Number of items per page (default: 15, max: 80)
     * @param page Page number for pagination (start from 1)
     * @return [PexelsSearchResponse] containing list of videos and pagination info
     * 
     * Note: Rate limits apply - 200 requests per hour
     */
    @GET("videos/search")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1
    ): PexelsSearchResponse

    /**
     * Fetch a specific video by its ID.
     * 
     * @param id Unique identifier of the video
     * @return [PexelsVideo] containing video details and available formats
     * 
     * Tip: Use this when you need to refresh video details or
     * fetch higher quality versions for playback
     */
    @GET("videos/videos/{id}")
    suspend fun getVideoById(
        @Path("id") id: Int
    ): PexelsVideo
} 
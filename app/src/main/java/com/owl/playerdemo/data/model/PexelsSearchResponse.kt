package com.owl.playerdemo.data.model

import com.google.gson.annotations.SerializedName

data class PexelsSearchResponse(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("total_results")
    val totalResults: Int,
    
    @SerializedName("videos")
    val videos: List<PexelsVideo>
) 
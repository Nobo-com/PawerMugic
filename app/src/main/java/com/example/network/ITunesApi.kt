package com.example.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ITunesSearchResponse(
    val resultCount: Int,
    val results: List<ITunesTrack>
)

@JsonClass(generateAdapter = true)
data class ITunesTrack(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val collectionName: String?,
    val previewUrl: String?,
    val artworkUrl100: String?
)

interface ITunesApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") query: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 20
    ): ITunesSearchResponse
}

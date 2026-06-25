package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ITunesNetwork {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val apiRetrofit = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: ITunesApiService = apiRetrofit.create(ITunesApiService::class.java)
}

class ITunesRepository {
    suspend fun searchTracks(query: String): List<ITunesTrack> {
        return try {
            val response = ITunesNetwork.apiService.searchTracks(query = query)
            response.results
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

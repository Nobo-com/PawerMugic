package com.example.models

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // Path to file or URI
    val duration: Long,
    val albumArtUri: String? = null,
    val isOnline: Boolean = false,
    val youtubeId: String? = null
)

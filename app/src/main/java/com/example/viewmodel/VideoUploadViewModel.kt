package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.VideoUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VideoUploadState {
    object Idle : VideoUploadState()
    data class Uploading(val progressMessage: String) : VideoUploadState()
    object Success : VideoUploadState()
    data class Error(val message: String) : VideoUploadState()
}

class VideoUploadViewModel : ViewModel() {
    private val repository = VideoUploadRepository()
    
    private val _uiState = MutableStateFlow<VideoUploadState>(VideoUploadState.Idle)
    val uiState: StateFlow<VideoUploadState> = _uiState.asStateFlow()
    
    private val _videos = MutableStateFlow<List<com.example.repository.TursoVideo>>(emptyList())
    val videos: StateFlow<List<com.example.repository.TursoVideo>> = _videos.asStateFlow()

    init {
        loadVideos()
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            try {
                _videos.value = repository.getVideosFromTurso()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun initializeDatabase() {
        viewModelScope.launch {
            try {
                _uiState.value = VideoUploadState.Uploading("Initializing Turso Database Tables...")
                repository.initializeDatabase()
                _uiState.value = VideoUploadState.Success
            } catch (e: Exception) {
                _uiState.value = VideoUploadState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateFbToken(token: String) {
        viewModelScope.launch {
            try {
                _uiState.value = VideoUploadState.Uploading("Updating Facebook Token in Database...")
                repository.updateFbToken(token)
                _uiState.value = VideoUploadState.Success
            } catch (e: Exception) {
                _uiState.value = VideoUploadState.Error(e.message ?: "Failed to update FB token")
            }
        }
    }

    fun uploadVideo(context: Context, videoUri: Uri, title: String, category: String) {
        viewModelScope.launch {
            try {
                _uiState.value = VideoUploadState.Uploading("Fetching Facebook token from Database...")
                val fbToken = repository.getFbTokenFromTurso()
                
                if (fbToken.isNullOrEmpty()) {
                    _uiState.value = VideoUploadState.Error("Failed to retrieve Facebook Token from Database.")
                    return@launch
                }
                
                _uiState.value = VideoUploadState.Uploading("Uploading video to Facebook (This may take a while)...")
                val fbVideoId = repository.uploadVideoToFacebook(context, videoUri, title, category, fbToken)
                
                if (fbVideoId.isEmpty()) {
                    _uiState.value = VideoUploadState.Error("Failed to get Video ID from Facebook.")
                    return@launch
                }
                
                _uiState.value = VideoUploadState.Uploading("Syncing video to Turso Database...")
                repository.saveVideoToTurso(title, category, fbVideoId)
                
                loadVideos()
                
                _uiState.value = VideoUploadState.Success
            } catch (e: Exception) {
                _uiState.value = VideoUploadState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = VideoUploadState.Idle
    }
}

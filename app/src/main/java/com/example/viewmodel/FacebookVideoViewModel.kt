package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.FacebookVideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FacebookVideoState {
    object Idle : FacebookVideoState()
    object Loading : FacebookVideoState()
    data class Success(val videoUrl: String) : FacebookVideoState()
    data class Error(val message: String) : FacebookVideoState()
}

class FacebookVideoViewModel : ViewModel() {
    private val repository = FacebookVideoRepository()
    
    private val _uiState = MutableStateFlow<FacebookVideoState>(FacebookVideoState.Idle)
    val uiState: StateFlow<FacebookVideoState> = _uiState.asStateFlow()

    fun loadFacebookVideo(fbPublicUrl: String) {
        viewModelScope.launch {
            _uiState.value = FacebookVideoState.Loading
            try {
                // Fetch video source URL from Facebook using scraper
                val videoUrl = repository.extractFacebookVideoUrl(fbPublicUrl)
                
                if (videoUrl.isNullOrEmpty()) {
                    _uiState.value = FacebookVideoState.Error("Failed to extract .mp4 source from Facebook URL.")
                } else {
                    _uiState.value = FacebookVideoState.Success(videoUrl)
                }
                
            } catch (e: Exception) {
                _uiState.value = FacebookVideoState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}

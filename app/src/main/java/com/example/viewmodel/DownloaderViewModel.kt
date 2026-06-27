package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

sealed class DownloaderState {
    object Idle : DownloaderState()
    object Loading : DownloaderState()
    data class Success(
        val hdUrl: String?,
        val sdUrl: String?,
        val currentPreviewUrl: String?
    ) : DownloaderState()
    data class Error(val message: String) : DownloaderState()
}

class DownloaderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DownloaderState>(DownloaderState.Idle)
    val uiState: StateFlow<DownloaderState> = _uiState.asStateFlow()

    fun fetchVideo(url: String) {
        if (url.isBlank()) {
            _uiState.value = DownloaderState.Error("Please enter a valid URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = DownloaderState.Loading
            try {
                val resolvedUrl = resolveUrl(url)
                val html = fetchHtml(resolvedUrl)
                
                val hdUrl = extractUrl(html, arrayOf("browser_native_hd_url", "playable_url_quality_hd"))
                val sdUrl = extractUrl(html, arrayOf("browser_native_sd_url", "playable_url", "og:video:secure_url", "og:video"))
                
                if (hdUrl == null && sdUrl == null) {
                    _uiState.value = DownloaderState.Error("Could not find video links.")
                } else {
                    _uiState.value = DownloaderState.Success(
                        hdUrl = hdUrl,
                        sdUrl = sdUrl,
                        currentPreviewUrl = hdUrl ?: sdUrl
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DownloaderState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
    
    fun switchQuality(url: String?) {
        val currentState = _uiState.value
        if (currentState is DownloaderState.Success && url != null) {
            _uiState.value = currentState.copy(currentPreviewUrl = url)
        }
    }

    private suspend fun resolveUrl(urlStr: String): String = withContext(Dispatchers.IO) {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                if (location != null) {
                    currentUrl = location
                    redirects++
                } else {
                    break
                }
            } else {
                break
            }
        }
        currentUrl
    }

    private suspend fun fetchHtml(urlStr: String): String = withContext(Dispatchers.IO) {
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        
        try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            val responseCode = connection.responseCode
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("HTTP Error $responseCode: ${e.message}\nBody: ${errorBody?.take(100)}")
        }
    }

    private fun extractUrl(html: String, keys: Array<String>): String? {
        for (key in keys) {
            var matcher = Pattern.compile("\"$key\":\"(.*?)\"").matcher(html)
            if (matcher.find()) {
                val match = matcher.group(1)
                if (match != null) {
                    return unescape(match)
                }
            }
            
            if (key == "og:video" || key == "og:video:secure_url") {
                matcher = Pattern.compile("<meta property=\"$key\" content=\"([^\"]+)\"").matcher(html)
                if (matcher.find()) {
                    val match = matcher.group(1)
                    if (match != null) {
                        return unescape(match)
                    }
                }
            }
        }
        return null
    }

    private fun unescape(url: String): String {
        return url.replace("\\/", "/")
                  .replace("&amp;", "&")
                  .replace("\\u0026", "&")
    }
}

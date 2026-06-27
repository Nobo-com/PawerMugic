package com.example.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class FacebookVideoRepository {
    private val client = OkHttpClient()
    
    suspend fun extractFacebookVideoUrl(fbPublicUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(fbPublicUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: return@use null
                    
                    // Try hd_src
                    var matcher = Pattern.compile("\"hd_src\":\"(.*?)\"").matcher(html)
                    if (matcher.find()) {
                        return@use matcher.group(1)?.replace("\\/", "/")
                    }
                    
                    // Try sd_src
                    matcher = Pattern.compile("\"sd_src\":\"(.*?)\"").matcher(html)
                    if (matcher.find()) {
                        return@use matcher.group(1)?.replace("\\/", "/")
                    }

                    // Try og:video
                    matcher = Pattern.compile("<meta property=\"og:video\" content=\"([^\"]+)\"").matcher(html)
                    if (matcher.find()) {
                        return@use matcher.group(1)?.replace("&amp;", "&")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}

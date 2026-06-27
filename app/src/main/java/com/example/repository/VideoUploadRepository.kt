package com.example.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TursoVideo(val title: String, val category: String, val fbPublicUrl: String)

class VideoUploadRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
        
    private val tursoUrl = BuildConfig.TURSO_DB_URL
    private val tursoToken = BuildConfig.TURSO_READ_TOKEN

    private fun getApiUrl(): String {
        return if (tursoUrl.endsWith("/v2/pipeline")) tursoUrl
        else "${tursoUrl.trimEnd('/')}/v2/pipeline"
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            throw Exception("Turso DB URL is not configured.")
        }

        val createConfigTable = "CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)"
        val createVideosTable = "CREATE TABLE IF NOT EXISTS videos_v2 (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, category TEXT, fb_public_url TEXT)"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        requestsArray.put(JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply { put("sql", createConfigTable) })
        })
        requestsArray.put(JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply { put("sql", createVideosTable) })
        })
        
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(getApiUrl())
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to initialize database: ${response.body?.string()}")
        }
    }

    suspend fun saveVideoToTurso(title: String, category: String, fbPublicUrl: String) = withContext(Dispatchers.IO) {
        val sqlQuery = "INSERT INTO videos_v2 (title, category, fb_public_url) VALUES (?, ?, ?)"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val argsArray = JSONArray().apply {
            put(JSONObject().apply { put("type", "text"); put("value", title) })
            put(JSONObject().apply { put("type", "text"); put("value", category) })
            put(JSONObject().apply { put("type", "text"); put("value", fbPublicUrl) })
        }
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
                put("args", argsArray)
            })
        }
        
        requestsArray.put(executeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(getApiUrl())
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to save to Turso DB: ${response.body?.string()}")
            
            val responseData = response.body?.string() ?: throw IOException("Empty response from Turso")
            val tursoResponse = JSONObject(responseData)
            val results = tursoResponse.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                if (firstResult.has("error")) {
                    val errorMsg = firstResult.getJSONObject("error").optString("message", "Unknown error")
                    throw Exception("Turso DB Error: $errorMsg")
                }
            }
        }
    }
    
    suspend fun getVideosFromTurso(): List<TursoVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<TursoVideo>()
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            return@withContext videos
        }
        
        val sqlQuery = "SELECT title, category, fb_public_url FROM videos_v2 ORDER BY id DESC"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
            })
        }
        
        requestsArray.put(executeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(getApiUrl())
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                
                val responseData = response.body?.string() ?: return@use
                val tursoResponse = JSONObject(responseData)
                
                val results = tursoResponse.optJSONArray("results") ?: return@use
                if (results.length() > 0) {
                    val firstResult = results.getJSONObject(0)
                    if (firstResult.has("error")) {
                        println("Turso DB Error in getVideos: " + firstResult.getJSONObject("error").optString("message"))
                        return@use
                    }
                    val resp = firstResult.optJSONObject("response")
                    val resultObj = resp?.optJSONObject("result")
                    val rows = resultObj?.optJSONArray("rows")
                    
                    if (rows != null) {
                        for (i in 0 until rows.length()) {
                            val row = rows.getJSONArray(i)
                            if (row.length() >= 3) {
                                val title = row.getJSONObject(0).optString("value", "")
                                val category = row.getJSONObject(1).optString("value", "")
                                val fbPublicUrl = row.getJSONObject(2).optString("value", "")
                                videos.add(TursoVideo(title, category, fbPublicUrl))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext videos
    }
}

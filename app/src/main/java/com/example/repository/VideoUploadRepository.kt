package com.example.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class TursoVideo(val title: String, val category: String, val fbVideoId: String)

class VideoUploadRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
        
    private val tursoUrl = BuildConfig.TURSO_DB_URL
    private val tursoToken = BuildConfig.TURSO_READ_TOKEN

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            throw Exception("Turso DB URL is not configured.")
        }

        val createConfigTable = "CREATE TABLE IF NOT EXISTS app_config (key TEXT PRIMARY KEY, value TEXT)"
        val createVideosTable = "CREATE TABLE IF NOT EXISTS videos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, category TEXT, fb_video_id TEXT)"
        
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
        requestsArray.put(JSONObject().apply {
            put("type", "close")
            put("stmt", JSONObject().apply { put("sql", "") })
        })
        
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(tursoUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to initialize database: ${response.body?.string()}")
        }
    }

    suspend fun updateFbToken(token: String) = withContext(Dispatchers.IO) {
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            throw Exception("Turso DB URL is not configured.")
        }

        // Insert or replace fb_token
        val sqlQuery = "INSERT OR REPLACE INTO app_config (key, value) VALUES ('fb_token', ?)"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val argsArray = JSONArray().apply {
            put(JSONObject().apply { put("type", "text"); put("value", token) })
        }
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
                put("args", argsArray)
            })
        }
        
        val closeStmt = JSONObject().apply {
            put("type", "close")
            put("stmt", JSONObject().apply { put("sql", "") })
        }
        
        requestsArray.put(executeStmt)
        requestsArray.put(closeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(tursoUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to save FB token: ${response.body?.string()}")
        }
    }

    suspend fun getFbTokenFromTurso(): String? = withContext(Dispatchers.IO) {
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            throw Exception("Turso DB URL is not configured.")
        }
        
        val sqlQuery = "SELECT value FROM app_config WHERE key = 'fb_token' LIMIT 1"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
            })
        }
        
        val closeStmt = JSONObject().apply {
            put("type", "close")
            put("stmt", JSONObject().apply {
                put("sql", "")
            })
        }
        
        requestsArray.put(executeStmt)
        requestsArray.put(closeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(tursoUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to get token: $response")
            
            val responseData = response.body?.string() ?: return@use null
            val tursoResponse = JSONObject(responseData)
            
            val results = tursoResponse.optJSONArray("results") ?: return@use null
            if (results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                val resp = firstResult.optJSONObject("response")
                val resultObj = resp?.optJSONObject("result")
                val rows = resultObj?.optJSONArray("rows")
                
                if (rows != null && rows.length() > 0) {
                    val firstRow = rows.getJSONArray(0)
                    if (firstRow.length() > 0) {
                        val valueObj = firstRow.getJSONObject(0)
                        return@use valueObj.optString("value", null)
                    }
                }
            }
            return@use null
        }
    }
    
    suspend fun uploadVideoToFacebook(
        context: Context,
        videoUri: Uri,
        title: String,
        category: String,
        fbToken: String
    ): String = withContext(Dispatchers.IO) {
        val file = getFileFromUri(context, videoUri) ?: throw Exception("Cannot read video file")
        
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("access_token", fbToken)
            .addFormDataPart("title", title)
            .addFormDataPart("description", "Category: $category")
            .addFormDataPart(
                "source",
                file.name,
                file.asRequestBody("video/mp4".toMediaTypeOrNull())
            )
            .build()
            
        val request = Request.Builder()
            .url("https://graph-video.facebook.com/v18.0/me/videos")
            .post(requestBody)
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Upload failed: ${response.body?.string()}")
            
            val responseData = response.body?.string() ?: throw Exception("Empty response from Facebook")
            val jsonResponse = JSONObject(responseData)
            
            return@use jsonResponse.optString("id")
        }
    }
    
    suspend fun saveVideoToTurso(title: String, category: String, fbVideoId: String) = withContext(Dispatchers.IO) {
        val sqlQuery = "INSERT INTO videos (title, category, fb_video_id) VALUES (?, ?, ?)"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val argsArray = JSONArray().apply {
            put(JSONObject().apply { put("type", "text"); put("value", title) })
            put(JSONObject().apply { put("type", "text"); put("value", category) })
            put(JSONObject().apply { put("type", "text"); put("value", fbVideoId) })
        }
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
                put("args", argsArray)
            })
        }
        
        val closeStmt = JSONObject().apply {
            put("type", "close")
            put("stmt", JSONObject().apply {
                put("sql", "")
            })
        }
        
        requestsArray.put(executeStmt)
        requestsArray.put(closeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(tursoUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $tursoToken")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to save to Turso DB: ${response.body?.string()}")
        }
    }
    
    suspend fun getVideosFromTurso(): List<TursoVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<TursoVideo>()
        if (tursoUrl.isBlank() || tursoUrl.contains("YOUR_TURSO")) {
            return@withContext videos
        }
        
        val sqlQuery = "SELECT title, category, fb_video_id FROM videos ORDER BY id DESC"
        
        val requestObj = JSONObject()
        val requestsArray = JSONArray()
        
        val executeStmt = JSONObject().apply {
            put("type", "execute")
            put("stmt", JSONObject().apply {
                put("sql", sqlQuery)
            })
        }
        
        val closeStmt = JSONObject().apply {
            put("type", "close")
            put("stmt", JSONObject().apply {
                put("sql", "")
            })
        }
        
        requestsArray.put(executeStmt)
        requestsArray.put(closeStmt)
        requestObj.put("requests", requestsArray)
        
        val body = requestObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(tursoUrl)
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
                    val resp = firstResult.optJSONObject("response")
                    val resultObj = resp?.optJSONObject("result")
                    val rows = resultObj?.optJSONArray("rows")
                    
                    if (rows != null) {
                        for (i in 0 until rows.length()) {
                            val row = rows.getJSONArray(i)
                            if (row.length() >= 3) {
                                val title = row.getJSONObject(0).optString("value", "")
                                val category = row.getJSONObject(1).optString("value", "")
                                val fbVideoId = row.getJSONObject(2).optString("value", "")
                                videos.add(TursoVideo(title, category, fbVideoId))
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
    
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            var fileName = "temp_video.mp4"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
            return tempFile
        }
        return null
    }
}

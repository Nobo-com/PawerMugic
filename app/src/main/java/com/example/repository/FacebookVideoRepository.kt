package com.example.repository

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

class FacebookVideoRepository {
    private val client = OkHttpClient()
    
    suspend fun getFbTokenFromTurso(): String? = withContext(Dispatchers.IO) {
        val url = BuildConfig.TURSO_DB_URL
        val token = BuildConfig.TURSO_READ_TOKEN
        
        if (url.isBlank() || url.contains("YOUR_TURSO")) {
            throw Exception("Turso DB URL is not configured. Please add it to the Secrets panel.")
        }
        
        val apiUrl = if (url.endsWith("/v2/pipeline")) url else "${url.trimEnd('/')}/v2/pipeline"
        
        // Construct the Turso request to get the FB token using org.json
        val sqlQuery = "SELECT value FROM app_config WHERE key = 'fb_token' LIMIT 1"
        
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
        
        val jsonBody = requestObj.toString()
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            
            val responseData = response.body?.string() ?: return@use null
            val tursoResponse = JSONObject(responseData)
            
            // Extract the value from the first row, first column
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
    
    suspend fun getFacebookVideoSource(fbVideoId: String, fbToken: String): String? = withContext(Dispatchers.IO) {
        val fbUrl = "https://graph.facebook.com/v18.0/$fbVideoId?fields=source&access_token=$fbToken"
        
        val request = Request.Builder()
            .url(fbUrl)
            .get()
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch FB video source: $response")
            
            val responseData = response.body?.string() ?: return@use null
            val fbResponse = JSONObject(responseData)
            
            return@use fbResponse.optString("source", null)
        }
    }
}

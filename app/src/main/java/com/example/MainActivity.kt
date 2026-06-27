package com.example

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FBDownloaderScreen()
            }
        }
    }
}

data class VideoOptions(
    val title: String,
    val description: String,
    val hdUrl: String?,
    val sdUrl: String?
)

class DownloaderViewModel : ViewModel() {
    private val _urlInput = MutableStateFlow("https://www.facebook.com/share/v/19AExLG3zX/")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _videoOptions = MutableStateFlow<VideoOptions?>(null)
    val videoOptions: StateFlow<VideoOptions?> = _videoOptions.asStateFlow()

    fun setUrl(url: String) {
        _urlInput.value = url
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAll() {
        _urlInput.value = ""
        _videoOptions.value = null
        _errorMessage.value = null
    }

    private fun shouldResolveRedirect(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("fb.watch") || 
               lower.contains("facebook.com/share") || 
               lower.contains("fb.me") || 
               (lower.contains("fb.com") && !lower.contains("facebook.com/reel") && !lower.contains("facebook.com/watch") && !lower.contains("facebook.com/videos"))
    }

    private suspend fun resolveRedirect(urlStr: String): String = withContext(Dispatchers.IO) {
        val trimmed = urlStr.trim()
        if (!shouldResolveRedirect(trimmed)) {
            return@withContext trimmed.replace("&amp;", "&")
        }
        try {
            var currentUrl = trimmed
            for (i in 0..4) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        val nextUrl = if (loc.startsWith("/")) {
                            val originalUrl = java.net.URL(currentUrl)
                            "${originalUrl.protocol}://${originalUrl.host}$loc"
                        } else {
                            loc
                        }
                        
                        // If redirect leads to login, skip following it!
                        if (nextUrl.contains("login") || nextUrl.contains("checkpoint") || nextUrl.contains("cookie")) {
                            connection.disconnect()
                            break
                        }
                        currentUrl = nextUrl.replace("&amp;", "&")
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
                connection.disconnect()
            }
            currentUrl.replace("&amp;", "&")
        } catch (e: Exception) {
            trimmed.replace("&amp;", "&")
        }
    }

    private suspend fun extractVideo(fbUrl: String): VideoOptions? = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveRedirect(fbUrl)
        
        // Strategy 0: Cobalt (Dynamic & Static fallback list)
        try {
            val instances = mutableListOf<String>()
            try {
                val listUrl = java.net.URL("https://instances.cobalt.tools/api/instances")
                val conn = listUrl.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode in 200..299) {
                    val json = conn.inputStream.bufferedReader().readText()
                    val urlRegex = """"(apiUrl|url)"\s*:\s*"(https://[^"]+)"""".toRegex()
                    val matches = urlRegex.findAll(json).map { it.groups[2]?.value }.filterNotNull().distinct().toList()
                    instances.addAll(matches)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val fallbacks = listOf(
                "https://api.cobalt.tools",
                "https://co.wuk.sh",
                "https://cobalt.best",
                "https://api.cobalt.best",
                "https://cobalt.sh",
                "https://kuss.pub"
            )
            val finalInstances = (fallbacks + instances).distinct()

            for (baseUrl in finalInstances) {
                val cleanBaseUrl = baseUrl.trimEnd('/')
                try {
                    val url = java.net.URL(cleanBaseUrl)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.doOutput = true

                    val payload = "{\"url\":\"$resolvedUrl\"}"
                    conn.outputStream.write(payload.toByteArray(Charsets.UTF_8))
                    conn.outputStream.flush()
                    conn.outputStream.close()

                    if (conn.responseCode in 200..299) {
                        val responseJson = conn.inputStream.bufferedReader().readText()
                        val directUrlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
                        val extractedUrl = directUrlRegex.find(responseJson)?.groups?.get(1)?.value?.replace("\\/", "/")
                        
                        if (!extractedUrl.isNullOrBlank()) {
                            return@withContext VideoOptions(
                                title = "Facebook Video",
                                description = "Extracted successfully via Cobalt Server ($cleanBaseUrl)",
                                hdUrl = extractedUrl,
                                sdUrl = extractedUrl
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Strategy 1: FDown.net (with enhanced headers to mimic web browser)
        try {
            val url = java.net.URL("https://fdown.net/download.php")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Origin", "https://fdown.net")
            connection.setRequestProperty("Referer", "https://fdown.net/")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            val postData = "URLz=" + java.net.URLEncoder.encode(resolvedUrl, "UTF-8")
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))
            connection.outputStream.flush()
            connection.outputStream.close()

            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()

                val hdRegex = """href="([^"]+)"[^>]*id="hdlink"""".toRegex()
                val sdRegex = """href="([^"]+)"[^>]*id="sdlink"""".toRegex()

                val hdUrl = hdRegex.find(html)?.groups?.get(1)?.value?.replace("&amp;", "&")
                val sdUrl = sdRegex.find(html)?.groups?.get(1)?.value?.replace("&amp;", "&")

                if (hdUrl != null || sdUrl != null) {
                    val titleRegex = """<div class="lib-text-container">[\s\S]*?<p class="lib-text">([\s\S]*?)</p>""".toRegex()
                    val descRegex = """<strong>Description:</strong>\s*([^<]+)""".toRegex()

                    var title = titleRegex.find(html)?.groups?.get(1)?.value?.trim() ?: ""
                    if (title.isBlank() || title == "No video title") {
                        title = "Facebook Video"
                    }
                    
                    var description = descRegex.find(html)?.groups?.get(1)?.value?.trim() ?: ""
                    if (description.isBlank() || description.startsWith("No video description")) {
                        description = "Ready for download"
                    }

                    return@withContext VideoOptions(
                        title = title,
                        description = description,
                        hdUrl = hdUrl,
                        sdUrl = sdUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 2: Getfvid.com
        try {
            val url = java.net.URL("https://www.getfvid.com/download")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Origin", "https://www.getfvid.com")
            connection.setRequestProperty("Referer", "https://www.getfvid.com/")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            val postData = "url=" + java.net.URLEncoder.encode(resolvedUrl, "UTF-8")
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))
            connection.outputStream.flush()
            connection.outputStream.close()

            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()
                val buttonRegex = """href="([^"]+)"[^>]*class="btn btn-download[^"]*"""".toRegex()
                val matches = buttonRegex.findAll(html).map { it.groups[1]?.value?.replace("&amp;", "&") }.toList()
                
                if (matches.isNotEmpty()) {
                    val hdUrl = matches.firstOrNull { it?.contains("hd") == true || it?.contains("video-hd") == true } ?: matches.firstOrNull()
                    val sdUrl = matches.firstOrNull { it?.contains("sd") == true || it?.contains("video-sd") == true } ?: matches.getOrNull(1) ?: matches.firstOrNull()

                    return@withContext VideoOptions(
                        title = "Facebook Video",
                        description = "Extracted via Backup Server",
                        hdUrl = hdUrl,
                        sdUrl = sdUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 3: Direct Meta & JSON Extraction (Highly robust direct extraction)
        try {
            val url = java.net.URL(resolvedUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            
            if (connection.responseCode in 200..299) {
                val html = connection.inputStream.bufferedReader().readText()
                
                val ogVideoRegex = """<meta\s+property="og:video:secure_url"\s+content="([^"]+)"""".toRegex()
                val ogVideoRegex2 = """<meta\s+property="og:video"\s+content="([^"]+)"""".toRegex()
                val sdVideoRegex = """"browser_native_sd_url"\s*:\s*"([^"]+)"""".toRegex()
                val hdVideoRegex = """"browser_native_hd_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableUrlRegex = """"playable_url"\s*:\s*"([^"]+)"""".toRegex()
                val playableUrlHdRegex = """"playable_url_quality_hd"\s*:\s*"([^"]+)"""".toRegex()
                
                var hdUrl: String? = hdVideoRegex.find(html)?.groups?.get(1)?.value
                    ?: playableUrlHdRegex.find(html)?.groups?.get(1)?.value
                
                var sdUrl: String? = sdVideoRegex.find(html)?.groups?.get(1)?.value
                    ?: playableUrlRegex.find(html)?.groups?.get(1)?.value
                
                val ogUrl = ogVideoRegex.find(html)?.groups?.get(1)?.value
                    ?: ogVideoRegex2.find(html)?.groups?.get(1)?.value
                
                // Sanitize matches
                hdUrl = hdUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                sdUrl = sdUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                val ogSanitized = ogUrl?.replace("&amp;", "&")?.replace("\\/", "/")
                
                // Fallbacks
                if (hdUrl.isNullOrBlank()) {
                    hdUrl = ogSanitized ?: sdUrl
                }
                if (sdUrl.isNullOrBlank()) {
                    sdUrl = ogSanitized ?: hdUrl
                }
                
                if (!hdUrl.isNullOrBlank() || !sdUrl.isNullOrBlank()) {
                    return@withContext VideoOptions(
                        title = "Facebook Video",
                        description = "Directly extracted from Facebook Page",
                        hdUrl = hdUrl ?: sdUrl,
                        sdUrl = sdUrl ?: hdUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    fun extractOptions() {
        val fbUrl = _urlInput.value.trim()
        if (fbUrl.isEmpty()) {
            _errorMessage.value = "Please enter a valid URL."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _videoOptions.value = null
            
            val options = extractVideo(fbUrl)
            if (options != null) {
                _videoOptions.value = options
            } else {
                _errorMessage.value = "Failed to extract. Please make sure the video is public and the link is correct."
            }
            _isLoading.value = false
        }
    }

    fun triggerDownload(context: Context, url: String, isHd: Boolean) {
        try {
            val qualityTag = if (isHd) "HD" else "SD"
            val fileName = "FB_Video_${qualityTag}_${System.currentTimeMillis()}.mp4"
            
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading Facebook video...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started ($qualityTag)...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to start download. Please check storage permissions."
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FBDownloaderScreen(viewModel: DownloaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val urlInput by viewModel.urlInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val videoOptions by viewModel.videoOptions.collectAsState()
    val selectedUrl = remember(videoOptions) {
        mutableStateOf(videoOptions?.hdUrl ?: videoOptions?.sdUrl ?: "")
    }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FB Downloader Pro", 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Hero Header Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Icon",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Download Facebook Videos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Paste any video, reel, or watch link to extract instantly",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // URL input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.setUrl(it) },
                        label = { Text("Video URL") },
                        placeholder = { Text("https://www.facebook.com/...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "Link")
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearAll() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrEmpty()) {
                                    viewModel.setUrl(clip)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Link", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.extractOptions() },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading && urlInput.isNotBlank(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Results Card
            AnimatedVisibility(
                visible = videoOptions != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                videoOptions?.let { options ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ready to Download",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = options.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = options.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Native Stream Player
                            VideoPreviewPlayer(
                                url = selectedUrl.value,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stream Quality Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Preview Quality:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (options.hdUrl != null) {
                                        val isSelected = selectedUrl.value == options.hdUrl
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedUrl.value = options.hdUrl },
                                            label = { Text("HD Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                    if (options.sdUrl != null) {
                                        val isSelected = selectedUrl.value == options.sdUrl
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedUrl.value = options.sdUrl },
                                            label = { Text("SD Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Direct Database Link display box
                            Text(
                                text = "DIRECT DATABASE/SERVER LINK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedUrl.value,
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedUrl.value))
                                            Toast.makeText(context, "Direct link copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy link",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "SELECT QUALITY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (options.hdUrl != null) {
                                    ElevatedCard(
                                        onClick = { viewModel.triggerDownload(context, options.hdUrl, true) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.HighQuality,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "High Definition (HD)",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = "Best quality 720p / 1080p",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download HD",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                
                                if (options.sdUrl != null) {
                                    ElevatedCard(
                                        onClick = { viewModel.triggerDownload(context, options.sdUrl, false) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Sd,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Standard Quality (SD)",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = "Normal quality 360p / 480p",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download SD",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPreviewPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    var isPrepared by remember(url) { mutableStateOf(false) }
    var hasError by remember(url) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError && url.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoURI(Uri.parse(url))
                        
                        setOnPreparedListener { mediaPlayer ->
                            isPrepared = true
                            mediaPlayer.isLooping = true
                            start()
                        }
                        
                        setOnErrorListener { _, _, _ ->
                            hasError = true
                            true
                        }
                    }
                },
                update = { view ->
                    try {
                        val currentTag = view.tag as? String
                        if (currentTag != url) {
                            view.tag = url
                            isPrepared = false
                            hasError = false
                            view.setVideoURI(Uri.parse(url))
                            view.start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (url.isEmpty()) {
            Text(
                text = "No direct stream URL available",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        } else if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Streaming Preview direct link loaded.",
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Direct database/server link is ready for play or download.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else if (!isPrepared) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading direct stream from server database...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

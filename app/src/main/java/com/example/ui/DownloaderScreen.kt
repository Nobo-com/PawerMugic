package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.MediaController
import android.widget.VideoView
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.DownloaderState
import com.example.viewmodel.DownloaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(
    viewModel: DownloaderViewModel = viewModel()
) {
    var urlInput by remember { mutableStateOf("https://www.facebook.com/share/v/19AExLG3zX/") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("FB Video Downloader", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Facebook Video URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFBB86FC),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color(0xFFBB86FC),
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.fetchVideo(urlInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fetch / Download", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is DownloaderState.Idle -> {
                    Text("Enter a URL to start", color = Color.Gray)
                }
                is DownloaderState.Loading -> {
                    CircularProgressIndicator(color = Color(0xFFBB86FC))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Extracting video links...", color = Color.Gray)
                }
                is DownloaderState.Error -> {
                    Text(text = state.message, color = Color(0xFFCF6679))
                }
                is DownloaderState.Success -> {
                    state.currentPreviewUrl?.let { previewUrl ->
                        VideoPreviewPlayer(videoUrl = previewUrl)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (state.hdUrl != null) {
                            FilterChip(
                                selected = state.currentPreviewUrl == state.hdUrl,
                                onClick = { viewModel.switchQuality(state.hdUrl) },
                                label = { Text("HD Quality") },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFBB86FC),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        if (state.sdUrl != null) {
                            FilterChip(
                                selected = state.currentPreviewUrl == state.sdUrl,
                                onClick = { viewModel.switchQuality(state.sdUrl) },
                                label = { Text("SD Quality") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFBB86FC),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    state.currentPreviewUrl?.let { currentUrl ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentUrl,
                                    color = Color(0xFF03DAC6),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = ClipData.newPlainText("Video URL", currentUrl)
                                    clipboardManager.setPrimaryClip(clipData)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy URL",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Download Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.hdUrl != null) {
                        DownloadOptionCard("Download HD Video", Color(0xFFBB86FC)) {
                            // Intent to browser or DownloadManager could be implemented here
                            Toast.makeText(context, "Downloading HD...", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (state.sdUrl != null) {
                        DownloadOptionCard("Download SD Video", Color(0xFF03DAC6)) {
                            Toast.makeText(context, "Downloading SD...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPreviewPlayer(videoUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.Black, RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    setVideoURI(Uri.parse(videoUrl))
                    requestFocus()
                    start()
                }
            },
            update = { view ->
                view.setVideoURI(Uri.parse(videoUrl))
                view.start()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadOptionCard(title: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}

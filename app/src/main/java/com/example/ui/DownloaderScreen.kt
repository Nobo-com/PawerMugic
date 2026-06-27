package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.DownloaderState
import com.example.viewmodel.VideoDownloaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(
    viewModel: VideoDownloaderViewModel = viewModel()
) {
    var urlInput by remember { mutableStateOf("https://www.facebook.com/share/v/19AExLG3zX/") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = { Text("FB Video Downloader", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show input when not successfully loaded yet, to save space, or always show?
            // The screenshot shows the success state. I will show input above.
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Facebook Video URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF475569),
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.fetchVideo(urlInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Fetch / Download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is DownloaderState.Idle -> {
                    Text("Enter a URL to start", color = Color(0xFF94A3B8))
                }
                is DownloaderState.Loading -> {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Extracting video links...", color = Color(0xFF94A3B8))
                }
                is DownloaderState.Error -> {
                    Text(text = state.message, color = Color(0xFFEF4444), modifier = Modifier.padding(16.dp))
                }
                is DownloaderState.Success -> {
                    // Success UI matching the screenshot exactly
                    Text(
                        text = "Directly extracted from Facebook Page",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    state.currentPreviewUrl?.let { previewUrl ->
                        VideoPreviewPlayer(videoUrl = previewUrl)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📺 Preview Quality:",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (state.hdUrl != null) {
                            val isSelected = state.currentPreviewUrl == state.hdUrl
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) Color(0xFF6366F1) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.switchQuality(state.hdUrl) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "HD Preview",
                                    color = if (isSelected) Color(0xFF818CF8) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (state.sdUrl != null) {
                            val isSelected = state.currentPreviewUrl == state.sdUrl
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) Color(0xFF6366F1) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.switchQuality(state.sdUrl) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "SD Preview",
                                    color = if (isSelected) Color(0xFF818CF8) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "DIRECT DATABASE/SERVER LINK",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    state.currentPreviewUrl?.let { currentUrl ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentUrl,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF334155))
                                        .clickable {
                                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clipData = ClipData.newPlainText("Video URL", currentUrl)
                                            clipboardManager.setPrimaryClip(clipData)
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(6.dp)
                                ) {
                                    CopyIcon(tint = Color(0xFFCBD5E1))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "SELECT QUALITY",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.hdUrl != null) {
                        DownloadOptionCard(
                            title = "High Definition (HD)",
                            subtitle = "Best quality 720p / 1080p",
                            badgeText = "HQ",
                            bgColor = Color(0xFF4F46E5), // Indigo
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.hdUrl))
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (state.sdUrl != null) {
                        DownloadOptionCard(
                            title = "Standard Quality (SD)",
                            subtitle = "Normal quality 360p / 480p",
                            badgeText = "SD",
                            bgColor = Color(0xFF334155), // Slate
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.sdUrl))
                                context.startActivity(intent)
                            }
                        )
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
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
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

@Composable
fun DownloadOptionCard(title: String, subtitle: String, badgeText: String, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Text(badgeText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                DownloadIcon(tint = Color.White)
            }
        }
    }
}

@Composable
fun CopyIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // A simple copy icon (two overlapping rectangles)
        drawRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        val bgPath = Path().apply {
            moveTo(w * 0.7f, h * 0.2f)
            lineTo(w * 0.7f, h * 0.1f)
            lineTo(w * 0.1f, h * 0.1f)
            lineTo(w * 0.1f, h * 0.7f)
            lineTo(w * 0.2f, h * 0.7f)
        }
        drawPath(
            path = bgPath,
            color = tint,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun DownloadIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.65f)
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.85f),
            end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.85f),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

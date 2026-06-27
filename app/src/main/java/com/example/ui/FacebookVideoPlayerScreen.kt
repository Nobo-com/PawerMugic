package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.viewmodel.FacebookVideoViewModel
import com.example.viewmodel.FacebookVideoState

@Composable
fun FacebookVideoPlayerScreen(
    fbPublicUrl: String,
    onBack: () -> Unit,
    viewModel: FacebookVideoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(fbPublicUrl) {
        viewModel.loadFacebookVideo(fbPublicUrl)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        
        when (val state = uiState) {
            is FacebookVideoState.Idle, is FacebookVideoState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            is FacebookVideoState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                    Button(onClick = { viewModel.loadFacebookVideo(fbPublicUrl) }) {
                        Text("Retry")
                    }
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            is FacebookVideoState.Success -> {
                VideoPlayerContent(videoSourceUrl = state.videoUrl, onBack = onBack)
            }
        }
    }
}

@Composable
private fun VideoPlayerContent(
    videoSourceUrl: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoSourceUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            setFullscreen(context, false)
        }
    }

    BackHandler(enabled = true) {
        if (isLocked) {
            // Do nothing if locked
        } else if (isFullscreen) {
            setFullscreen(context, false)
            isFullscreen = false
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = !isLocked
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay Controls
        if (isLocked) {
            // Only show unlock button
            IconButton(
                onClick = {
                    isLocked = false
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Screen",
                    tint = Color.White
                )
            }
        } else {
            // Show Lock & Fullscreen buttons in corners over the default ExoPlayer controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { isLocked = true }) {
                    Icon(
                        imageVector = Icons.Default.Clear, // Use a clear icon as Lock Open placeholder
                        contentDescription = "Lock Screen",
                        tint = Color.White
                    )
                }
            }
            
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    }
}

private fun setFullscreen(context: Context, isFullscreen: Boolean) {
    val activity = context as? Activity ?: return
    if (isFullscreen) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

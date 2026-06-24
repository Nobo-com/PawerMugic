package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.models.Song
import com.example.viewmodel.MusicViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Local", "Online")
    
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    
    var showFullPlayer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PowerMusic", fontWeight = FontWeight.Bold, color = com.example.ui.theme.PowerAccent) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = com.example.ui.theme.PowerBackground,
                        titleContentColor = com.example.ui.theme.PowerAccent
                    )
                )
            },
            bottomBar = {
                if (currentSong != null) {
                    MiniPlayer(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress,
                        duration = duration,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { showFullPlayer = true }
                    )
                }
            },
            containerColor = com.example.ui.theme.PowerBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = com.example.ui.theme.PowerBackground,
                    contentColor = com.example.ui.theme.PowerAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = com.example.ui.theme.PowerAccent
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) },
                            selectedContentColor = com.example.ui.theme.PowerAccent,
                            unselectedContentColor = com.example.ui.theme.PowerTextSecondary
                        )
                    }
                }

                if (selectedTabIndex == 0) {
                    LocalLibraryScreen(viewModel)
                } else {
                    OnlineLibraryScreen(viewModel)
                }
            }
        }

        // Full Screen Animated Player
        androidx.compose.animation.AnimatedVisibility(
            visible = showFullPlayer && currentSong != null,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it }
            ),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it }
            )
        ) {
            currentSong?.let { song ->
                SleekPlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    progress = progress,
                    duration = duration,
                    isShuffleEnabled = isShuffleEnabled,
                    isRepeatEnabled = isRepeatEnabled,
                    isFavorited = favoriteSongs.contains(song.id),
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextSong() },
                    onPrevious = { viewModel.previousSong() },
                    onShuffleToggle = { viewModel.toggleShuffle() },
                    onRepeatToggle = { viewModel.toggleRepeat() },
                    onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                    onSeek = { position -> viewModel.seekTo(position.toLong()) },
                    onCollapse = { showFullPlayer = false }
                )
            }
        }
    }
}

@Composable
fun LocalLibraryScreen(viewModel: MusicViewModel) {
    val songs by viewModel.localSongs.collectAsStateWithLifecycle()
    
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No local music found.", color = com.example.ui.theme.PowerTextSecondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) })
            }
        }
    }
}

@Composable
fun OnlineLibraryScreen(viewModel: MusicViewModel) {
    val songs by viewModel.onlineSongs.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(com.example.ui.theme.PowerSurface)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = com.example.ui.theme.PowerTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search YouTube...", color = com.example.ui.theme.PowerTextSecondary)
            }
        }
        
        Text(
            text = "Trending on YouTube",
            color = com.example.ui.theme.PowerText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                SongItem(song = song, onClick = { viewModel.playSong(song) }, isOnline = true)
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit, isOnline: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val defaultImage = if (isOnline) Icons.Default.Cloud else Icons.Default.MusicNote
        
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(defaultImage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = com.example.ui.theme.PowerText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = com.example.ui.theme.PowerTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (isOnline) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play Online", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    duration: Long,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    val progressPercent = if (duration > 0) progress / duration else 0f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.example.ui.theme.PowerSurface)
            .clickable(onClick = onClick)
    ) {
        LinearProgressIndicator(
            progress = { progressPercent },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = com.example.ui.theme.PowerAccent,
            trackColor = com.example.ui.theme.PowerProgressTrack,
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.PowerBackground),
                contentAlignment = Alignment.Center
            ) {
                 if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = com.example.ui.theme.PowerAccent)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = com.example.ui.theme.PowerText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = com.example.ui.theme.PowerTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = com.example.ui.theme.PowerAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SleekPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    duration: Long,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    isFavorited: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onCollapse: () -> Unit
) {
    var sliderValue by remember(progress) { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F1115), Color(0xFF14161B))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2228))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = com.example.ui.theme.PowerTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (song.isOnline) "Streaming Mode" else "Offline Mode",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2228))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.White
                )
            }
        }

        // Album Art Space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow backdrop effect
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors = listOf(Color(0x3B35CCFF), Color.Transparent)))
            )

            // Album Art Container
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF23262E), Color(0xFF14161B))))
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = com.example.ui.theme.PowerAccent,
                        modifier = Modifier.size(96.dp).align(Alignment.Center)
                    )
                }

                // Inner overlays for premium look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                )

                // Bottom badge and favorite icon
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x66000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (song.isOnline) "AAC 320K" else "HI-RES 96K",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x4D000000))
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Song Information
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = com.example.ui.theme.PowerTextSecondary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Slider Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Slider(
                value = if (isDragging) sliderValue else progress,
                onValueChange = {
                    isDragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek(sliderValue)
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = com.example.ui.theme.PowerAccent,
                    activeTrackColor = com.example.ui.theme.PowerAccent,
                    inactiveTrackColor = com.example.ui.theme.PowerProgressTrack
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime((if (isDragging) sliderValue else progress).toLong()),
                    color = com.example.ui.theme.PowerTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(duration),
                    color = com.example.ui.theme.PowerTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) com.example.ui.theme.PowerAccent else com.example.ui.theme.PowerTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2228))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(com.example.ui.theme.PowerAccent)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF0F1115),
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2228))
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            IconButton(
                onClick = onRepeatToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeatEnabled) com.example.ui.theme.PowerAccent else com.example.ui.theme.PowerTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

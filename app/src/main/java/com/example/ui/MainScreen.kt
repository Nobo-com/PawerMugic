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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.models.Song
import com.example.viewmodel.MusicViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startapp.sdk.ads.banner.Banner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    val onlineSongs by viewModel.onlineSongs.collectAsStateWithLifecycle()
    
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
                Column {
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
                    AndroidView(
                        factory = { context ->
                            try {
                                Banner(context).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                }
                            } catch (e: Exception) {
                                android.widget.FrameLayout(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    )
                    PowerBottomNavigation(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it }
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
                when (selectedTabIndex) {
                    0 -> LibraryFolderScreen(viewModel)
                    1 -> EqualizerScreen(viewModel)
                    2 -> OnlineLibraryScreen(viewModel) // Search
                    3 -> SettingsScreen(viewModel)
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
                val list = if (song.isOnline) onlineSongs else localSongs
                val currentIndex = list.indexOfFirst { it.id == song.id }
                val context = androidx.compose.ui.platform.LocalContext.current
                
                SleekPlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    progress = progress,
                    duration = duration,
                    isShuffleEnabled = isShuffleEnabled,
                    isRepeatEnabled = isRepeatEnabled,
                    isFavorited = favoriteSongs.contains(song.id),
                    sleepTimerMinutes = sleepTimerMinutes,
                    currentIndex = currentIndex,
                    totalSongs = list.size,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { 
                        try {
                            com.startapp.sdk.adsbase.StartAppAd.showAd(context)
                        } catch (e: Exception) {}
                        viewModel.nextSong() 
                    },
                    onPrevious = { 
                        try {
                            com.startapp.sdk.adsbase.StartAppAd.showAd(context)
                        } catch (e: Exception) {}
                        viewModel.previousSong() 
                    },
                    onShuffleToggle = { viewModel.toggleShuffle() },
                    onRepeatToggle = { viewModel.toggleRepeat() },
                    onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                    onSleepTimerSet = { viewModel.setSleepTimer(it) },
                    onSeek = { position -> viewModel.seekTo(position.toLong()) },
                    onCollapse = { showFullPlayer = false }
                )
            }
        }
    }
}

@Composable
fun PowerBottomNavigation(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(com.example.ui.theme.PowerSurface)
                .padding(horizontal = 32.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icons = listOf(
                Icons.Default.GridView,
                Icons.Default.Equalizer,
                Icons.Default.Search,
                Icons.Default.Menu
            )
            
            icons.forEachIndexed { index, icon ->
                val isSelected = selectedTabIndex == index
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) com.example.ui.theme.PowerAccent else com.example.ui.theme.PowerTextSecondary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
fun AudioVisualizer(isPlaying: Boolean) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "visualizer")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 16
        for (i in 0 until bars) {
            val randomDuration = remember(i) { kotlin.random.Random.nextInt(300, 900) }
            val minHeight = remember(i) { kotlin.random.Random.nextDouble(0.1, 0.3).toFloat() }
            val maxHeight = remember(i) { kotlin.random.Random.nextDouble(0.5, 1.0).toFloat() }
            
            val heightMultiplier by infiniteTransition.animateFloat(
                initialValue = minHeight,
                targetValue = if (isPlaying) maxHeight else minHeight,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(randomDuration, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "barHeight$i"
            )
            
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(heightMultiplier)
                    .clip(RoundedCornerShape(3.dp))
                    .background(com.example.ui.theme.PowerAccent)
            )
        }
    }
}
@Composable
fun SettingsScreen(viewModel: MusicViewModel) {
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val availableThemes = listOf(
        com.example.ui.theme.ThemeBlue,
        com.example.ui.theme.ThemePurple,
        com.example.ui.theme.ThemeGreen,
        com.example.ui.theme.ThemeRed
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = com.example.ui.theme.PowerAccent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Settings", color = com.example.ui.theme.PowerText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("App Theme", color = com.example.ui.theme.PowerTextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableThemes) { theme ->
                val isSelected = theme.name == currentTheme.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) com.example.ui.theme.PowerAccent.copy(alpha = 0.2f) else com.example.ui.theme.PowerSurface)
                        .clickable { viewModel.setAppTheme(theme) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = theme.name,
                            color = com.example.ui.theme.PowerText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = com.example.ui.theme.PowerAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryFolderScreen(viewModel: MusicViewModel) {
    val songs by viewModel.localSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val customPlaylists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    
    // Group songs by actual filesystem folder
    val systemFolders = remember(songs) { 
        songs.groupBy { song ->
            val path = song.data
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash != -1) {
                val folderPath = path.substring(0, lastSlash)
                val secondLastSlash = folderPath.lastIndexOf('/')
                if (secondLastSlash != -1) {
                    folderPath.substring(secondLastSlash + 1)
                } else folderPath
            } else "Unknown"
        } 
    }
    
    var currentView by remember { mutableStateOf<String?>(null) }
    var currentViewType by remember { mutableStateOf<String?>(null) } // "ALL", "FAVORITE", "SYSTEM_FOLDER", "CUSTOM_FOLDER"
    
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Folder", color = com.example.ui.theme.PowerText) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Folder Name", color = com.example.ui.theme.PowerTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.example.ui.theme.PowerAccent,
                        focusedTextColor = com.example.ui.theme.PowerText,
                        unfocusedTextColor = com.example.ui.theme.PowerText
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistName.isNotBlank()) {
                        viewModel.createPlaylist(playlistName)
                    }
                    showCreatePlaylistDialog = false
                }) {
                    Text("Create", color = com.example.ui.theme.PowerAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = com.example.ui.theme.PowerTextSecondary)
                }
            },
            containerColor = com.example.ui.theme.PowerSurface
        )
    }

    var songToAdd by remember { mutableStateOf<Song?>(null) }

    if (songToAdd != null) {
        AlertDialog(
            onDismissRequest = { songToAdd = null },
            title = { Text("Add to Folder", color = com.example.ui.theme.PowerText) },
            text = {
                if (customPlaylists.isEmpty()) {
                    Text("No custom folders found. Create one first.", color = com.example.ui.theme.PowerTextSecondary)
                } else {
                    LazyColumn {
                        items(customPlaylists.keys.toList()) { playlistName ->
                            Text(
                                text = playlistName,
                                color = com.example.ui.theme.PowerText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlistName, songToAdd!!.id)
                                        songToAdd = null
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { songToAdd = null }) {
                    Text("Cancel", color = com.example.ui.theme.PowerAccent)
                }
            },
            containerColor = com.example.ui.theme.PowerSurface
        )
    }

    if (currentView != null) {
        // Show songs inside the selected view
        val folderSongs = when (currentViewType) {
            "ALL" -> songs
            "FAVORITE" -> songs.filter { favoriteSongs.contains(it.id) }
            "SYSTEM_FOLDER" -> systemFolders[currentView] ?: emptyList()
            "CUSTOM_FOLDER" -> songs.filter { customPlaylists[currentView]?.contains(it.id) == true }
            else -> emptyList()
        }
        
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentView = null; currentViewType = null }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = com.example.ui.theme.PowerAccent)
                Spacer(modifier = Modifier.width(16.dp))
                Text(currentView!!, color = com.example.ui.theme.PowerText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            if (folderSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs found.", color = com.example.ui.theme.PowerTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folderSongs) { song ->
                        SongItem(
                            song = song, 
                            onClick = { viewModel.playSong(song) },
                            onAddToPlaylistClick = { songToAdd = song }
                        )
                    }
                }
            }
        }
    } else {
        // Show folders list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Songs
            item {
                FolderItem(name = "All Songs", count = songs.size, icon = Icons.Default.MusicNote) {
                    currentView = "All Songs"
                    currentViewType = "ALL"
                }
            }
            // Favorites
            item {
                val favCount = songs.count { favoriteSongs.contains(it.id) }
                FolderItem(name = "Favorites", count = favCount, icon = Icons.Default.Favorite) {
                    currentView = "Favorites"
                    currentViewType = "FAVORITE"
                }
            }
            
            // Create Custom Folder
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.example.ui.theme.PowerSurface)
                        .clickable { showCreatePlaylistDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Folder",
                        tint = com.example.ui.theme.PowerAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Create New Folder",
                        color = com.example.ui.theme.PowerText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Custom Folders (Playlists)
            if (customPlaylists.isNotEmpty()) {
                item {
                    Text("My Folders", color = com.example.ui.theme.PowerAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(customPlaylists.keys.toList()) { playlistName ->
                    val count = customPlaylists[playlistName]?.size ?: 0
                    FolderItem(name = playlistName, count = count, icon = Icons.Default.FolderSpecial) {
                        currentView = playlistName
                        currentViewType = "CUSTOM_FOLDER"
                    }
                }
            }
            
            // System Folders
            if (systemFolders.isNotEmpty()) {
                item {
                    Text("Device Folders", color = com.example.ui.theme.PowerAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(systemFolders.keys.toList()) { folderName ->
                    val count = systemFolders[folderName]?.size ?: 0
                    FolderItem(name = folderName, count = count, icon = Icons.Default.Folder) {
                        currentView = folderName
                        currentViewType = "SYSTEM_FOLDER"
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(name: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(com.example.ui.theme.PowerSurface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = "Folder",
            tint = com.example.ui.theme.PowerAccent,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                color = com.example.ui.theme.PowerText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count songs",
                color = com.example.ui.theme.PowerTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun OnlineLibraryScreen(viewModel: MusicViewModel) {
    val songs by viewModel.onlineSongs.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("lofi") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search music online...", color = com.example.ui.theme.PowerTextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = com.example.ui.theme.PowerTextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchOnlineSongs(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Submit Search", tint = com.example.ui.theme.PowerAccent)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { 
                    viewModel.searchOnlineSongs(searchQuery)
                    // Optionally hide keyboard here
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = com.example.ui.theme.PowerAccent,
                unfocusedBorderColor = com.example.ui.theme.PowerSurface,
                focusedTextColor = com.example.ui.theme.PowerText,
                unfocusedTextColor = com.example.ui.theme.PowerText,
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        Text(
            text = "Results",
            color = com.example.ui.theme.PowerText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = com.example.ui.theme.PowerAccent,
                modifier = Modifier.padding(16.dp)
            )
        }
        
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
fun SongItem(
    song: Song, 
    onClick: () -> Unit, 
    isOnline: Boolean = false,
    onAddToPlaylistClick: (() -> Unit)? = null
) {
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
        } else if (onAddToPlaylistClick != null) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = com.example.ui.theme.PowerTextSecondary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(com.example.ui.theme.PowerSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Folder", color = com.example.ui.theme.PowerText) },
                        onClick = { 
                            expanded = false
                            onAddToPlaylistClick() 
                        }
                    )
                }
            }
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
    sleepTimerMinutes: Int?,
    currentIndex: Int,
    totalSongs: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSleepTimerSet: (Int?) -> Unit,
    onSeek: (Float) -> Unit,
    onCollapse: () -> Unit
) {
    var sliderValue by remember(progress) { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "waveform")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF16152C), Color(0xFF0C0910))))
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Top
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
                    .background(Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = { /* Settings */ },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // Album Art Space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Album Art Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF2E2B5F), Color(0xFF1E1A3F))))
            ) {
                if (showVisualizer) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AudioVisualizer(isPlaying = isPlaying)
                    }
                } else if (song.albumArtUri != null) {
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
                        .background(Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        ))
                )

                // Overlay Content (Buttons and Text)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Like / Dislike / More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(
                                onClick = onFavoriteToggle,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (isFavorited) com.example.ui.theme.PowerAccent else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { /* Dislike action */ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Dislike",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).rotate(180f)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { /* More options */ },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons Row (Visualizer, Timer, Repeat, Shuffle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = { showVisualizer = !showVisualizer },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (showVisualizer) com.example.ui.theme.PowerAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF))
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Visualizer", tint = if (showVisualizer) com.example.ui.theme.PowerAccent else Color.White)
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = { showSleepTimerMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (sleepTimerMinutes != null) com.example.ui.theme.PowerAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF))
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = if (sleepTimerMinutes != null) com.example.ui.theme.PowerAccent else Color.White)
                }
                DropdownMenu(
                    expanded = showSleepTimerMenu,
                    onDismissRequest = { showSleepTimerMenu = false },
                    modifier = Modifier.background(com.example.ui.theme.PowerSurface)
                ) {
                    val options = listOf(null to "Off", 5 to "5 Minutes", 15 to "15 Minutes", 30 to "30 Minutes", 60 to "1 Hour", -1 to "Custom...")
                    options.forEach { (minutes, label) ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = if (sleepTimerMinutes != null && minutes == -1 && !listOf(5, 15, 30, 60).contains(sleepTimerMinutes)) "$sleepTimerMinutes Minutes" else label,
                                    color = if (sleepTimerMinutes == minutes || (minutes == -1 && sleepTimerMinutes != null && !listOf(5, 15, 30, 60).contains(sleepTimerMinutes))) com.example.ui.theme.PowerAccent else com.example.ui.theme.PowerText 
                                ) 
                            },
                            onClick = {
                                if (minutes == -1) showCustomTimerDialog = true else onSleepTimerSet(minutes)
                                showSleepTimerMenu = false
                            }
                        )
                    }
                }
                
                if (showCustomTimerDialog) {
                    var customMinutes by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showCustomTimerDialog = false },
                        title = { Text("Custom Timer", color = com.example.ui.theme.PowerText) },
                        text = {
                            OutlinedTextField(
                                value = customMinutes,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) customMinutes = it },
                                label = { Text("Minutes", color = com.example.ui.theme.PowerTextSecondary) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = com.example.ui.theme.PowerAccent,
                                    focusedTextColor = com.example.ui.theme.PowerText,
                                    unfocusedTextColor = com.example.ui.theme.PowerText
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val mins = customMinutes.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    onSleepTimerSet(mins)
                                }
                                showCustomTimerDialog = false
                            }) {
                                Text("Start", color = com.example.ui.theme.PowerAccent)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomTimerDialog = false }) {
                                Text("Cancel", color = com.example.ui.theme.PowerTextSecondary)
                            }
                        },
                        containerColor = com.example.ui.theme.PowerSurface
                    )
                }
            }

            IconButton(
                onClick = onRepeatToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isRepeatEnabled) com.example.ui.theme.PowerAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF))
            ) {
                Icon(if (isRepeatEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat, contentDescription = "Repeat", tint = if (isRepeatEnabled) com.example.ui.theme.PowerAccent else Color.White)
            }

            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isShuffleEnabled) com.example.ui.theme.PowerAccent.copy(alpha = 0.3f) else Color(0x22FFFFFF))
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffleEnabled) com.example.ui.theme.PowerAccent else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Playback Controls & Waveform
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 24.dp)
        ) {
            // Animated Waveform (Decorative) on the right side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(start = 180.dp), // Start after play button
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bars = 18
                for (i in 0 until bars) {
                    val randomDuration = remember(i) { kotlin.random.Random.nextInt(300, 900) }
                    val minHeight = remember(i) { kotlin.random.Random.nextDouble(0.2, 0.4).toFloat() }
                    val maxHeight = remember(i) { kotlin.random.Random.nextDouble(0.6, 1.0).toFloat() }
                    
                    val heightMultiplier by infiniteTransition.animateFloat(
                        initialValue = minHeight,
                        targetValue = if (isPlaying) maxHeight else minHeight,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(randomDuration, easing = androidx.compose.animation.core.LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "waveform_$i"
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(heightMultiplier)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = if (i < bars / 2) 0.8f else 0.4f))
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Fast Rewind */ },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = Color.White)
                }
                
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0x1AFFFFFF))
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                // Giant Play button
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(56.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                IconButton(
                    onClick = { /* Fast Forward */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Slider & Timeline text
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Timeline text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 24.dp), // Push text down a bit
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(progress.toLong()),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            
            // Progress Slider
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
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp).offset(y = (-16).dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Context Pill
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                
                val indexText = if (currentIndex >= 0 && totalSongs > 0) "${currentIndex + 1}/$totalSongs" else "∞"
                val sourceText = if (song.isOnline) "ONLINE SONGS" else "ALL SONGS"
                
                Text(
                    text = "$sourceText - $indexText",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

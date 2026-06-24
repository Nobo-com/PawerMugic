package com.example.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _onlineSongs = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _onlineSongs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongs: StateFlow<Set<Long>> = _favoriteSongs.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    init {
        setupPlayer()
        loadMockOnlineSongs()
        
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _progress.value = player.currentPosition.toFloat()
                }
                delay(1000)
            }
        }
    }

    private fun setupPlayer() {
        // ExoPlayer instance config
        // Wait mode is not available on all exo player versions in the same way,
        // so I will avoid wake mode and just use handleAudioBecomingNoisy which is handled by media3 directly.
        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val mediaId = mediaItem.mediaId
                    val song = localSongs.value.find { it.id.toString() == mediaId } 
                        ?: onlineSongs.value.find { it.id.toString() == mediaId }
                    _currentSong.value = song
                    _duration.value = player.duration.coerceAtLeast(0)
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                } else if (playbackState == Player.STATE_ENDED) {
                    if (!_isRepeatEnabled.value) {
                        nextSong()
                    }
                }
            }
        })
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
        player.repeatMode = if (_isRepeatEnabled.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleFavorite(songId: Long) {
        val currentFavs = _favoriteSongs.value.toMutableSet()
        if (currentFavs.contains(songId)) {
            currentFavs.remove(songId)
        } else {
            currentFavs.add(songId)
        }
        _favoriteSongs.value = currentFavs
    }

    fun nextSong() {
        val current = _currentSong.value ?: return
        val list = if (current.isOnline) _onlineSongs.value else _localSongs.value
        if (list.isEmpty()) return

        if (_isShuffleEnabled.value) {
            val nextIndex = (list.indices).random()
            playSong(list[nextIndex])
        } else {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            if (currentIndex != -1) {
                val nextIndex = (currentIndex + 1) % list.size
                playSong(list[nextIndex])
            }
        }
    }

    fun previousSong() {
        val current = _currentSong.value ?: return
        val list = if (current.isOnline) _onlineSongs.value else _localSongs.value
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1) {
            val prevIndex = (currentIndex - 1 + list.size) % list.size
            playSong(list[prevIndex])
        }
    }

    fun loadLocalMusic() {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                val songList = mutableListOf<Song>()
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                val query = getApplication<Application>().contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.TITLE + " ASC"
                )

                query?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val data = cursor.getString(dataColumn)
                        val duration = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)

                        val albumArtUri = Uri.parse("content://media/external/audio/albumart")
                        val artUri = ContentUris.withAppendedId(albumArtUri, albumId).toString()

                        songList.add(Song(id, title, artist, data, duration, artUri, false))
                    }
                }
                songList
            }
            _localSongs.value = songs
        }
    }

    private fun loadMockOnlineSongs() {
        // Mock YouTube Data
        val mockSongs = listOf(
            Song(9001, "Lo-Fi Beats to Relax", "Lofi Girl", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", 372000, "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80", true, "jfKfPfyJRdk"),
            Song(9002, "Synthwave Mix 2024", "Neon Nights", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", 420000, "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&q=80", true, "MV_3Dpw-BRY"),
            Song(9003, "Epic Orchestral Soundtrack", "Hans Studio", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", 315000, "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=500&q=80", true, "pWUMSPeVvbA")
        )
        _onlineSongs.value = mockSongs
    }

    fun playSong(song: Song) {
        val mediaItem = if (song.isOnline) {
             MediaItem.Builder()
                .setUri(song.data)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(Uri.parse(song.albumArtUri ?: ""))
                        .build()
                )
                .build()
        } else {
             MediaItem.Builder()
                .setUri(song.data)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(if (song.albumArtUri != null) Uri.parse(song.albumArtUri) else null)
                        .build()
                )
                .build()
        }

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _progress.value = positionMs.toFloat()
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}

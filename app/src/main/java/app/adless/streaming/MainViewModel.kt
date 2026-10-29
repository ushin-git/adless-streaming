package app.adless.streaming

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import app.adless.streaming.data.LocalSettingsStore
import app.adless.streaming.data.MusicLibraryStore
import app.adless.streaming.data.UserPlaylist
import app.adless.streaming.data.SourceRegistry
import app.adless.streaming.domain.MusicTrack
import app.adless.streaming.domain.QueueState
import app.adless.streaming.domain.RepeatMode
import app.adless.streaming.domain.SourceSearch
import app.adless.streaming.domain.ThemeMode
import app.adless.streaming.playback.PlaybackController
import app.adless.streaming.playback.PlaybackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class MainUiState(
    val query: String = "",
    val sources: List<SourceSearch> = emptyList(),
    val likedTracks: List<MusicTrack> = emptyList(),
    val recentTracks: List<MusicTrack> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val selectedPlaylist: UserPlaylist? = null,
    val playlistTracks: List<MusicTrack> = emptyList(),
    val enabledSourceIds: Set<String> = emptySet(),
    val queue: QueueState = QueueState(),
    val isSearching: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlayerConnected: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val error: String? = null,
)

class MainViewModel(
    private val registry: SourceRegistry,
    private val playback: PlaybackController,
    private val settings: LocalSettingsStore,
    private val library: MusicLibraryStore,
) : ViewModel() {
    private val defaultSourceIds = registry.sources.map { it.id }.toSet()
    private val _state = MutableStateFlow(
        MainUiState(
            enabledSourceIds = settings.enabledSources(defaultSourceIds),
            themeMode = settings.themeMode(),
        ),
    )
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private var searchJob: Job? = null
    private var playerQueueJob: Job? = null

    init {
        refreshLibrary()
        playback.setListener { connected ->
            controller = connected
            _state.value = _state.value.copy(isPlayerConnected = connected != null)
            connected?.let { player ->
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.value = _state.value.copy(isPlaying = isPlaying)
                    }

                    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                        _state.value = _state.value.copy(
                            queue = _state.value.queue.copy(currentIndex = player.currentMediaItemIndex),
                            currentTitle = mediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                            currentArtist = mediaItem?.mediaMetadata?.artist?.toString().orEmpty(),
                        )
                    }
                })
            }
        }
        viewModelScope.launch {
            while (true) {
                controller?.let { player ->
                    _state.value = _state.value.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.takeIf { it >= 0L } ?: 0L,
                    )
                }
                delay(500L)
            }
        }
    }

    fun isLiked(track: MusicTrack): Boolean = _state.value.likedTracks.any { it.stableKey == track.stableKey }

    fun toggleLike(track: MusicTrack) {
        viewModelScope.launch {
            runCatching {
                library.setLiked(track, !isLiked(track))
                reloadLibrary()
            }.onFailure { failure -> reportLibraryError(failure) }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            runCatching {
                library.createPlaylist(name)
                reloadLibrary()
            }.onFailure { failure -> reportLibraryError(failure) }
        }
    }

    fun openPlaylist(playlist: UserPlaylist) {
        viewModelScope.launch {
            runCatching { library.playlistTracks(playlist.id) }
                .onSuccess { tracks -> _state.value = _state.value.copy(selectedPlaylist = playlist, playlistTracks = tracks) }
                .onFailure { failure -> reportLibraryError(failure) }
        }
    }

    fun closePlaylist() {
        _state.value = _state.value.copy(selectedPlaylist = null, playlistTracks = emptyList())
    }

    fun addToPlaylist(playlist: UserPlaylist, track: MusicTrack) {
        viewModelScope.launch {
            runCatching {
                library.addToPlaylist(playlist.id, track)
                if (_state.value.selectedPlaylist?.id == playlist.id) openPlaylist(playlist)
            }.onFailure { failure -> reportLibraryError(failure) }
        }
    }

    private fun refreshLibrary() {
        viewModelScope.launch {
            runCatching { reloadLibrary() }.onFailure { failure -> reportLibraryError(failure) }
        }
    }

    private suspend fun reloadLibrary() {
        val likedTracks = library.likedTracks()
        val recentTracks = library.history()
        val playlists = library.playlists()
        _state.value = _state.value.copy(likedTracks = likedTracks, recentTracks = recentTracks, playlists = playlists)
    }

    private fun reportLibraryError(failure: Throwable) {
        if (failure is CancellationException) throw failure
        _state.value = _state.value.copy(error = failure.message ?: "Your on-device library could not be updated.")
    }

    fun search(query: String) {
        searchJob?.cancel()
        _state.value = _state.value.copy(query = query, isSearching = true, error = null)
        searchJob = viewModelScope.launch {
            val result = registry.search(query.trim(), _state.value.enabledSourceIds)
            _state.value = _state.value.copy(sources = result, isSearching = false)
        }
    }

    fun toggleSource(sourceId: String) {
        val current = _state.value.enabledSourceIds
        val updated = if (sourceId in current) current - sourceId else current + sourceId
        settings.setEnabledSources(updated)
        _state.value = _state.value.copy(enabledSourceIds = updated)
    }

    fun setThemeMode(mode: ThemeMode) {
        settings.setThemeMode(mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun play(track: MusicTrack) {
        val existingIndex = _state.value.queue.tracks.indexOfFirst { it.stableKey == track.stableKey }
        val nextQueue = if (existingIndex >= 0) {
            _state.value.queue.copy(currentIndex = existingIndex)
        } else {
            QueueState(tracks = listOf(track), currentIndex = 0)
        }
        _state.value = _state.value.copy(queue = nextQueue, error = null)
        playFromQueue(track)
    }

    fun playQueuedAt(index: Int) {
        val queue = _state.value.queue
        val track = queue.tracks.getOrNull(index) ?: return
        _state.value = _state.value.copy(queue = queue.copy(currentIndex = index), error = null)
        playFromQueue(track)
    }

    fun addNext(track: MusicTrack) {
        val queue = _state.value.queue.addNext(track)
        _state.value = _state.value.copy(queue = queue)
        updatePlayerQueue(queue, startPlayback = false)
    }

    fun removeFromQueue(index: Int) {
        val queue = _state.value.queue.remove(index)
        _state.value = _state.value.copy(queue = queue)
        updatePlayerQueue(queue, startPlayback = false)
    }

    fun moveInQueue(from: Int, to: Int) {
        val queue = _state.value.queue.move(from, to)
        _state.value = _state.value.copy(queue = queue)
        updatePlayerQueue(queue, startPlayback = false)
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(queue = _state.value.queue.copy(shuffle = !_state.value.queue.shuffle))
        controller?.shuffleModeEnabled = _state.value.queue.shuffle
    }

    fun cycleRepeat() {
        val next = when (_state.value.queue.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(queue = _state.value.queue.copy(repeatMode = next))
        controller?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun togglePlayback() {
        controller?.let { player -> if (player.isPlaying) player.pause() else player.play() }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    private fun playFromQueue(track: MusicTrack) {
        playerQueueJob?.cancel()
        playerQueueJob = viewModelScope.launch {
            try {
                val queue = _state.value.queue
                val items = queue.tracks.map { queuedTrack ->
                    val source = registry.source(queuedTrack.sourceId) ?: error("Music source is unavailable.")
                    PlaybackItem(
                        uri = Uri.parse(source.resolveStream(queuedTrack)),
                        title = queuedTrack.title,
                        artist = queuedTrack.artist,
                    )
                }
                playback.setQueue(items, queue.currentIndex, 0L, startPlayback = true)
                _state.value = _state.value.copy(currentTitle = track.title, currentArtist = track.artist)
                runCatching {
                    library.recordPlay(track)
                    reloadLibrary()
                }.onFailure { failure -> reportLibraryError(failure) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.value = _state.value.copy(error = failure.message ?: "Could not start this track.")
            }
        }
    }

    private fun updatePlayerQueue(queue: QueueState, startPlayback: Boolean) {
        playerQueueJob?.cancel()
        if (queue.tracks.isEmpty()) {
            playback.clearQueue()
            return
        }
        playerQueueJob = viewModelScope.launch {
            try {
                val items = queue.tracks.map { queuedTrack ->
                    val source = registry.source(queuedTrack.sourceId) ?: error("Music source is unavailable.")
                    PlaybackItem(Uri.parse(source.resolveStream(queuedTrack)), queuedTrack.title, queuedTrack.artist)
                }
                playback.setQueue(items, queue.currentIndex, controller?.currentPosition ?: 0L, startPlayback)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.value = _state.value.copy(error = failure.message ?: "Could not update the queue.")
            }
        }
    }

    fun localAudioPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCleared() {
        playback.release()
    }
}


package app.adless.streaming

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.adless.streaming.domain.MusicTrack
import app.adless.streaming.domain.ThemeMode
import app.adless.streaming.data.UserPlaylist

private val Ink = Color(0xFF171B18)
private val Paper = Color(0xFFF7F6F2)
private val Forest = Color(0xFF176B54)
private val Mint = Color(0xFF8CE3BD)
private val Coral = Color(0xFFB34D3D)
private val Quiet = Color(0xFF555E58)

class MainActivity : ComponentActivity() {
    private val appViewModel: MainViewModel by viewModels {
        val app = application as AdlessApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(
                    app.container.sourceRegistry,
                    app.container.playbackController,
                    app.container.settingsStore,
                    app.container.musicLibraryStore,
                ) as T
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            var showSettings by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }
            var showPlayer by remember { mutableStateOf(false) }
            var showLibrary by remember { mutableStateOf(false) }
            val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) appViewModel.search(state.query)
            }
            MaterialTheme(colorScheme = state.themeMode.colorScheme()) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BrandGlyph()
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("ADLESS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                        Text("Streaming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { showLibrary = !showLibrary; appViewModel.closePlaylist() }) {
                                    Icon(if (showLibrary) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.LibraryMusic, if (showLibrary) "Back to search" else "Open library")
                                }
                                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Sources and settings") }
                            },
                        )
                    },
                    bottomBar = {
                        if (state.queue.current != null) MiniPlayer(
                            title = state.currentTitle.ifBlank { state.queue.current?.title.orEmpty() },
                            artist = state.currentArtist.ifBlank { state.queue.current?.artist.orEmpty() },
                            isPlaying = state.isPlaying,
                            onPlayPause = appViewModel::togglePlayback,
                            onNext = appViewModel::next,
                            onExpand = { showPlayer = true },
                        )
                    },
                ) { padding ->
                    if (showLibrary) {
                        LibraryScreen(
                            state = state,
                            modifier = Modifier.padding(padding),
                            onPlay = appViewModel::play,
                            onAddNext = appViewModel::addNext,
                            onLike = appViewModel::toggleLike,
                            onCreatePlaylist = appViewModel::createPlaylist,
                            onOpenPlaylist = appViewModel::openPlaylist,
                            onClosePlaylist = appViewModel::closePlaylist,
                            onAddToPlaylist = appViewModel::addToPlaylist,
                        )
                    } else {
                        HomeScreen(
                            state = state,
                            modifier = Modifier.padding(padding),
                            onSearch = appViewModel::search,
                            onPlay = appViewModel::play,
                            onAddNext = appViewModel::addNext,
                            onLike = appViewModel::toggleLike,
                            onIsLiked = appViewModel::isLiked,
                            onAllowLocal = { requestAudioPermission.launch(appViewModel.localAudioPermission()) },
                            onAddToPlaylist = appViewModel::addToPlaylist,
                        )
                    }
                    if (showSettings) {
                        SourceSettingsDialog(
                            sources = (application as AdlessApplication).container.sourceRegistry.sources,
                            enabled = state.enabledSourceIds,
                            onToggle = appViewModel::toggleSource,
                            themeMode = state.themeMode,
                            onThemeChange = appViewModel::setThemeMode,
                            onAbout = { showSettings = false; showAbout = true },
                            onDismiss = { showSettings = false },
                        )
                    }
                    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
                    if (showPlayer && state.queue.current != null) {
                        ModalBottomSheet(onDismissRequest = { showPlayer = false }) {
                            FullPlayer(
                                state = state,
                                onPlayPause = appViewModel::togglePlayback,
                                onNext = appViewModel::next,
                                onPrevious = appViewModel::previous,
                                onSeek = appViewModel::seekTo,
                                onShuffle = appViewModel::toggleShuffle,
                                onRepeat = appViewModel::cycleRepeat,
                                onMove = appViewModel::moveInQueue,
                                onRemove = appViewModel::removeFromQueue,
                                onPlayTrack = appViewModel::playQueuedAt,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandGlyph() {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Forest),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(25.dp)) {
            val barWidth = size.width * 0.14f
            val gap = size.width * 0.07f
            val x = floatArrayOf(size.width * 0.08f, size.width * 0.29f, size.width * 0.71f, size.width * 0.92f)
            val top = floatArrayOf(0.62f, 0.34f, 0.18f, 0.54f)
            x.forEachIndexed { index, left ->
                drawLine(Mint, androidx.compose.ui.geometry.Offset(left, size.height * top[index]), androidx.compose.ui.geometry.Offset(left, size.height * 0.9f), barWidth, cap = StrokeCap.Round)
            }
            val play = Path().apply {
                moveTo(size.width * 0.43f, size.height * 0.4f)
                lineTo(size.width * 0.64f, size.height * 0.55f)
                lineTo(size.width * 0.43f, size.height * 0.7f)
                close()
            }
            drawPath(play, Color.White)
        }
    }
}

@Composable
private fun HomeScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit,
    onPlay: (MusicTrack) -> Unit,
    onAddNext: (MusicTrack) -> Unit,
    onLike: (MusicTrack) -> Unit,
    onIsLiked: (MusicTrack) -> Boolean,
    onAllowLocal: () -> Unit,
    onAddToPlaylist: (UserPlaylist, MusicTrack) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Music, on your terms.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text("No ads. No account. Your sources, your library.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("Search songs, artists, albums") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    TextButton(onClick = { onSearch(query) }) { Text("Search") }
                },
            )
        }
        if (state.enabledSourceIds.contains("local") && state.sources.none { it.sourceId == "local" } && query.isBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Forest)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Bring in music from this device", fontWeight = FontWeight.SemiBold)
                            Text("Audio stays on your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onAllowLocal) { Text("Allow") }
                    }
                }
            }
        }
        if (state.error != null) {
            item { ErrorCard(state.error) { onSearch(query) } }
        }
        if (state.isSearching) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) } }
        }
        state.sources.forEach { sourceResult ->
            item(key = "header-${sourceResult.sourceId}") {
                Text(sourceResult.sourceId.displayLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (sourceResult.error != null) item(key = "error-${sourceResult.sourceId}") { ErrorCard(sourceResult.error) { onSearch(query) } }
            items(sourceResult.tracks, key = MusicTrack::stableKey) { track ->
                TrackRow(
                    track = track,
                    liked = onIsLiked(track),
                    playlists = state.playlists,
                    onClick = { onPlay(track) },
                    onAdd = { onAddNext(track) },
                    onLike = { onLike(track) },
                    onAddToPlaylist = { playlist -> onAddToPlaylist(playlist, track) },
                )
            }
            if (sourceResult.tracks.isEmpty() && sourceResult.error == null) {
                item(key = "empty-${sourceResult.sourceId}") {
                    Text("No matches from this source.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!state.isSearching && state.sources.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("A quieter kind of listening", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Search public catalogs or choose your device library. Sources can be turned on or off in settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onPlay: (MusicTrack) -> Unit,
    onAddNext: (MusicTrack) -> Unit,
    onLike: (MusicTrack) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onOpenPlaylist: (UserPlaylist) -> Unit,
    onClosePlaylist: () -> Unit,
    onAddToPlaylist: (UserPlaylist, MusicTrack) -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(state.selectedPlaylist?.name ?: "Your library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Likes, playlists, and listening history stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.selectedPlaylist != null) {
            item { TextButton(onClick = onClosePlaylist) { Text("Back to library") } }
            items(state.playlistTracks, key = { "playlist-${it.stableKey}" }) { track ->
                TrackRow(
                    track = track,
                    liked = state.likedTracks.any { it.stableKey == track.stableKey },
                    playlists = state.playlists,
                    onClick = { onPlay(track) },
                    onAdd = { onAddNext(track) },
                    onLike = { onLike(track) },
                    onAddToPlaylist = { playlist -> onAddToPlaylist(playlist, track) },
                )
            }
            if (state.playlistTracks.isEmpty()) item { Text("This playlist is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item { Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it.take(60) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("New playlist") },
                    )
                    IconButton(onClick = { onCreatePlaylist(playlistName); playlistName = "" }, enabled = playlistName.isNotBlank(), modifier = Modifier.semantics { contentDescription = "Create playlist" }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
            if (state.playlists.isEmpty()) item { Text("Create a playlist, then add tracks from search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.playlists, key = { "playlist-${it.id}" }) { playlist ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPlaylist(playlist) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(playlist.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("Open", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Text("Liked songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (state.likedTracks.isEmpty()) item { Text("Songs you like will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.likedTracks, key = { "liked-${it.stableKey}" }) { track ->
                TrackRow(
                    track = track, liked = true, playlists = state.playlists,
                    onClick = { onPlay(track) }, onAdd = { onAddNext(track) }, onLike = { onLike(track) },
                    onAddToPlaylist = { playlist -> onAddToPlaylist(playlist, track) },
                )
            }
            item { Text("Recently played", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (state.recentTracks.isEmpty()) item { Text("Your listening history will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(state.recentTracks, key = { index, track -> "history-${track.stableKey}-$index" }) { _, track ->
                TrackRow(
                    track = track,
                    liked = state.likedTracks.any { it.stableKey == track.stableKey },
                    playlists = state.playlists,
                    onClick = { onPlay(track) }, onAdd = { onAddNext(track) }, onLike = { onLike(track) },
                    onAddToPlaylist = { playlist -> onAddToPlaylist(playlist, track) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: MusicTrack,
    liked: Boolean,
    playlists: List<UserPlaylist>,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onLike: () -> Unit,
    onAddToPlaylist: (UserPlaylist) -> Unit,
) {
    val context = LocalContext.current
    var playlistMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            track.licenseName?.let { Text(it, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = Forest) }
            Text(
                "Open source",
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.sourcePageUrl)))
                },
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = Forest,
            )
            track.licenseUrl?.let { licenseUrl ->
                Text(
                    "License details",
                    modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(licenseUrl))) },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onLike, modifier = Modifier.semantics { contentDescription = if (liked) "Unlike ${track.title}" else "Like ${track.title}" }) {
            Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onAdd, modifier = Modifier.semantics { contentDescription = "Play next: ${track.title}" }) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(
                onClick = { playlistMenuExpanded = true },
                enabled = playlists.isNotEmpty(),
                modifier = Modifier.semantics { contentDescription = "Add ${track.title} to a playlist" },
            ) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) }
            DropdownMenu(expanded = playlistMenuExpanded, onDismissRequest = { playlistMenuExpanded = false }) {
                playlists.forEach { playlist ->
                    DropdownMenuItem(
                        text = { Text(playlist.name) },
                        onClick = { onAddToPlaylist(playlist); playlistMenuExpanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDEA)), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color(0xFF702D24))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun MiniPlayer(title: String, artist: String, isPlaying: Boolean, onPlayPause: () -> Unit, onNext: () -> Unit, onExpand: () -> Unit) {
    Surface(color = Color(0xFF191E1B), tonalElevation = 4.dp, modifier = Modifier.clickable(onClick = onExpand)) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Forest), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Mint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(artist, color = Color(0xFFB7C0B9), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.semantics { contentDescription = if (isPlaying) "Pause" else "Play" }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Mint)
            }
            IconButton(onClick = onNext, modifier = Modifier.semantics { contentDescription = "Next track" }) {
                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FullPlayer(
    state: MainUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onPlayTrack: (Int) -> Unit,
) {
    val track = state.queue.current ?: return
    Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(28.dp)).background(Forest), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, contentDescription = "Album artwork placeholder", tint = Mint, modifier = Modifier.size(88.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        Text(track.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Slider(
            value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1L).toFloat()),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onShuffle, modifier = Modifier.semantics { contentDescription = "Shuffle ${if (state.queue.shuffle) "on" else "off"}" }) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (state.queue.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrevious, modifier = Modifier.semantics { contentDescription = "Previous track" }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null)
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp).clip(CircleShape).background(Forest).semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" }) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.semantics { contentDescription = "Next track" }) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
            }
            IconButton(onClick = onRepeat, modifier = Modifier.semantics { contentDescription = "Repeat ${state.queue.repeatMode.name.lowercase()}" }) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = if (state.queue.repeatMode.name != "OFF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Queue · ${state.queue.tracks.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(state.queue.tracks.indices.toList(), key = { state.queue.tracks[it].stableKey }) { index ->
                val queued = state.queue.tracks[index]
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onPlayTrack(index) }.padding(vertical = 8.dp)) {
                        Text(queued.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (index == state.queue.currentIndex) FontWeight.Bold else FontWeight.Normal)
                        Text(queued.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0, modifier = Modifier.semantics { contentDescription = "Move ${queued.title} up" }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = index < state.queue.tracks.lastIndex, modifier = Modifier.semantics { contentDescription = "Move ${queued.title} down" }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    TextButton(onClick = { onRemove(index) }) { Text("Remove") }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

@Composable
private fun SourceSettingsDialog(
    sources: List<app.adless.streaming.domain.MusicSource>,
    enabled: Set<String>,
    onToggle: (String) -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = themeMode == mode, onClick = { onThemeChange(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Music sources", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Choose which catalogs to search. No music-service account is needed; Jamendo requires a developer client ID.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sources.forEach { source ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle(source.id) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = source.id in enabled, onCheckedChange = { onToggle(source.id) })
                        Column(Modifier.weight(1f)) {
                            Text(source.displayName, fontWeight = FontWeight.SemiBold)
                            Text(source.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Jamendo needs a developer client ID. Offline downloads are disabled for online sources unless their terms allow them.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAbout) { Text("About / Credits") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About / Credits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Adless Streaming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Author and maintainer: Tanishq")
                Text("Inspired by Echo Music. Adless Streaming is an independent clean-room implementation; no Echo Music code or assets were copied.")
                Text(
                    "Echo Music project and contributors",
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EchoMusicApp/Echo-Music")))
                    },
                    color = Forest,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun ThemeMode.colorScheme() = when (this) {
    ThemeMode.LIGHT -> lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        secondary = Coral,
        background = Paper,
        surface = Color.White,
        onSurface = Ink,
        onBackground = Ink,
        outline = Quiet,
    )
    ThemeMode.DARK -> darkColorScheme(
        primary = Mint,
        onPrimary = Color(0xFF08271C),
        secondary = Color(0xFFFF9B86),
        background = Color(0xFF111412),
        surface = Color(0xFF191E1B),
        onSurface = Color(0xFFF2F4F0),
        onBackground = Color(0xFFF2F4F0),
        outline = Color(0xFF89948D),
    )
    ThemeMode.AMOLED -> darkColorScheme(
        primary = Mint,
        onPrimary = Color(0xFF00140C),
        secondary = Color(0xFFFF9B86),
        background = Color.Black,
        surface = Color(0xFF080A09),
        onSurface = Color.White,
        onBackground = Color.White,
        outline = Color(0xFF78847C),
    )
}

private fun String.displayLabel(): String = when (this) {
    "local" -> "On this device"
    "jamendo" -> "Jamendo"
    "audius" -> "Audius"
    "internet-archive" -> "Internet Archive"
    "wikimedia-commons" -> "Wikimedia Commons"
    else -> this
}


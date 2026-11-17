package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MainTab
import com.example.model.MediaType
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.screens.AudioTabScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.ExpandedAudioPlayerScreen
import com.example.ui.screens.FoldersTabScreen
import com.example.ui.screens.MediaLibraryTopHeader
import com.example.ui.screens.PlaylistDetailSheet
import com.example.ui.screens.PlaylistsTabScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.VideosTabScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.MediaViewModel

@Composable
fun MediaPlayerApp(
    viewModel: MediaViewModel,
    widthSizeClass: WindowWidthSizeClass,
    isInPipMode: Boolean
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val requiredMediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkMediaGranted(): Boolean = requiredMediaPermissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    var hasAudioRecordPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        viewModel.setMediaPermissionGranted(checkMediaGranted())
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it } || checkMediaGranted()
        viewModel.setMediaPermissionGranted(allGranted)
    }

    val visualizerPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioRecordPerm = granted
        if (granted) {
            viewModel.onAudioVisualizerPermissionGranted()
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.openExternalUri(it) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val isExpandedLayout = widthSizeClass != WindowWidthSizeClass.Compact
    val showMiniPlayer = playbackState.currentItem != null &&
        !uiState.isFullscreenVideoOpen &&
        !uiState.isExpandedAudioOpen

    val listBottomPadding = if (showMiniPlayer) 96.dp else 20.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        if (!isInPipMode) {
            Scaffold(
                containerColor = OledBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = if (showMiniPlayer) 80.dp else 8.dp)
                    )
                },
                bottomBar = {
                    if (!isExpandedLayout) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showMiniPlayer) {
                                PersistentMiniPlayer(
                                    playbackState = playbackState,
                                    onOpenPlayer = { viewModel.openActivePlayerOverlay() },
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onForward10s = { viewModel.seekBy(10_000L) }
                                )
                            }
                            StudioNavigationBar(
                                activeTab = uiState.activeTab,
                                onSelectTab = { viewModel.selectTab(it) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedLayout) {
                        StudioNavigationRail(
                            activeTab = uiState.activeTab,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            MediaLibraryTopHeader(
                                activeTab = uiState.activeTab,
                                searchQuery = uiState.searchQuery,
                                sortField = uiState.sortField,
                                sortOrder = uiState.sortOrder,
                                isScanning = uiState.isScanning,
                                onSearchChange = { viewModel.updateSearchQuery(it) },
                                onRefresh = { viewModel.refreshLibrary() },
                                onOpenLocalFile = {
                                    openFileLauncher.launch(arrayOf("video/*", "audio/*"))
                                },
                                onSelectSortField = { viewModel.updateSort(it) },
                                onToggleSortOrder = { viewModel.toggleSortOrder() }
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                when (uiState.activeTab) {
                                    MainTab.VIDEOS -> {
                                        VideosTabScreen(
                                            videos = uiState.videos,
                                            currentPlayingId = playbackState.currentItem?.id,
                                            hasMediaPermission = uiState.hasMediaPermission,
                                            bottomPaddingDp = listBottomPadding,
                                            onRequestPermission = {
                                                mediaPermissionLauncher.launch(requiredMediaPermissions)
                                            },
                                            onPlayVideo = { item ->
                                                viewModel.playMedia(item, uiState.videos)
                                            },
                                            onAddToPlaylist = { item ->
                                                viewModel.requestAddToPlaylist(item)
                                            }
                                        )
                                    }

                                    MainTab.AUDIO -> {
                                        AudioTabScreen(
                                            audios = uiState.audios,
                                            currentPlayingId = playbackState.currentItem?.id,
                                            hasMediaPermission = uiState.hasMediaPermission,
                                            bottomPaddingDp = listBottomPadding,
                                            onRequestPermission = {
                                                mediaPermissionLauncher.launch(requiredMediaPermissions)
                                            },
                                            onPlayAudio = { item ->
                                                viewModel.playMedia(item, uiState.audios)
                                            },
                                            onAddToPlaylist = { item ->
                                                viewModel.requestAddToPlaylist(item)
                                            }
                                        )
                                    }

                                    MainTab.FOLDERS -> {
                                        FoldersTabScreen(
                                            folders = uiState.folders,
                                            selectedFolder = uiState.selectedFolderFilter,
                                            filteredItems = uiState.folderFilteredItems,
                                            currentPlayingId = playbackState.currentItem?.id,
                                            bottomPaddingDp = listBottomPadding,
                                            onSelectFolder = { viewModel.selectFolderFilter(it) },
                                            onPlayItem = { item, queue ->
                                                viewModel.playMedia(item, queue)
                                            },
                                            onAddToPlaylist = { viewModel.requestAddToPlaylist(it) }
                                        )
                                    }

                                    MainTab.PLAYLISTS -> {
                                        PlaylistsTabScreen(
                                            playlists = uiState.playlists,
                                            bottomPaddingDp = listBottomPadding,
                                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                            onOpenPlaylistDetail = { pl ->
                                                viewModel.openPlaylistDetail(pl.id)
                                            },
                                            onPlayAll = { pl -> viewModel.playPlaylist(pl) },
                                            onDeletePlaylist = { pl ->
                                                viewModel.deleteCustomPlaylist(pl.id)
                                            }
                                        )
                                    }

                                    MainTab.EQUALIZER -> {
                                        EqualizerScreen(
                                            eqState = eqState,
                                            isPlaying = playbackState.isPlaying,
                                            hasAudioVisualizerPermission = hasAudioRecordPerm,
                                            bottomPaddingDp = listBottomPadding,
                                            onRequestVisualizerPermission = {
                                                visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            },
                                            onToggleMaster = { viewModel.setEqEnabled(it) },
                                            onBandChange = { idx, db -> viewModel.setEqBandLevel(idx, db) },
                                            onSelectPreset = { viewModel.selectEqPreset(it) },
                                            onSaveCustomPreset = { viewModel.saveCustomPreset(it) },
                                            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
                                            onResetFlat = { viewModel.resetEqToFlat() },
                                            onBassBoostChange = { viewModel.setBassBoost(it) },
                                            onVirtualizerChange = { viewModel.setVirtualizer(it) },
                                            onLoudnessChange = { viewModel.setLoudness(it) }
                                        )
                                    }
                                }
                            }
                        }

                        if (isExpandedLayout && showMiniPlayer) {
                            PersistentMiniPlayer(
                                playbackState = playbackState,
                                onOpenPlayer = { viewModel.openActivePlayerOverlay() },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onForward10s = { viewModel.seekBy(10_000L) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen Video Player Overlay (also active in PiP mode)
        AnimatedVisibility(
            visible = uiState.isFullscreenVideoOpen ||
                (isInPipMode && playbackState.currentItem?.type == MediaType.VIDEO),
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }
        ) {
            VideoPlayerScreen(
                exoPlayer = viewModel.playerManager.exoPlayer,
                playbackState = playbackState,
                isInPipMode = isInPipMode,
                onBack = { viewModel.closeFullscreenVideo() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekBy = { viewModel.seekBy(it) },
                onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                onCycleResizeMode = { viewModel.cycleResizeMode() },
                onToggleLoop = { viewModel.toggleLoop() },
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
                onDisableSubtitles = { viewModel.disableSubtitles() },
                onOpenEqualizer = { viewModel.openEqualizerFromPlayer() },
                onRetry = { viewModel.retryPlayback() }
            )
        }

        // Slide-up Expanded Audio Player Overlay
        AnimatedVisibility(
            visible = uiState.isExpandedAudioOpen && !isInPipMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            ExpandedAudioPlayerScreen(
                playbackState = playbackState,
                eqState = eqState,
                onClose = { viewModel.closeExpandedAudio() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekBy = { viewModel.seekBy(it) },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleLoop = { viewModel.toggleLoop() },
                onOpenEqualizer = { viewModel.openEqualizerFromPlayer() },
                onSelectSleepTimer = { viewModel.setSleepTimer(it) }
            )
        }

        // Add to Playlist Bottom Sheet
        uiState.pendingPlaylistTargetItem?.let { targetItem ->
            AddToPlaylistSheet(
                item = targetItem,
                playlists = uiState.playlists,
                onSelectPlaylist = { pl -> viewModel.addItemToPlaylist(pl.id, targetItem) },
                onRequestCreatePlaylist = { showCreatePlaylistDialog = true },
                onDismiss = { viewModel.dismissAddToPlaylist() }
            )
        }

        // Playlist Detail Bottom Sheet
        uiState.selectedPlaylistDetailId?.let { plId ->
            val playlist = uiState.playlists.find { it.id == plId }
            if (playlist != null) {
                PlaylistDetailSheet(
                    playlist = playlist,
                    currentPlayingId = playbackState.currentItem?.id,
                    onPlayItem = { item ->
                        viewModel.openPlaylistDetail(null)
                        viewModel.playMedia(item, playlist.items)
                    },
                    onPlayAll = {
                        viewModel.openPlaylistDetail(null)
                        viewModel.playPlaylist(playlist)
                    },
                    onRemoveItem = { item ->
                        viewModel.removeFromPlaylist(playlist.id, item.id)
                    },
                    onDismiss = { viewModel.openPlaylistDetail(null) }
                )
            }
        }

        // New Playlist Dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onCreate = { name, desc -> viewModel.createPlaylist(name, desc) },
                onDismiss = { showCreatePlaylistDialog = false }
            )
        }
    }
}

@Composable
private fun StudioNavigationBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        windowInsets = WindowInsets.navigationBars
    ) {
        MainTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val (selectedIcon, unselectedIcon) = tabIcons(tab)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    indicatorColor = SurfaceHighlight,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun StudioNavigationRail(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationRail(
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        MainTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val (selectedIcon, unselectedIcon) = tabIcons(tab)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    indicatorColor = SurfaceHighlight,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
            )
        }
    }
}

private fun tabIcons(tab: MainTab): Pair<ImageVector, ImageVector> = when (tab) {
    MainTab.VIDEOS -> Icons.Rounded.VideoLibrary to Icons.Outlined.VideoLibrary
    MainTab.AUDIO -> Icons.Rounded.LibraryMusic to Icons.Outlined.LibraryMusic
    MainTab.FOLDERS -> Icons.Rounded.Folder to Icons.Outlined.Folder
    MainTab.PLAYLISTS -> Icons.Rounded.QueueMusic to Icons.Outlined.QueueMusic
    MainTab.EQUALIZER -> Icons.Rounded.Equalizer to Icons.Outlined.Equalizer
}

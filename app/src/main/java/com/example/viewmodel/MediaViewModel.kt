package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaRepository
import com.example.model.EqPreset
import com.example.model.FolderGroup
import com.example.model.MainTab
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.ResizeMode
import com.example.model.SleepTimerOption
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.model.StudioEqState
import com.example.model.TrackOption
import com.example.player.AudioEffectsManager
import com.example.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val activeTab: MainTab = MainTab.VIDEOS,
    val searchQuery: String = "",
    val sortField: SortField = SortField.DATE_ADDED,
    val sortOrder: SortOrder = SortOrder.DESC,
    val selectedFolderFilter: String? = null,
    val isScanning: Boolean = false,
    val hasMediaPermission: Boolean = false,
    val videos: List<MediaItem> = emptyList(),
    val audios: List<MediaItem> = emptyList(),
    val folders: List<FolderGroup> = emptyList(),
    val folderFilteredItems: List<MediaItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isFullscreenVideoOpen: Boolean = false,
    val isExpandedAudioOpen: Boolean = false,
    val pendingPlaylistTargetItem: MediaItem? = null,
    val selectedPlaylistDetailId: String? = null,
    val snackbarMessage: String? = null
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    val audioEffectsManager = AudioEffectsManager(application)
    val playerManager = PlayerManager(application, audioEffectsManager)

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val eqState: StateFlow<StudioEqState> = audioEffectsManager.eqState

    private val rawVideos = MutableStateFlow<List<MediaItem>>(emptyList())
    private val rawAudios = MutableStateFlow<List<MediaItem>>(emptyList())

    private val activeTab = MutableStateFlow(MainTab.VIDEOS)
    private val searchQuery = MutableStateFlow("")
    private val sortField = MutableStateFlow(SortField.DATE_ADDED)
    private val sortOrder = MutableStateFlow(SortOrder.DESC)
    private val selectedFolderFilter = MutableStateFlow<String?>(null)
    private val isScanning = MutableStateFlow(false)
    private val hasMediaPermission = MutableStateFlow(false)
    private val isFullscreenVideoOpen = MutableStateFlow(false)
    private val isExpandedAudioOpen = MutableStateFlow(false)
    private val pendingPlaylistTargetItem = MutableStateFlow<MediaItem?>(null)
    private val selectedPlaylistDetailId = MutableStateFlow<String?>(null)
    private val snackbarMessage = MutableStateFlow<String?>(null)

    private val filterControlsFlow = combine(
        activeTab,
        searchQuery,
        sortField,
        sortOrder,
        selectedFolderFilter
    ) { tab, query, field, order, folder ->
        FilterControls(tab, query, field, order, folder)
    }

    private val modalControlsFlow = combine(
        isScanning,
        hasMediaPermission,
        isFullscreenVideoOpen,
        isExpandedAudioOpen,
        pendingPlaylistTargetItem
    ) { scanning, perm, fullVid, expAud, target ->
        ModalControls(scanning, perm, fullVid, expAud, target)
    }

    private val overlayFlow = combine(
        modalControlsFlow,
        selectedPlaylistDetailId,
        snackbarMessage
    ) { modals, playlistDetailId, snackbar ->
        Triple(modals, playlistDetailId, snackbar)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        rawVideos,
        rawAudios,
        repository.playlistsFlow,
        filterControlsFlow,
        overlayFlow
    ) { vids, auds, playlists, filters, overlay ->
        val (modals, playlistDetailId, snackbar) = overlay

        val sortedVideos = applySearchAndSort(vids, filters.query, filters.field, filters.order)
        val sortedAudios = applySearchAndSort(auds, filters.query, filters.field, filters.order)

        val allMedia = vids + auds
        val folderGroups = allMedia
            .groupBy { it.folderName }
            .map { (name, items) ->
                FolderGroup(
                    folderName = name,
                    folderPath = items.firstOrNull()?.folderPath ?: name,
                    videoCount = items.count { it.type == MediaType.VIDEO },
                    audioCount = items.count { it.type == MediaType.AUDIO },
                    totalCount = items.size,
                    totalSizeBytes = items.sumOf { it.sizeBytes }
                )
            }
            .filter {
                filters.query.isBlank() ||
                    it.folderName.contains(filters.query, ignoreCase = true) ||
                    it.folderPath.contains(filters.query, ignoreCase = true)
            }
            .sortedByDescending { it.totalCount }

        val folderFiltered = if (filters.folder != null) {
            applySearchAndSort(
                allMedia.filter { it.folderName == filters.folder },
                filters.query,
                filters.field,
                filters.order
            )
        } else {
            emptyList()
        }

        LibraryUiState(
            activeTab = filters.tab,
            searchQuery = filters.query,
            sortField = filters.field,
            sortOrder = filters.order,
            selectedFolderFilter = filters.folder,
            isScanning = modals.isScanning,
            hasMediaPermission = modals.hasMediaPermission,
            videos = sortedVideos,
            audios = sortedAudios,
            folders = folderGroups,
            folderFilteredItems = folderFiltered,
            playlists = playlists,
            isFullscreenVideoOpen = modals.isFullscreenVideoOpen,
            isExpandedAudioOpen = modals.isExpandedAudioOpen,
            pendingPlaylistTargetItem = modals.pendingPlaylistTargetItem,
            selectedPlaylistDetailId = playlistDetailId,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState(
            videos = repository.demoVideos(),
            audios = repository.demoAudios()
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultPlaylistsSeeded()
            refreshLibrary()
        }
        viewModelScope.launch {
            repository.eqPresetsFlow.collect { presets ->
                audioEffectsManager.updatePresetsList(presets)
            }
        }
    }

    fun setMediaPermissionGranted(granted: Boolean) {
        val changed = hasMediaPermission.value != granted
        hasMediaPermission.value = granted
        if (changed && granted) {
            refreshLibrary()
        }
    }

    fun onAudioVisualizerPermissionGranted() {
        audioEffectsManager.refreshVisualizerPermission()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            isScanning.value = true
            rawVideos.value = repository.scanDeviceVideos()
            rawAudios.value = repository.scanDeviceAudio()
            isScanning.value = false
        }
    }

    fun selectTab(tab: MainTab) {
        activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSort(field: SortField, order: SortOrder = sortOrder.value) {
        sortField.value = field
        sortOrder.value = order
    }

    fun toggleSortOrder() {
        sortOrder.value = if (sortOrder.value == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
    }

    fun selectFolderFilter(folderName: String?) {
        selectedFolderFilter.value = folderName
    }

    fun playMedia(item: MediaItem, queue: List<MediaItem> = emptyList()) {
        val resolvedQueue = when {
            queue.isNotEmpty() -> queue
            item.type == MediaType.VIDEO -> uiState.value.videos
            else -> uiState.value.audios
        }
        playerManager.playItem(item, resolvedQueue)
        if (item.type == MediaType.VIDEO) {
            isFullscreenVideoOpen.value = true
            isExpandedAudioOpen.value = false
        } else {
            isFullscreenVideoOpen.value = false
        }
    }

    fun openExternalUri(uri: Uri, fallbackMime: String? = null) {
        viewModelScope.launch {
            val item = repository.resolveExternalUri(uri, fallbackMime)
            playerManager.playItem(item, listOf(item))
            if (item.type == MediaType.VIDEO) {
                isFullscreenVideoOpen.value = true
                isExpandedAudioOpen.value = false
            } else {
                isExpandedAudioOpen.value = true
                isFullscreenVideoOpen.value = false
            }
        }
    }

    fun openActivePlayerOverlay() {
        val current = playbackState.value.currentItem ?: return
        if (current.type == MediaType.VIDEO) {
            isFullscreenVideoOpen.value = true
            isExpandedAudioOpen.value = false
        } else {
            isExpandedAudioOpen.value = true
            isFullscreenVideoOpen.value = false
        }
    }

    fun closeFullscreenVideo() {
        isFullscreenVideoOpen.value = false
    }

    fun openExpandedAudio() {
        isExpandedAudioOpen.value = true
    }

    fun closeExpandedAudio() {
        isExpandedAudioOpen.value = false
    }

    fun openEqualizerFromPlayer() {
        isFullscreenVideoOpen.value = false
        isExpandedAudioOpen.value = false
        activeTab.value = MainTab.EQUALIZER
    }

    fun requestAddToPlaylist(item: MediaItem) {
        pendingPlaylistTargetItem.value = item
    }

    fun dismissAddToPlaylist() {
        pendingPlaylistTargetItem.value = null
    }

    fun addItemToPlaylist(playlistId: String, item: MediaItem) {
        viewModelScope.launch {
            val added = repository.addItemToPlaylist(playlistId, item)
            pendingPlaylistTargetItem.value = null
            snackbarMessage.value = if (added) {
                "Added \"${item.title}\" to playlist"
            } else {
                "\"${item.title}\" is already in this playlist"
            }
        }
    }

    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            snackbarMessage.value = "Created playlist \"$name\""
        }
    }

    fun removeFromPlaylist(playlistId: String, mediaId: String) {
        viewModelScope.launch {
            repository.removeItemFromPlaylist(playlistId, mediaId)
        }
    }

    fun deleteCustomPlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deleteCustomPlaylist(playlistId)
            if (selectedPlaylistDetailId.value == playlistId) {
                selectedPlaylistDetailId.value = null
            }
            snackbarMessage.value = "Playlist deleted"
        }
    }

    fun openPlaylistDetail(playlistId: String?) {
        selectedPlaylistDetailId.value = playlistId
    }

    fun playPlaylist(playlist: Playlist) {
        if (playlist.items.isEmpty()) {
            snackbarMessage.value = "This playlist is empty"
            return
        }
        playMedia(playlist.items.first(), playlist.items)
    }

    // Equalizer & Effects Controls
    fun setEqEnabled(enabled: Boolean) = audioEffectsManager.setMasterEnabled(enabled)
    fun setEqBandLevel(index: Int, db: Float) = audioEffectsManager.setBandLevel(index, db)
    fun selectEqPreset(preset: EqPreset) = audioEffectsManager.applyPreset(preset)
    fun setBassBoost(percent: Int) = audioEffectsManager.setBassBoost(percent)
    fun setVirtualizer(percent: Int) = audioEffectsManager.setVirtualizer(percent)
    fun setLoudness(db: Float) = audioEffectsManager.setLoudness(db)
    fun resetEqToFlat() = audioEffectsManager.resetToFlat()

    fun saveCustomPreset(name: String) {
        if (name.isBlank()) return
        val current = eqState.value
        viewModelScope.launch {
            val saved = repository.saveCustomEqPreset(
                name = name,
                bandsDb = current.bandsDb,
                bassBoostPercent = current.bassBoostPercent,
                virtualizerPercent = current.virtualizerPercent,
                loudnessDb = current.loudnessDb
            )
            audioEffectsManager.applyPreset(saved)
            snackbarMessage.value = "Saved preset \"${saved.name}\""
        }
    }

    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomEqPreset(presetId)
            snackbarMessage.value = "Custom preset removed"
        }
    }

    // Player Controls
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = playerManager.seekBy(deltaMs)
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun toggleLoop() = playerManager.toggleLoop()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun cycleResizeMode() = playerManager.cycleResizeMode()
    fun setResizeMode(mode: ResizeMode) = playerManager.setResizeMode(mode)
    fun playNext() = playerManager.playNext()
    fun playPrevious() = playerManager.playPrevious()
    fun selectAudioTrack(option: TrackOption) = playerManager.selectAudioTrack(option)
    fun selectSubtitleTrack(option: TrackOption) = playerManager.selectSubtitleTrack(option)
    fun disableSubtitles() = playerManager.disableSubtitles()
    fun setSleepTimer(option: SleepTimerOption) = playerManager.setSleepTimer(option)
    fun retryPlayback() = playerManager.retryCurrentItem()

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    private fun applySearchAndSort(
        list: List<MediaItem>,
        query: String,
        field: SortField,
        order: SortOrder
    ): List<MediaItem> {
        val filtered = if (query.isBlank()) {
            list
        } else {
            list.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                    item.artist.contains(query, ignoreCase = true) ||
                    item.album.contains(query, ignoreCase = true) ||
                    item.folderName.contains(query, ignoreCase = true) ||
                    item.formatLabel.contains(query, ignoreCase = true)
            }
        }
        val comparator: Comparator<MediaItem> = when (field) {
            SortField.DATE_ADDED -> compareBy { it.dateAddedSec }
            SortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.DURATION -> compareBy { it.durationMs }
            SortField.SIZE -> compareBy { it.sizeBytes }
        }
        return if (order == SortOrder.ASC) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(comparator.reversed())
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    private data class FilterControls(
        val tab: MainTab,
        val query: String,
        val field: SortField,
        val order: SortOrder,
        val folder: String?
    )

    private data class ModalControls(
        val isScanning: Boolean,
        val hasMediaPermission: Boolean,
        val isFullscreenVideoOpen: Boolean,
        val isExpandedAudioOpen: Boolean,
        val pendingPlaylistTargetItem: MediaItem?
    )
}

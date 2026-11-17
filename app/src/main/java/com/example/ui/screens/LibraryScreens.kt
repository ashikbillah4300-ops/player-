package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderGroup
import com.example.model.MainTab
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.Playlist
import com.example.model.SortField
import com.example.model.SortOrder
import com.example.ui.components.AudioMediaCard
import com.example.ui.components.FolderGroupCard
import com.example.ui.components.PlaylistCard
import com.example.ui.components.StudioBadge
import com.example.ui.components.VideoMediaCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.utils.formatDuration

@Composable
fun MediaLibraryTopHeader(
    activeTab: MainTab,
    searchQuery: String,
    sortField: SortField,
    sortOrder: SortOrder,
    isScanning: Boolean,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenLocalFile: () -> Unit,
    onSelectSortField: (SortField) -> Unit,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, OledBlack)
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MEDIA PLAYER",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    StudioBadge(text = "STUDIO EQ", accentColor = NeonCyan)
                }
                Text(
                    text = "Universal Engine • ${activeTab.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onOpenLocalFile,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_local_file_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileOpen,
                    contentDescription = "Open local file",
                    tint = ElectricViolet
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("refresh_library_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Scan and refresh media",
                    tint = NeonCyan
                )
            }

            if (activeTab != MainTab.EQUALIZER && activeTab != MainTab.PLAYLISTS) {
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort media",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = SurfaceElevated
                    ) {
                        SortField.entries.forEach { field ->
                            val selected = field == sortField
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = field.label,
                                        color = if (selected) NeonCyan else TextPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectSortField(field)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (sortOrder == SortOrder.ASC) {
                                            Icons.Rounded.ArrowUpward
                                        } else {
                                            Icons.Rounded.ArrowDownward
                                        },
                                        contentDescription = null,
                                        tint = AmberAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Order: ${sortOrder.label}",
                                        color = AmberAccent
                                    )
                                }
                            },
                            onClick = {
                                onToggleSortOrder()
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (activeTab != MainTab.EQUALIZER) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Search title, artist, album, folder…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.72f),
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = OutlineSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = NeonCyan,
                trackColor = OutlineSubtle
            )
        }
    }
}

@Composable
fun MediaPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = ElectricViolet.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PermMedia,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scan Device Videos & Music",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Grant media access to load local storage files alongside showcase streams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("grant_media_permission_button")
            ) {
                Text("Scan")
            }
        }
    }
}

@Composable
fun VideosTabScreen(
    videos: List<MediaItem>,
    currentPlayingId: String?,
    hasMediaPermission: Boolean,
    bottomPaddingDp: androidx.compose.ui.unit.Dp,
    onRequestPermission: () -> Unit,
    onPlayVideo: (MediaItem) -> Unit,
    onAddToPlaylist: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPaddingDp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasMediaPermission) {
            item(key = "perm_banner") {
                MediaPermissionBanner(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }
        }

        if (videos.isEmpty()) {
            item(key = "empty_videos") {
                EmptyLibraryState(
                    icon = Icons.Rounded.Movie,
                    title = "No matching videos",
                    subtitle = "Try clearing your search filter or opening a local file directly."
                )
            }
        } else {
            items(videos, key = { it.id }) { item ->
                VideoMediaCard(
                    item = item,
                    isPlayingNow = item.id == currentPlayingId,
                    onPlay = { onPlayVideo(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) }
                )
            }
        }
    }
}

@Composable
fun AudioTabScreen(
    audios: List<MediaItem>,
    currentPlayingId: String?,
    hasMediaPermission: Boolean,
    bottomPaddingDp: androidx.compose.ui.unit.Dp,
    onRequestPermission: () -> Unit,
    onPlayAudio: (MediaItem) -> Unit,
    onAddToPlaylist: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPaddingDp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasMediaPermission) {
            item(key = "perm_banner_audio") {
                MediaPermissionBanner(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }
        }

        if (audios.isEmpty()) {
            item(key = "empty_audio") {
                EmptyLibraryState(
                    icon = Icons.Rounded.MusicNote,
                    title = "No matching audio tracks",
                    subtitle = "Try clearing your search filter or scanning device storage."
                )
            }
        } else {
            items(audios, key = { it.id }) { item ->
                AudioMediaCard(
                    item = item,
                    isPlayingNow = item.id == currentPlayingId,
                    onPlay = { onPlayAudio(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) }
                )
            }
        }
    }
}

@Composable
fun FoldersTabScreen(
    folders: List<FolderGroup>,
    selectedFolder: String?,
    filteredItems: List<MediaItem>,
    currentPlayingId: String?,
    bottomPaddingDp: androidx.compose.ui.unit.Dp,
    onSelectFolder: (String?) -> Unit,
    onPlayItem: (MediaItem, List<MediaItem>) -> Unit,
    onAddToPlaylist: (MediaItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectFolder(null) },
                    label = { Text("Folder: $selectedFolder") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear folder filter",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                        selectedLabelColor = AmberAccent,
                        selectedLeadingIconColor = AmberAccent,
                        selectedTrailingIconColor = AmberAccent
                    ),
                    modifier = Modifier.testTag("folder_filter_chip")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = bottomPaddingDp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedFolder != null) {
                if (filteredItems.isEmpty()) {
                    item(key = "empty_folder_filter") {
                        EmptyLibraryState(
                            icon = Icons.Rounded.FolderOpen,
                            title = "Folder filter has no results",
                            subtitle = "Clear the search query or tap the folder chip above to view all folders."
                        )
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        if (item.type == MediaType.VIDEO) {
                            VideoMediaCard(
                                item = item,
                                isPlayingNow = item.id == currentPlayingId,
                                onPlay = { onPlayItem(item, filteredItems.filter { it.type == MediaType.VIDEO }) },
                                onAddToPlaylist = { onAddToPlaylist(item) }
                            )
                        } else {
                            AudioMediaCard(
                                item = item,
                                isPlayingNow = item.id == currentPlayingId,
                                onPlay = { onPlayItem(item, filteredItems.filter { it.type == MediaType.AUDIO }) },
                                onAddToPlaylist = { onAddToPlaylist(item) }
                            )
                        }
                    }
                }
            } else {
                if (folders.isEmpty()) {
                    item(key = "empty_folders") {
                        EmptyLibraryState(
                            icon = Icons.Rounded.FolderOpen,
                            title = "No folders found",
                            subtitle = "Scan device storage or clear your search filter."
                        )
                    }
                } else {
                    items(folders, key = { it.folderName }) { folder ->
                        FolderGroupCard(
                            folder = folder,
                            onClick = { onSelectFolder(folder.folderName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsTabScreen(
    playlists: List<Playlist>,
    bottomPaddingDp: androidx.compose.ui.unit.Dp,
    onCreatePlaylistClick: () -> Unit,
    onOpenPlaylistDetail: (Playlist) -> Unit,
    onPlayAll: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = bottomPaddingDp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "new_playlist_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${playlists.size} Collections",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Button(
                    onClick = onCreatePlaylistClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("new_playlist_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Playlist", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onOpenDetail = { onOpenPlaylistDetail(playlist) },
                onPlayAll = { onPlayAll(playlist) },
                onDelete = if (playlist.isDefault) null else ({ onDeletePlaylist(playlist) })
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailSheet(
    playlist: Playlist,
    currentPlayingId: String?,
    onPlayItem: (MediaItem) -> Unit,
    onPlayAll: () -> Unit,
    onRemoveItem: (MediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onPlayAll,
                    enabled = playlist.items.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = OledBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (playlist.items.isEmpty()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Text("No tracks yet — add from Videos or Audio tab", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(playlist.items, key = { it.id }) { item ->
                        val active = item.id == currentPlayingId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPlayItem(item) },
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (active) NeonCyan else OutlineSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        active -> Icons.Rounded.GraphicEq
                                        item.type == MediaType.VIDEO -> Icons.Rounded.Movie
                                        else -> Icons.Rounded.MusicNote
                                    },
                                    contentDescription = null,
                                    tint = if (active) NeonCyan else ElectricViolet
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (active) NeonCyan else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.formatLabel} • ${formatDuration(item.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { onRemoveItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove from playlist",
                                        tint = CrimsonError.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        color = SurfaceElevated.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

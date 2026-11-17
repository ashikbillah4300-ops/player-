package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderGroup
import com.example.model.MediaItem
import com.example.model.Playlist
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.utils.formatDuration
import com.example.ui.utils.formatFileSize

@Composable
fun VideoMediaCard(
    item: MediaItem,
    isPlayingNow: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isPlayingNow) NeonCyan else OutlineSubtle
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay)
            .testTag("video_card_${item.id}"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (isPlayingNow) 1.5.dp else 1.dp, borderColor),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Artwork Thumbnail Frame with Duration Badge
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF10253A),
                                Color(0xFF1D1B3E),
                                Color(0xFF141824)
                            )
                        )
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingNow) Icons.Rounded.GraphicEq else Icons.Rounded.PlayCircleFilled,
                    contentDescription = "Play video",
                    tint = if (isPlayingNow) NeonCyan else TextPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(34.dp)
                )

                // Duration pill bottom-right
                Surface(
                    color = OledBlack.copy(alpha = 0.84f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                ) {
                    Text(
                        text = formatDuration(item.durationMs),
                        style = MonoBadgeStyle,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Showcase chip top-left if demo
                if (item.isDemo) {
                    Surface(
                        color = ElectricViolet.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "STREAM",
                            style = MonoBadgeStyle,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlayingNow) NeonCyan else TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val resColor = when {
                        item.resolutionLabel.contains("4K", true) -> AmberAccent
                        item.resolutionLabel.contains("1080", true) -> NeonCyan
                        else -> EmeraldAccent
                    }
                    if (item.resolutionLabel.isNotBlank()) {
                        StudioBadge(text = item.resolutionLabel, accentColor = resColor)
                    }
                    StudioBadge(text = item.formatLabel, accentColor = ElectricViolet)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${formatFileSize(item.sizeBytes)} • ${item.folderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_video_playlist_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AudioMediaCard(
    item: MediaItem,
    isPlayingNow: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isPlayingNow) NeonCyan else OutlineSubtle
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("audio_card_${item.id}"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isPlayingNow) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.42f),
                                NeonPink.copy(alpha = 0.16f),
                                SurfaceHighlight
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isPlayingNow) NeonCyan else ElectricViolet.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingNow) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (isPlayingNow) NeonCyan else ElectricViolet,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlayingNow) NeonCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.artist} • ${item.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fmtColor = when (item.formatLabel) {
                        "FLAC", "WAV" -> EmeraldAccent
                        "OGG", "AAC" -> NeonPink
                        else -> NeonCyan
                    }
                    StudioBadge(text = item.formatLabel, accentColor = fmtColor)
                    Text(
                        text = formatDuration(item.durationMs),
                        style = MonoBadgeStyle,
                        color = TextSecondary
                    )
                    if (item.isDemo) {
                        Text(
                            text = "• ${item.bitrateLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_audio_playlist_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun FolderGroupCard(
    folder: FolderGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.folderName}"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberAccent.copy(alpha = 0.16f))
                    .border(1.dp, AmberAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioBadge(text = "${folder.totalCount} Total", accentColor = NeonCyan)
                    if (folder.videoCount > 0) {
                        StudioBadge(text = "${folder.videoCount} Videos", accentColor = ElectricViolet)
                    }
                    if (folder.audioCount > 0) {
                        StudioBadge(text = "${folder.audioCount} Audio", accentColor = EmeraldAccent)
                    }
                }
            }

            Text(
                text = formatFileSize(folder.totalSizeBytes),
                style = MonoBadgeStyle,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onOpenDetail: () -> Unit,
    onPlayAll: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("playlist_card_${playlist.id}"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (playlist.isDefault) ElectricViolet.copy(alpha = 0.4f) else OutlineSubtle
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.35f),
                                    NeonPink.copy(alpha = 0.25f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (playlist.isDefault) {
                            StudioBadge(text = "STUDIO", accentColor = ElectricViolet)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete playlist",
                            tint = CrimsonError.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val vidCount = playlist.items.count { it.type == com.example.model.MediaType.VIDEO }
                val audCount = playlist.items.count { it.type == com.example.model.MediaType.AUDIO }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioBadge(text = "${playlist.items.size} Items", accentColor = NeonCyan)
                    if (vidCount > 0) StudioBadge(text = "$vidCount Vid", accentColor = AmberAccent)
                    if (audCount > 0) StudioBadge(text = "$audCount Aud", accentColor = EmeraldAccent)
                }

                Button(
                    onClick = onPlayAll,
                    enabled = playlist.items.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = OledBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("play_all_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play All", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

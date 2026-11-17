package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MediaItem
import com.example.model.MediaType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "mediaId"]
)
data class PlaylistItemEntity(
    val playlistId: String,
    val mediaId: String,
    val title: String,
    val uri: String,
    val typeName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String,
    val formatLabel: String,
    val resolutionLabel: String,
    val width: Int,
    val height: Int,
    val artist: String,
    val album: String,
    val folderName: String,
    val folderPath: String,
    val bitrateLabel: String,
    val isDemo: Boolean,
    val addedAt: Long
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        uri = uri,
        type = if (typeName == MediaType.VIDEO.name) MediaType.VIDEO else MediaType.AUDIO,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        dateAddedSec = dateAddedSec,
        mimeType = mimeType,
        formatLabel = formatLabel,
        resolutionLabel = resolutionLabel,
        width = width,
        height = height,
        artist = artist,
        album = album,
        folderName = folderName,
        folderPath = folderPath,
        bitrateLabel = bitrateLabel,
        isDemo = isDemo
    )

    companion object {
        fun fromMediaItem(playlistId: String, item: MediaItem): PlaylistItemEntity =
            PlaylistItemEntity(
                playlistId = playlistId,
                mediaId = item.id,
                title = item.title,
                uri = item.uri,
                typeName = item.type.name,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                dateAddedSec = item.dateAddedSec,
                mimeType = item.mimeType,
                formatLabel = item.formatLabel,
                resolutionLabel = item.resolutionLabel,
                width = item.width,
                height = item.height,
                artist = item.artist,
                album = item.album,
                folderName = item.folderName,
                folderPath = item.folderPath,
                bitrateLabel = item.bitrateLabel,
                isDemo = item.isDemo,
                addedAt = System.currentTimeMillis()
            )
    }
}

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandsCsv: String,
    val bassBoostPercent: Int,
    val virtualizerPercent: Int,
    val loudnessDb: Float,
    val createdAt: Long
)

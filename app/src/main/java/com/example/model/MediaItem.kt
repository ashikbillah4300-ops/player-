package com.example.model

enum class MediaType {
    VIDEO,
    AUDIO
}

enum class MainTab(val label: String) {
    VIDEOS("Videos"),
    AUDIO("Audio"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    EQUALIZER("Equalizer")
}

enum class SortField(val label: String) {
    DATE_ADDED("Date Added"),
    TITLE("Name / Title"),
    DURATION("Duration"),
    SIZE("File Size")
}

enum class SortOrder(val label: String) {
    DESC("Descending"),
    ASC("Ascending")
}

data class MediaItem(
    val id: String,
    val title: String,
    val uri: String,
    val type: MediaType,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String,
    val formatLabel: String,
    val resolutionLabel: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val artist: String = "Unknown Artist",
    val album: String = "Single",
    val folderName: String = "Showcase Streams",
    val folderPath: String = "Online Showcase",
    val bitrateLabel: String = "",
    val isDemo: Boolean = false
)

data class FolderGroup(
    val folderName: String,
    val folderPath: String,
    val videoCount: Int,
    val audioCount: Int,
    val totalCount: Int,
    val totalSizeBytes: Long
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

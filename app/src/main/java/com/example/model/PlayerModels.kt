package com.example.model

enum class ResizeMode(val label: String) {
    FIT("Fit"),
    ZOOM("Crop / Zoom"),
    FILL("Stretch"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Turn Off", 0),
    MIN_15("15 min", 15),
    MIN_30("30 min", 30),
    MIN_45("45 min", 45),
    MIN_60("60 min", 60),
    MIN_90("90 min", 90)
}

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val detail: String,
    val isSelected: Boolean
)

data class PlaybackState(
    val currentItem: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioSessionId: Int = 0,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesDisabled: Boolean = true,
    val sleepTimerRemainingSec: Int = 0,
    val activeSleepTimer: SleepTimerOption = SleepTimerOption.OFF,
    val errorMessage: String? = null
)

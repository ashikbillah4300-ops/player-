package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PlaybackState
import com.example.model.ResizeMode
import com.example.model.SleepTimerOption
import com.example.model.TrackOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerManager(
    context: Context,
    private val audioEffectsManager: AudioEffectsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = runCatching {
            MediaSession.Builder(context, exoPlayer).build()
        }.getOrNull()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
                _playbackState.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = if (duration > 0) duration else it.durationMs
                    )
                }
                if (playbackState == Player.STATE_READY) {
                    val sessionId = exoPlayer.audioSessionId
                    if (sessionId > 0) {
                        audioEffectsManager.attachToSession(sessionId)
                        _playbackState.update { it.copy(audioSessionId = sessionId) }
                    }
                }
                if (playbackState == Player.STATE_ENDED && !_playbackState.value.isLooping) {
                    handleTrackEnded()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    audioEffectsManager.attachToSession(audioSessionId)
                    _playbackState.update { it.copy(audioSessionId = audioSessionId) }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _playbackState.update {
                    it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                parseTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = error.localizedMessage ?: "Unable to play this media stream"
                    )
                }
            }
        })

        startPollingLoop()
    }

    private fun startPollingLoop() {
        scope.launch {
            while (isActive) {
                val playing = exoPlayer.isPlaying
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.takeIf { it > 0L } ?: _playbackState.value.durationMs
                _playbackState.update { state ->
                    if (state.positionMs != pos || state.durationMs != dur) {
                        state.copy(positionMs = pos, durationMs = dur)
                    } else state
                }
                audioEffectsManager.stepSpectrum(playing)
                delay(if (playing) 80L else 180L)
            }
        }
    }

    fun playItem(item: MediaItem, queue: List<MediaItem> = listOf(item)) {
        val resolvedQueue = if (queue.any { it.id == item.id }) queue else listOf(item) + queue
        val idx = resolvedQueue.indexOfFirst { it.id == item.id }.coerceAtLeast(0)

        val contentType = if (item.type == MediaType.VIDEO) {
            C.AUDIO_CONTENT_TYPE_MOVIE
        } else {
            C.AUDIO_CONTENT_TYPE_MUSIC
        }
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(contentType)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )

        val media3Item = Media3Item.Builder()
            .setUri(Uri.parse(item.uri))
            .setMediaId(item.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .build()
            )
            .build()

        _playbackState.update {
            it.copy(
                currentItem = item,
                queue = resolvedQueue,
                currentIndex = idx,
                positionMs = 0L,
                durationMs = item.durationMs,
                isBuffering = true,
                errorMessage = null,
                audioTracks = emptyList(),
                subtitleTracks = emptyList()
            )
        }

        exoPlayer.setMediaItem(media3Item)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = exoPlayer.duration.takeIf { it > 0L } ?: _playbackState.value.durationMs
        val target = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(target)
        _playbackState.update { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Long) {
        seekTo(exoPlayer.currentPosition + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.0f)
        exoPlayer.setPlaybackSpeed(clamped)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
    }

    fun toggleLoop() {
        val next = !_playbackState.value.isLooping
        exoPlayer.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _playbackState.update { it.copy(isLooping = next) }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun cycleResizeMode() {
        val modes = ResizeMode.entries
        val next = modes[(modes.indexOf(_playbackState.value.resizeMode) + 1) % modes.size]
        _playbackState.update { it.copy(resizeMode = next) }
    }

    fun setResizeMode(mode: ResizeMode) {
        _playbackState.update { it.copy(resizeMode = mode) }
    }

    fun playNext() {
        val state = _playbackState.value
        if (state.queue.size <= 1) {
            seekBy(10_000L)
            return
        }
        val nextIndex = if (state.isShuffleEnabled) {
            val candidates = state.queue.indices.filter { it != state.currentIndex }
            candidates.randomOrNull() ?: 0
        } else {
            (state.currentIndex + 1) % state.queue.size
        }
        playItem(state.queue[nextIndex], state.queue)
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (exoPlayer.currentPosition > 4_000L || state.queue.size <= 1) {
            seekBy(-10_000L)
            return
        }
        val prevIndex = if (state.currentIndex - 1 >= 0) {
            state.currentIndex - 1
        } else {
            state.queue.lastIndex
        }
        playItem(state.queue[prevIndex], state.queue)
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        if (state.queue.size > 1 && state.currentItem?.type == MediaType.AUDIO) {
            playNext()
        } else {
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    private fun parseTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackOption>()
        val subtitleList = mutableListOf<TrackOption>()
        var subtitlesOff = true

        tracks.groups.forEachIndexed { groupIdx, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase() ?: "UND"
                        val ch = when (format.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1 Surround"
                            8 -> "7.1 Surround"
                            else -> if (format.channelCount > 0) "${format.channelCount}ch" else "Audio"
                        }
                        val bitrate = if (format.bitrate > 0) "${format.bitrate / 1000} kbps" else ""
                        val label = format.label ?: "Audio Track ${audioList.size + 1} ($lang)"
                        val detail = listOf(lang, ch, bitrate).filter { it.isNotBlank() }.joinToString(" • ")
                        audioList.add(
                            TrackOption(
                                groupIndex = groupIdx,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                detail = detail,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase() ?: "CC"
                        val label = format.label ?: "Subtitle ${subtitleList.size + 1} ($lang)"
                        val selected = group.isTrackSelected(i)
                        if (selected) subtitlesOff = false
                        subtitleList.add(
                            TrackOption(
                                groupIndex = groupIdx,
                                trackIndex = i,
                                label = label,
                                language = lang,
                                detail = format.sampleMimeType ?: "Text",
                                isSelected = selected
                            )
                        )
                    }
                }
            }
        }

        _playbackState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subtitleList,
                subtitlesDisabled = subtitlesOff
            )
        }
    }

    fun selectAudioTrack(option: TrackOption) {
        val group = exoPlayer.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    fun selectSubtitleTrack(option: TrackOption) {
        val group = exoPlayer.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    fun disableSubtitles() {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        if (option == SleepTimerOption.OFF) {
            _playbackState.update {
                it.copy(activeSleepTimer = SleepTimerOption.OFF, sleepTimerRemainingSec = 0)
            }
            return
        }

        val totalSeconds = option.minutes * 60
        _playbackState.update {
            it.copy(activeSleepTimer = option, sleepTimerRemainingSec = totalSeconds)
        }

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _playbackState.update { it.copy(sleepTimerRemainingSec = remaining) }
            }
            if (isActive && remaining == 0) {
                exoPlayer.pause()
                _playbackState.update {
                    it.copy(
                        activeSleepTimer = SleepTimerOption.OFF,
                        sleepTimerRemainingSec = 0
                    )
                }
            }
        }
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    fun retryCurrentItem() {
        val item = _playbackState.value.currentItem ?: return
        playItem(item, _playbackState.value.queue)
    }

    fun release() {
        sleepTimerJob?.cancel()
        audioEffectsManager.releaseEffects()
        mediaSession?.release()
        mediaSession = null
        exoPlayer.release()
        scope.cancel()
    }
}

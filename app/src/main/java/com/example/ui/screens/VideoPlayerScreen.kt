package com.example.ui.screens

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.PlaybackState
import com.example.model.ResizeMode
import com.example.model.TrackOption
import com.example.ui.components.StudioBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MonoTimerStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.utils.formatDuration
import com.example.ui.utils.formatSignedSeconds
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class DragMode { NONE, HORIZONTAL, BRIGHTNESS, VOLUME }

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    exoPlayer: ExoPlayer,
    playbackState: PlaybackState,
    isInPipMode: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleLoop: () -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onSelectSubtitleTrack: (TrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableLongStateOf(0L) }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

    // Gesture Overlay States
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var scrubTargetMs by remember { mutableLongStateOf(0L) }
    var scrubDeltaMs by remember { mutableLongStateOf(0L) }

    val currentBrightnessInit = remember(activity) {
        val attr = activity?.window?.attributes?.screenBrightness ?: -1f
        if (attr in 0.01f..1f) attr else 0.6f
    }
    var brightnessFraction by remember { mutableFloatStateOf(currentBrightnessInit) }
    var showBrightnessHud by remember { mutableStateOf(false) }

    val maxStreamVol = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volumeFraction by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxStreamVol
        )
    }
    var showVolumeHud by remember { mutableStateOf(false) }

    var doubleTapBurstText by remember { mutableStateOf<String?>(null) }

    // Auto-hide controls after 4.5s
    LaunchedEffect(controlsVisible, interactionTick, playbackState.isPlaying, isLocked) {
        if (controlsVisible && playbackState.isPlaying && !isLocked && !isInPipMode) {
            delay(4500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(doubleTapBurstText) {
        if (doubleTapBurstText != null) {
            delay(650L)
            doubleTapBurstText = null
        }
    }

    BackHandler(enabled = !isInPipMode) {
        if (isLocked) {
            controlsVisible = true
        } else {
            onBack()
        }
    }

    // Pre-Android 12 OnUserLeaveHint auto-PiP
    val shouldEnterPip by rememberUpdatedState(playbackState.isPlaying && !isInPipMode)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && activity != null) {
        DisposableEffect(activity) {
            val listener = Runnable {
                if (shouldEnterPip) {
                    runCatching {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                        activity.enterPictureInPictureMode(params)
                    }
                }
            }
            activity.addOnUserLeaveHintListener(listener)
            onDispose { activity.removeOnUserLeaveHintListener(listener) }
        }
    }

    fun triggerPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                activity.enterPictureInPictureMode(params)
            }
        }
    }

    val videoContainerModifier = when (playbackState.resizeMode) {
        ResizeMode.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
        ResizeMode.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
        else -> Modifier.fillMaxSize()
    }

    val exoResizeMode = when (playbackState.resizeMode) {
        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        ResizeMode.FILL, ResizeMode.RATIO_16_9, ResizeMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    val pipModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
        Modifier.onGloballyPositioned { coords ->
            runCatching {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                if (shouldEnterPip) {
                    builder.setSourceRectHint(coords.boundsInWindow().toAndroidRectF().toRect())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setAutoEnterEnabled(shouldEnterPip)
                }
                activity.setPictureInPictureParams(builder.build())
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .then(pipModifier)
            .pointerInput(isLocked, isInPipMode, playbackState.durationMs) {
                if (isInPipMode) return@pointerInput
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        interactionTick++
                    },
                    onDoubleTap = { offset ->
                        if (isLocked) {
                            controlsVisible = true
                            return@detectTapGestures
                        }
                        val width = containerSize.width.coerceAtLeast(1)
                        if (offset.x < width / 2f) {
                            onSeekBy(-10_000L)
                            doubleTapBurstText = "-10s"
                        } else {
                            onSeekBy(10_000L)
                            doubleTapBurstText = "+10s"
                        }
                        interactionTick++
                    }
                )
            }
            .pointerInput(isLocked, isInPipMode, playbackState.durationMs, playbackState.positionMs) {
                if (isInPipMode || isLocked) return@pointerInput
                var startPosMs = 0L
                detectDragGestures(
                    onDragStart = {
                        dragMode = DragMode.NONE
                        startPosMs = playbackState.positionMs
                        scrubTargetMs = startPosMs
                        scrubDeltaMs = 0L
                    },
                    onDragEnd = {
                        if (dragMode == DragMode.HORIZONTAL) {
                            onSeekTo(scrubTargetMs)
                        }
                        dragMode = DragMode.NONE
                        showBrightnessHud = false
                        showVolumeHud = false
                    },
                    onDragCancel = {
                        dragMode = DragMode.NONE
                        showBrightnessHud = false
                        showVolumeHud = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = containerSize.width.coerceAtLeast(1).toFloat()
                        val h = containerSize.height.coerceAtLeast(1).toFloat()

                        if (dragMode == DragMode.NONE) {
                            dragMode = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                DragMode.HORIZONTAL
                            } else if (change.position.x < w / 2f) {
                                DragMode.BRIGHTNESS
                            } else {
                                DragMode.VOLUME
                            }
                        }

                        when (dragMode) {
                            DragMode.HORIZONTAL -> {
                                val duration = playbackState.durationMs.coerceAtLeast(60_000L)
                                val deltaMs = ((dragAmount.x / w) * 90_000f).toLong()
                                scrubDeltaMs += deltaMs
                                scrubTargetMs = (startPosMs + scrubDeltaMs).coerceIn(0L, duration)
                            }
                            DragMode.BRIGHTNESS -> {
                                showBrightnessHud = true
                                val delta = -dragAmount.y / (h * 0.7f)
                                brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1.0f)
                                activity?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = brightnessFraction
                                    win.attributes = lp
                                }
                            }
                            DragMode.VOLUME -> {
                                showVolumeHud = true
                                val delta = -dragAmount.y / (h * 0.7f)
                                volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                                val targetVol = (volumeFraction * maxStreamVol).toInt().coerceIn(0, maxStreamVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            }
                            DragMode.NONE -> Unit
                        }
                    }
                )
            }
    ) {
        // Video Surface
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                        resizeMode = exoResizeMode
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.resizeMode = exoResizeMode
                },
                modifier = videoContainerModifier
            )
        }

        if (!isInPipMode) {
            // Center Buffering Spinner
            if (playbackState.isBuffering && dragMode != DragMode.HORIZONTAL) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }

            // Double-Tap Burst Feedback
            doubleTapBurstText?.let { text ->
                Surface(
                    color = OledBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            // Horizontal Scrubbing Preview HUD
            if (dragMode == DragMode.HORIZONTAL) {
                Surface(
                    color = OledBlack.copy(alpha = 0.84f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = formatSignedSeconds(scrubDeltaMs),
                            style = MaterialTheme.typography.headlineSmall,
                            color = NeonCyan
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatDuration(scrubTargetMs)} / ${formatDuration(playbackState.durationMs)}",
                            style = MonoTimerStyle,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Left Brightness Vertical HUD
            if (showBrightnessHud) {
                VerticalGestureGauge(
                    icon = Icons.Rounded.BrightnessHigh,
                    label = "${(brightnessFraction * 100).toInt().coerceIn(1, 100)}%",
                    fraction = brightnessFraction,
                    accentColor = AmberAccent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                )
            }

            // Right Volume Vertical HUD
            if (showVolumeHud) {
                VerticalGestureGauge(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "${(volumeFraction * 100).toInt().coerceIn(0, 100)}%",
                    fraction = volumeFraction,
                    accentColor = NeonCyan,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                )
            }

            // Playback Error Overlay
            playbackState.errorMessage?.let { err ->
                Surface(
                    color = SurfaceElevated.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CrimsonError),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = CrimsonError,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = OledBlack
                            )
                        ) {
                            Text("Retry Stream", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Locked Floating Unlock Button
            if (isLocked) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                ) {
                    Surface(
                        color = OledBlack.copy(alpha = 0.8f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                isLocked = false
                                controlsVisible = true
                                interactionTick++
                            }
                            .testTag("unlock_screen_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock", tint = NeonCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Full Player HUD Controls
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        OledBlack.copy(alpha = 0.78f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        OledBlack.copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .systemBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playbackState.currentItem?.title ?: "Video Player",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    playbackState.currentItem?.resolutionLabel?.takeIf { it.isNotBlank() }?.let {
                                        StudioBadge(text = it, accentColor = NeonCyan)
                                    }
                                    playbackState.currentItem?.formatLabel?.let {
                                        StudioBadge(text = it, accentColor = ElectricViolet)
                                    }
                                }
                            }

                            IconButton(
                                onClick = { showSubtitleSheet = true; interactionTick++ },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (!playbackState.subtitlesDisabled) NeonCyan else TextPrimary
                                )
                            }

                            IconButton(
                                onClick = { showAudioSheet = true; interactionTick++ },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Audiotrack,
                                    contentDescription = "Audio Tracks",
                                    tint = TextPrimary
                                )
                            }

                            IconButton(
                                onClick = onOpenEqualizer,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_open_eq_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Studio Equalizer",
                                    tint = NeonCyan
                                )
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                IconButton(
                                    onClick = { triggerPip() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("video_pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureInPictureAlt,
                                        contentDescription = "Picture-in-Picture",
                                        tint = TextPrimary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    isLocked = true
                                    controlsVisible = true
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Lock Screen",
                                    tint = TextPrimary
                                )
                            }
                        }

                        // Center Transport Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSeekBy(-10_000L); interactionTick++ },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(OledBlack.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = NeonCyan,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable { onTogglePlayPause(); interactionTick++ }
                                    .testTag("video_play_pause_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                        tint = OledBlack,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onSeekBy(10_000L); interactionTick++ },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(OledBlack.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // Bottom Scrubber & Mode Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            val totalMs = playbackState.durationMs.coerceAtLeast(1L)
                            var sliderDragging by remember { mutableStateOf(false) }
                            var sliderValue by remember { mutableFloatStateOf(0f) }
                            val currentPos = if (sliderDragging) {
                                (sliderValue * totalMs).toLong()
                            } else {
                                playbackState.positionMs
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(currentPos),
                                    style = MonoTimerStyle,
                                    color = NeonCyan
                                )
                                Text(
                                    text = formatDuration(playbackState.durationMs),
                                    style = MonoTimerStyle,
                                    color = TextSecondary
                                )
                            }

                            Slider(
                                value = if (sliderDragging) {
                                    sliderValue
                                } else {
                                    (playbackState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                                },
                                onValueChange = {
                                    sliderDragging = true
                                    sliderValue = it
                                    interactionTick++
                                },
                                onValueChangeFinished = {
                                    onSeekTo((sliderValue * totalMs).toLong())
                                    sliderDragging = false
                                    interactionTick++
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = OutlineSubtle
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Playback Speed Selector
                                Box {
                                    Surface(
                                        color = SurfaceElevated.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, OutlineSubtle),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showSpeedMenu = true; interactionTick++ }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Speed,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "${playbackState.playbackSpeed}x",
                                                style = MonoTimerStyle,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSpeedMenu,
                                        onDismissRequest = { showSpeedMenu = false },
                                        containerColor = SurfaceElevated
                                    ) {
                                        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { spd ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                                                        color = if (spd == playbackState.playbackSpeed) NeonCyan else TextPrimary
                                                    )
                                                },
                                                onClick = {
                                                    onSetSpeed(spd)
                                                    showSpeedMenu = false
                                                    interactionTick++
                                                }
                                            )
                                        }
                                    }
                                }

                                // Resize Mode Toggle
                                Surface(
                                    color = SurfaceElevated.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, OutlineSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onCycleResizeMode(); interactionTick++ }
                                        .testTag("video_resize_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = ElectricViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = playbackState.resizeMode.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                // Loop Toggle
                                IconButton(
                                    onClick = { onToggleLoop(); interactionTick++ },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RepeatOne,
                                        contentDescription = "Loop Video",
                                        tint = if (playbackState.isLooping) EmeraldAccent else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Subtitles & Captions Bottom Sheet
    if (showSubtitleSheet) {
        TrackSelectionSheet(
            title = "Subtitles & Captions",
            tracks = playbackState.subtitleTracks,
            includeOffOption = true,
            offSelected = playbackState.subtitlesDisabled,
            onSelectOff = {
                onDisableSubtitles()
                showSubtitleSheet = false
            },
            onSelectTrack = {
                onSelectSubtitleTrack(it)
                showSubtitleSheet = false
            },
            onDismiss = { showSubtitleSheet = false }
        )
    }

    // Audio Tracks Bottom Sheet
    if (showAudioSheet) {
        TrackSelectionSheet(
            title = "Audio Tracks",
            tracks = playbackState.audioTracks,
            includeOffOption = false,
            offSelected = false,
            onSelectOff = {},
            onSelectTrack = {
                onSelectAudioTrack(it)
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }
}

@Composable
private fun VerticalGestureGauge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    fraction: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OledBlack.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OutlineSubtle),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                        .background(accentColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(text = label, style = MonoTimerStyle, color = TextPrimary)
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSelectionSheet(
    title: String,
    tracks: List<TrackOption>,
    includeOffOption: Boolean,
    offSelected: Boolean,
    onSelectOff: () -> Unit,
    onSelectTrack: (TrackOption) -> Unit,
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
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (includeOffOption) {
                    item(key = "off_option") {
                        TrackRow(
                            title = "Off / None",
                            subtitle = "Disable captions",
                            selected = offSelected,
                            onClick = onSelectOff
                        )
                    }
                }
                if (tracks.isEmpty() && !includeOffOption) {
                    item(key = "default_audio") {
                        TrackRow(
                            title = "Default Stereo Audio Track",
                            subtitle = "Primary container stream",
                            selected = true,
                            onClick = onDismiss
                        )
                    }
                } else {
                    items(tracks, key = { "${it.groupIndex}_${it.trackIndex}" }) { track ->
                        TrackRow(
                            title = track.label,
                            subtitle = track.detail,
                            selected = track.isSelected && (!includeOffOption || !offSelected),
                            onClick = { onSelectTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) NeonCyan else OutlineSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = NeonCyan)
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

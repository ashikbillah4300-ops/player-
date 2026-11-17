package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlaybackState
import com.example.model.SleepTimerOption
import com.example.model.StudioEqState
import com.example.ui.components.SpectrumVisualizer28
import com.example.ui.components.StudioBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MonoTimerStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.utils.formatCountdown
import com.example.ui.utils.formatDuration

@Composable
fun ExpandedAudioPlayerScreen(
    playbackState: PlaybackState,
    eqState: StudioEqState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit
) {
    val item = playbackState.currentItem
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    // Smooth continuous rotation angle while playing
    var vinylAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playbackState.isPlaying) {
        if (!playbackState.isPlaying) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        while (playbackState.isPlaying) {
            withFrameNanos { now ->
                val dtSec = (now - lastTime) / 1_000_000_000f
                lastTime = now
                vinylAngle = (vinylAngle + dtSec * 36f) % 360f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF13192B),
                        OledBlack,
                        Color(0xFF101320)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize audio player",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    Text(
                        text = item?.folderName ?: "Studio Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_open_eq_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Open Studio Equalizer",
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Rotating Vinyl Disc with Glowing Cyan Border
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                RotatingVinylDisc(
                    angleDegrees = vinylAngle,
                    isPlaying = playbackState.isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 28-bar Live Spectrum Visualizer
            Surface(
                color = SurfaceDark.copy(alpha = 0.72f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OutlineSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                SpectrumVisualizer28(
                    levels = eqState.spectrumLevels,
                    isPlaying = playbackState.isPlaying,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Track Info & Studio Master 24-bit Badge
            Text(
                text = item?.title ?: "No Track Selected",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item?.artist ?: "Unknown Artist"} • ${item?.album ?: "Single"}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioBadge(
                    text = item?.bitrateLabel?.ifBlank { "Studio Master 24-bit" } ?: "Studio Master 24-bit",
                    accentColor = EmeraldAccent
                )
                StudioBadge(
                    text = item?.formatLabel ?: "AUDIO",
                    accentColor = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Seek Bar & Timers
            val totalMs = playbackState.durationMs.coerceAtLeast(1L)
            var dragging by remember { mutableStateOf(false) }
            var dragFraction by remember { mutableFloatStateOf(0f) }
            val shownPosMs = if (dragging) (dragFraction * totalMs).toLong() else playbackState.positionMs

            Slider(
                value = if (dragging) {
                    dragFraction
                } else {
                    (playbackState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                },
                onValueChange = {
                    dragging = true
                    dragFraction = it
                },
                onValueChangeFinished = {
                    onSeekTo((dragFraction * totalMs).toLong())
                    dragging = false
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(shownPosMs), style = MonoTimerStyle, color = NeonCyan)
                Text(formatDuration(playbackState.durationMs), style = MonoTimerStyle, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Transport Controls: Shuffle, Previous, Play/Pause, Next, Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffleEnabled) NeonCyan else TextSecondary
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("audio_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track or rewind",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = NeonCyan,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("audio_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                color = OledBlack,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = OledBlack,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("audio_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track or forward",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_loop_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RepeatOne,
                        contentDescription = "Repeat one",
                        tint = if (playbackState.isLooping) EmeraldAccent else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Action Strip: -10s, Sleep Timer, +10s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSeekBy(-10_000L) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("-10s", style = MonoTimerStyle, color = TextPrimary)
                    }
                }

                Surface(
                    color = if (playbackState.sleepTimerRemainingSec > 0) {
                        AmberAccent.copy(alpha = 0.16f)
                    } else {
                        SurfaceElevated
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (playbackState.sleepTimerRemainingSec > 0) AmberAccent else OutlineSubtle
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSleepTimerSheet = true }
                        .testTag("sleep_timer_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (playbackState.sleepTimerRemainingSec > 0) AmberAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.sleepTimerRemainingSec > 0) {
                                "Sleep ${formatCountdown(playbackState.sleepTimerRemainingSec)}"
                            } else {
                                "Sleep Timer"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (playbackState.sleepTimerRemainingSec > 0) AmberAccent else TextPrimary
                        )
                    }
                }

                Surface(
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSeekBy(10_000L) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text("+10s", style = MonoTimerStyle, color = TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            activeOption = playbackState.activeSleepTimer,
            remainingSec = playbackState.sleepTimerRemainingSec,
            onSelect = {
                onSelectSleepTimer(it)
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false }
        )
    }
}

@Composable
private fun RotatingVinylDisc(
    angleDegrees: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer neon cyan glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = if (isPlaying) 0.35f else 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        rotate(degrees = angleDegrees, pivot = center) {
            val discRadius = radius * 0.90f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF0E1320),
                        Color(0xFF1F283D),
                        Color(0xFF0D111A),
                        Color(0xFF232C44),
                        Color(0xFF0E1320)
                    ),
                    center = center
                ),
                radius = discRadius,
                center = center
            )

            // Cyan glowing outer rim
            drawCircle(
                color = NeonCyan.copy(alpha = if (isPlaying) 0.9f else 0.45f),
                radius = discRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Vinyl grooves
            listOf(0.76f, 0.64f, 0.52f).forEach { factor ->
                drawCircle(
                    color = OutlineSubtle.copy(alpha = 0.65f),
                    radius = discRadius * factor,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Asymmetric specular notch on groove to make rotation clearly visible
            drawArc(
                color = NeonCyan.copy(alpha = 0.6f),
                startAngle = -35f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = Offset(center.x - discRadius * 0.72f, center.y - discRadius * 0.72f),
                size = androidx.compose.ui.geometry.Size(discRadius * 1.44f, discRadius * 1.44f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Center label
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(ElectricViolet, NeonPink),
                    start = Offset(center.x - 40f, center.y - 40f),
                    end = Offset(center.x + 40f, center.y + 40f)
                ),
                radius = discRadius * 0.32f,
                center = center
            )

            drawCircle(
                color = OledBlack,
                radius = discRadius * 0.09f,
                center = center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    activeOption: SleepTimerOption,
    remainingSec: Int,
    onSelect: (SleepTimerOption) -> Unit,
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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sleep Timer", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            if (remainingSec > 0) {
                Text(
                    text = "Playback pauses automatically in ${formatCountdown(remainingSec)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberAccent
                )
            }
            Spacer(Modifier.height(4.dp))
            SleepTimerOption.entries.forEach { option ->
                val selected = option == activeOption
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(option) },
                    color = SurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (selected) AmberAccent else OutlineSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) AmberAccent else TextPrimary
                        )
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = AmberAccent)
                        }
                    }
                }
            }
        }
    }
}

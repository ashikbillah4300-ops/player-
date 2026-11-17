package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.EQ_BAND_LABELS
import com.example.model.EqPreset
import com.example.model.StudioEqState
import com.example.ui.components.SpectrumVisualizer28
import com.example.ui.components.StudioBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimerStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.utils.formatDb
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    eqState: StudioEqState,
    isPlaying: Boolean,
    hasAudioVisualizerPermission: Boolean,
    bottomPaddingDp: Dp,
    onRequestVisualizerPermission: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onResetFlat: () -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Float) -> Unit
) {
    var showSavePresetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = bottomPaddingDp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Master Switch & Session Indicator
        item(key = "eq_master_card") {
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (eqState.isEnabled) NeonCyan.copy(alpha = 0.55f) else OutlineSubtle
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (eqState.isEnabled) NeonCyan.copy(alpha = 0.16f) else SurfaceHighlight
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (eqState.isEnabled) NeonCyan else TextMuted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pro Studio Equalizer",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val sessionLabel = if (eqState.audioSessionId > 0) {
                                "Session #${eqState.audioSessionId}"
                            } else {
                                "Standby Engine"
                            }
                            StudioBadge(
                                text = sessionLabel,
                                accentColor = if (eqState.audioSessionId > 0) EmeraldAccent else AmberAccent
                            )
                            StudioBadge(
                                text = if (eqState.isEnabled) "DSP ACTIVE" else "BYPASSED",
                                accentColor = if (eqState.isEnabled) NeonCyan else TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onResetFlat) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Flat",
                            tint = TextSecondary
                        )
                    }

                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = onToggleMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OledBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceHighlight
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // 2. Real-Time 28-Bar Spectrum Visualizer
        item(key = "eq_spectrum_card") {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME SPECTRUM",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        if (!hasAudioVisualizerPermission) {
                            Surface(
                                color = ElectricViolet.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onRequestVisualizerPermission)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Enable Live FFT",
                                        style = MonoBadgeStyle,
                                        color = ElectricViolet
                                    )
                                }
                            }
                        } else {
                            StudioBadge(text = "28-BAND FFT", accentColor = NeonCyan)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    SpectrumVisualizer28(
                        levels = eqState.spectrumLevels,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                    )
                }
            }
        }

        // 3. Presets Carousel + Save Custom Preset
        item(key = "eq_presets_card") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Studio Presets (${eqState.presets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Button(
                        onClick = { showSavePresetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("save_eq_preset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Save Preset", style = MaterialTheme.typography.labelLarge)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(eqState.presets, key = { it.id }) { preset ->
                        val selected = preset.id == eqState.activePresetId
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPreset(preset) },
                            label = {
                                Text(
                                    text = preset.name,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            trailingIcon = if (preset.isCustom) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Delete custom preset",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteCustomPreset(preset.id) }
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = OutlineSubtle,
                                selectedBorderColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("eq_preset_${preset.id}")
                        )
                    }
                }
            }
        }

        // 4. 10-Band Frequency Console (±15 dB)
        item(key = "eq_10_band_console") {
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10-Band Parametric Console",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        StudioBadge(text = "±15 dB", accentColor = ElectricViolet)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EQ_BAND_LABELS.forEachIndexed { idx, freqLabel ->
                            val dbValue = eqState.bandsDb.getOrElse(idx) { 0f }
                            VerticalEqBandSlider(
                                frequencyLabel = freqLabel,
                                dbValue = dbValue,
                                enabled = eqState.isEnabled,
                                onValueChange = { onBandChange(idx, it) },
                                modifier = Modifier.testTag("eq_band_$idx")
                            )
                        }
                    }
                }
            }
        }

        // 5. Acoustic Enhancements: Bass Boost & 3D Surround Virtualizer (Arc Meters)
        item(key = "eq_arc_enhancements") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AcousticArcCard(
                    title = "Bass Boost",
                    subtitle = "Sub-Harmonic",
                    percent = eqState.bassBoostPercent,
                    accentColor = NeonPink,
                    enabled = eqState.isEnabled,
                    onValueChange = onBassBoostChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bass_boost_card")
                )

                AcousticArcCard(
                    title = "3D Surround",
                    subtitle = "Virtualizer",
                    percent = eqState.virtualizerPercent,
                    accentColor = NeonCyan,
                    enabled = eqState.isEnabled,
                    onValueChange = onVirtualizerChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("virtualizer_card")
                )
            }
        }

        // 6. Loudness Enhancer (Pre-Amp Boost 0 dB - +20 dB)
        item(key = "eq_loudness_card") {
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, OutlineSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = AmberAccent
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Loudness Enhancer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pre-Amp Dynamic Gain Boost",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        StudioBadge(
                            text = formatDb(eqState.loudnessDb),
                            accentColor = AmberAccent
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Slider(
                        value = eqState.loudnessDb,
                        onValueChange = onLoudnessChange,
                        valueRange = 0f..20f,
                        enabled = eqState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberAccent,
                            activeTrackColor = AmberAccent,
                            inactiveTrackColor = OutlineSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loudness_slider")
                    )
                }
            }
        }
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            onSave = { name ->
                onSaveCustomPreset(name)
                showSavePresetDialog = false
            },
            onDismiss = { showSavePresetDialog = false }
        )
    }
}

@Composable
private fun VerticalEqBandSlider(
    frequencyLabel: String,
    dbValue: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDb(dbValue).replace(" dB", ""),
            style = MonoBadgeStyle,
            color = when {
                !enabled -> TextMuted
                dbValue > 0.2f -> NeonCyan
                dbValue < -0.2f -> NeonPink
                else -> TextSecondary
            },
            maxLines = 1
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(176.dp)
                .pointerInput(enabled, dbValue) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val yFraction = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val rawDb = 15f - yFraction * 30f
                        val snapped = (rawDb * 2f).roundToInt() / 2f
                        onValueChange(snapped.coerceIn(-15f, 15f))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val trackWidth = 6.dp.toPx()
                val topY = 12.dp.toPx()
                val bottomY = size.height - 12.dp.toPx()
                val trackHeight = bottomY - topY
                val midY = topY + trackHeight / 2f

                // Track background
                drawRoundRect(
                    color = SurfaceHighlight,
                    topLeft = Offset(centerX - trackWidth / 2f, topY),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                )

                // Zero center tick
                drawLine(
                    color = OutlineSubtle,
                    start = Offset(centerX - 10.dp.toPx(), midY),
                    end = Offset(centerX + 10.dp.toPx(), midY),
                    strokeWidth = 1.5.dp.toPx()
                )

                val norm = ((15f - dbValue) / 30f).coerceIn(0f, 1f)
                val thumbY = topY + norm * trackHeight
                val activeColor = when {
                    !enabled -> TextMuted
                    dbValue >= 0f -> NeonCyan
                    else -> ElectricViolet
                }

                // Active fill from 0 dB center line to thumb
                val fillTop = minOf(midY, thumbY)
                val fillHeight = kotlin.math.abs(thumbY - midY)
                if (fillHeight > 1f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(NeonCyan, ElectricViolet)),
                        topLeft = Offset(centerX - trackWidth / 2f, fillTop),
                        size = Size(trackWidth, fillHeight),
                        cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                    )
                }

                // Glowing thumb
                drawCircle(
                    color = activeColor.copy(alpha = 0.25f),
                    radius = 12.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = activeColor,
                    radius = 7.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = frequencyLabel,
            style = MonoBadgeStyle,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun AcousticArcCard(
    title: String,
    subtitle: String,
    percent: Int,
    accentColor: Color,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlineSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    val pad = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = SurfaceHighlight,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (enabled) accentColor else TextMuted,
                        startAngle = 135f,
                        sweepAngle = (percent / 100f) * 270f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        style = MonoTimerStyle,
                        color = if (enabled) accentColor else TextMuted
                    )
                    Icon(
                        imageVector = Icons.Rounded.SurroundSound,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            Slider(
                value = percent.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = 0f..100f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = SurfaceHighlight
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Save Custom EQ Preset", style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset Name") },
                placeholder = { Text("e.g., Late Night Studio") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = OutlineSubtle,
                    focusedLabelColor = NeonCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_preset_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = OledBlack
                ),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

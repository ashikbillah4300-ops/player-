package com.example.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import com.example.model.BUILT_IN_EQ_PRESETS
import com.example.model.EqPreset
import com.example.model.StudioEqState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

class AudioEffectsManager(private val context: Context) {

    private val uiBandCenterHz = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var visualizer: Visualizer? = null

    private var currentSessionId: Int = 0
    private var latestVisualizerSpectrum: FloatArray? = null
    private var spectrumPhase: Float = 0f

    private val _eqState = MutableStateFlow(StudioEqState())
    val eqState: StateFlow<StudioEqState> = _eqState.asStateFlow()

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        releaseEffects()
        currentSessionId = audioSessionId

        var hwSupported = false
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _eqState.value.isEnabled
            }
            hwSupported = true
        }
        runCatching {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _eqState.value.isEnabled
            }
        }
        runCatching {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _eqState.value.isEnabled
            }
        }
        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = _eqState.value.isEnabled
            }
        }

        attachVisualizerIfPermitted(audioSessionId)

        _eqState.update {
            it.copy(
                audioSessionId = audioSessionId,
                isHardwareSupported = hwSupported
            )
        }
        applyAllEffectsToHardware()
    }

    fun refreshVisualizerPermission() {
        if (currentSessionId > 0 && visualizer == null) {
            attachVisualizerIfPermitted(currentSessionId)
        }
    }

    private fun attachVisualizerIfPermitted(sessionId: Int) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        runCatching {
            val captureSizes = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizes.getOrElse(1) { 1024 }.coerceIn(captureSizes[0], captureSizes[1])
            visualizer = Visualizer(sessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null && fft.size >= 64) {
                                latestVisualizerSpectrum = compute28BarsFromFft(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        }
    }

    private fun compute28BarsFromFft(fft: ByteArray): FloatArray {
        val bars = FloatArray(28)
        val n = fft.size / 2
        for (i in 0 until 28) {
            val startBin = 1 + (i * (n - 2)) / 28
            val endBin = (1 + ((i + 1) * (n - 2)) / 28).coerceAtLeast(startBin + 1).coerceAtMost(n - 1)
            var sumMag = 0f
            var count = 0
            for (b in startBin until endBin) {
                val rfk = fft[2 * b].toFloat()
                val ifk = fft[2 * b + 1].toFloat()
                val mag = hypot(rfk, ifk)
                sumMag += mag
                count++
            }
            val avg = if (count > 0) sumMag / count else 0f
            val normalized = (ln(1f + avg) / 4.8f).coerceIn(0.05f, 1.0f)
            bars[i] = normalized
        }
        return bars
    }

    fun updatePresetsList(presets: List<EqPreset>) {
        _eqState.update { it.copy(presets = presets) }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _eqState.update { it.copy(isEnabled = enabled) }
        applyAllEffectsToHardware()
    }

    fun setBandLevel(bandIndex: Int, db: Float) {
        if (bandIndex !in 0 until 10) return
        val clamped = db.coerceIn(-15f, 15f)
        _eqState.update { state ->
            val updated = state.bandsDb.toMutableList().apply { this[bandIndex] = clamped }
            state.copy(bandsDb = updated, activePresetId = "custom_unsaved")
        }
        applyEqualizerBands()
    }

    fun applyPreset(preset: EqPreset) {
        _eqState.update {
            it.copy(
                activePresetId = preset.id,
                bandsDb = preset.bandsDb.take(10).let { list ->
                    if (list.size == 10) list else List(10) { idx -> list.getOrElse(idx) { 0f } }
                },
                bassBoostPercent = preset.bassBoostPercent.coerceIn(0, 100),
                virtualizerPercent = preset.virtualizerPercent.coerceIn(0, 100),
                loudnessDb = preset.loudnessDb.coerceIn(0f, 20f)
            )
        }
        applyAllEffectsToHardware()
    }

    fun setBassBoost(percent: Int) {
        _eqState.update { it.copy(bassBoostPercent = percent.coerceIn(0, 100)) }
        applyBassBoost()
    }

    fun setVirtualizer(percent: Int) {
        _eqState.update { it.copy(virtualizerPercent = percent.coerceIn(0, 100)) }
        applyVirtualizer()
    }

    fun setLoudness(db: Float) {
        _eqState.update { it.copy(loudnessDb = db.coerceIn(0f, 20f)) }
        applyLoudness()
    }

    fun resetToFlat() {
        val flat = BUILT_IN_EQ_PRESETS.first()
        applyPreset(flat)
    }

    fun stepSpectrum(isPlaying: Boolean) {
        val state = _eqState.value
        val hwFft = latestVisualizerSpectrum
        val hasLiveFft = isPlaying && hwFft != null && hwFft.any { it > 0.14f }

        val bars = if (!isPlaying) {
            state.spectrumLevels.map { prev -> (prev * 0.78f).coerceAtLeast(0.06f) }
        } else if (hasLiveFft && hwFft != null) {
            List(28) { idx ->
                val target = shapeByEq(hwFft[idx], idx, state)
                val prev = state.spectrumLevels.getOrElse(idx) { 0.08f }
                prev * 0.35f + target * 0.65f
            }
        } else {
            spectrumPhase += 0.34f
            List(28) { idx ->
                val f = idx / 27f
                val pulseA = abs(sin(spectrumPhase * 1.1f + idx * 0.42f))
                val pulseB = abs(sin(spectrumPhase * 0.63f - idx * 0.25f))
                val bassPunch = if (idx < 6) (state.bassBoostPercent / 100f) * 0.28f else 0f
                val raw = (0.18f + 0.50f * pulseA * (0.6f + 0.4f * pulseB) + bassPunch * (1f - f))
                val target = shapeByEq(raw, idx, state)
                val prev = state.spectrumLevels.getOrElse(idx) { 0.1f }
                prev * 0.30f + target * 0.70f
            }
        }

        _eqState.update { it.copy(spectrumLevels = bars) }
    }

    private fun shapeByEq(raw: Float, barIndex: Int, state: StudioEqState): Float {
        if (!state.isEnabled) return raw.coerceIn(0.06f, 0.96f)
        val bandPos = (barIndex / 27f) * 9f
        val lo = bandPos.toInt().coerceIn(0, 9)
        val hi = (lo + 1).coerceIn(0, 9)
        val frac = bandPos - lo
        val db = state.bandsDb[lo] * (1f - frac) + state.bandsDb[hi] * frac
        val eqFactor = 1f + (db / 28f)
        val loudnessFactor = 1f + (state.loudnessDb / 60f)
        return (raw * eqFactor * loudnessFactor).coerceIn(0.06f, 1.0f)
    }

    private fun applyAllEffectsToHardware() {
        val enabled = _eqState.value.isEnabled
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { loudnessEnhancer?.enabled = enabled }
        applyEqualizerBands()
        applyBassBoost()
        applyVirtualizer()
        applyLoudness()
    }

    private fun applyEqualizerBands() {
        val eq = equalizer ?: return
        val state = _eqState.value
        runCatching {
            eq.enabled = state.isEnabled
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()

            for (hwBand in 0 until numBands) {
                val centerHz = eq.getCenterFreq(hwBand.toShort()) / 1000
                val interpolatedDb = interpolateStudioDbForHz(centerHz, state.bandsDb)
                val targetMb = (interpolatedDb * 100f).toInt().coerceIn(minMb, maxMb)
                eq.setBandLevel(hwBand.toShort(), targetMb.toShort())
            }
        }
    }

    private fun interpolateStudioDbForHz(targetHz: Int, bandsDb: List<Float>): Float {
        if (targetHz <= uiBandCenterHz.first()) return bandsDb.first()
        if (targetHz >= uiBandCenterHz.last()) return bandsDb.last()
        for (i in 0 until uiBandCenterHz.size - 1) {
            val lowHz = uiBandCenterHz[i]
            val highHz = uiBandCenterHz[i + 1]
            if (targetHz in lowHz..highHz) {
                val ratio = (targetHz - lowHz).toFloat() / (highHz - lowHz).coerceAtLeast(1)
                return bandsDb[i] * (1f - ratio) + bandsDb[i + 1] * ratio
            }
        }
        return 0f
    }

    private fun applyBassBoost() {
        val bb = bassBoost ?: return
        val state = _eqState.value
        runCatching {
            bb.enabled = state.isEnabled && state.bassBoostPercent > 0
            if (bb.strengthSupported) {
                bb.setStrength((state.bassBoostPercent * 10).toShort())
            }
        }
    }

    private fun applyVirtualizer() {
        val virt = virtualizer ?: return
        val state = _eqState.value
        runCatching {
            virt.enabled = state.isEnabled && state.virtualizerPercent > 0
            if (virt.strengthSupported) {
                virt.setStrength((state.virtualizerPercent * 10).toShort())
            }
        }
    }

    private fun applyLoudness() {
        val le = loudnessEnhancer ?: return
        val state = _eqState.value
        runCatching {
            le.enabled = state.isEnabled && state.loudnessDb > 0.1f
            le.setTargetGain((state.loudnessDb * 100f).toInt())
        }
    }

    fun releaseEffects() {
        runCatching { visualizer?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        visualizer = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentSessionId = 0
    }
}

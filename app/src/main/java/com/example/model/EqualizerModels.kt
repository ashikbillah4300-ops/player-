package com.example.model

val EQ_BAND_LABELS = listOf(
    "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
    "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"
)

data class EqPreset(
    val id: String,
    val name: String,
    val bandsDb: List<Float>, // 10 bands in -15f..+15f
    val bassBoostPercent: Int = 0, // 0..100
    val virtualizerPercent: Int = 0, // 0..100
    val loudnessDb: Float = 0f, // 0f..20f
    val isCustom: Boolean = false
)

data class StudioEqState(
    val isEnabled: Boolean = true,
    val audioSessionId: Int = 0,
    val isHardwareSupported: Boolean = true,
    val activePresetId: String = "flat",
    val bandsDb: List<Float> = List(10) { 0f },
    val bassBoostPercent: Int = 25,
    val virtualizerPercent: Int = 20,
    val loudnessDb: Float = 2f,
    val presets: List<EqPreset> = BUILT_IN_EQ_PRESETS,
    val spectrumLevels: List<Float> = List(28) { 0.08f }
)

val BUILT_IN_EQ_PRESETS: List<EqPreset> = listOf(
    EqPreset(
        id = "flat",
        name = "Flat / Normal",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        bassBoostPercent = 0,
        virtualizerPercent = 0,
        loudnessDb = 0f
    ),
    EqPreset(
        id = "rock",
        name = "Rock",
        bandsDb = listOf(5.0f, 4.0f, 2.5f, -1.0f, -2.0f, 0.5f, 3.0f, 4.5f, 5.5f, 5.0f),
        bassBoostPercent = 35,
        virtualizerPercent = 25,
        loudnessDb = 3f
    ),
    EqPreset(
        id = "pop",
        name = "Pop",
        bandsDb = listOf(-1.5f, 1.5f, 3.5f, 4.5f, 3.0f, 0f, -1.0f, 1.5f, 3.0f, 3.5f),
        bassBoostPercent = 20,
        virtualizerPercent = 25,
        loudnessDb = 2.5f
    ),
    EqPreset(
        id = "classical",
        name = "Classical",
        bandsDb = listOf(4.0f, 3.0f, 2.0f, 0.5f, -1.5f, -1.0f, 1.5f, 3.5f, 4.5f, 5.0f),
        bassBoostPercent = 10,
        virtualizerPercent = 45,
        loudnessDb = 1.5f
    ),
    EqPreset(
        id = "jazz",
        name = "Jazz",
        bandsDb = listOf(3.5f, 2.5f, 1.0f, 2.0f, -1.5f, -1.0f, 1.5f, 3.0f, 4.0f, 3.5f),
        bassBoostPercent = 20,
        virtualizerPercent = 40,
        loudnessDb = 2f
    ),
    EqPreset(
        id = "dance_edm",
        name = "Dance / EDM",
        bandsDb = listOf(6.5f, 5.5f, 3.0f, 0f, -1.5f, 1.0f, 3.5f, 5.0f, 6.0f, 4.5f),
        bassBoostPercent = 65,
        virtualizerPercent = 50,
        loudnessDb = 5f
    ),
    EqPreset(
        id = "electronic",
        name = "Electronic",
        bandsDb = listOf(5.5f, 4.5f, 2.0f, -1.5f, 0.5f, 2.5f, 1.5f, 4.0f, 5.5f, 6.5f),
        bassBoostPercent = 55,
        virtualizerPercent = 60,
        loudnessDb = 4f
    ),
    EqPreset(
        id = "heavy_metal",
        name = "Heavy Metal",
        bandsDb = listOf(4.5f, 3.5f, 1.0f, -3.5f, -4.0f, 1.0f, 4.5f, 6.0f, 6.5f, 5.0f),
        bassBoostPercent = 45,
        virtualizerPercent = 30,
        loudnessDb = 4.5f
    ),
    EqPreset(
        id = "hip_hop",
        name = "Hip Hop",
        bandsDb = listOf(7.0f, 6.0f, 3.5f, 1.0f, -1.5f, -1.0f, 1.5f, 2.5f, 3.5f, 3.0f),
        bassBoostPercent = 75,
        virtualizerPercent = 30,
        loudnessDb = 4f
    ),
    EqPreset(
        id = "vocal_booster",
        name = "Vocal Booster",
        bandsDb = listOf(-3.0f, -2.0f, -0.5f, 2.5f, 5.5f, 6.5f, 5.0f, 2.5f, 0.5f, -1.0f),
        bassBoostPercent = 5,
        virtualizerPercent = 15,
        loudnessDb = 3.5f
    ),
    EqPreset(
        id = "bass_extreme",
        name = "Bass Extreme",
        bandsDb = listOf(10.5f, 9.0f, 6.5f, 3.0f, 0f, -1.0f, 0.5f, 2.0f, 3.0f, 2.5f),
        bassBoostPercent = 95,
        virtualizerPercent = 35,
        loudnessDb = 6f
    ),
    EqPreset(
        id = "theater_3d",
        name = "3D Theater",
        bandsDb = listOf(5.0f, 4.0f, 2.0f, 0.5f, 1.5f, 2.5f, 4.0f, 5.5f, 6.0f, 5.0f),
        bassBoostPercent = 55,
        virtualizerPercent = 90,
        loudnessDb = 5f
    )
)

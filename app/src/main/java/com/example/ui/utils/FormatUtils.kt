package com.example.ui.utils

import java.util.Locale
import kotlin.math.abs

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatSignedSeconds(deltaMs: Long): String {
    val sec = (deltaMs / 1000L).toInt()
    return if (sec >= 0) "+${sec}s" else "${sec}s"
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Stream"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDb(db: Float): String {
    val rounded = kotlin.math.round(db * 10f) / 10f
    return when {
        rounded > 0.05f -> String.format(Locale.US, "+%.1f dB", rounded)
        rounded < -0.05f -> String.format(Locale.US, "-%.1f dB", abs(rounded))
        else -> "0.0 dB"
    }
}

fun formatCountdown(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}

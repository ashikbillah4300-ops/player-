package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

private val StudioOledColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OledBlack,
    primaryContainer = Color(0xFF003640),
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF281B4B),
    onSecondaryContainer = Color(0xFFDCD0FF),
    tertiary = NeonPink,
    onTertiary = TextPrimary,
    tertiaryContainer = Color(0xFF471531),
    onTertiaryContainer = Color(0xFFFFD8E8),
    background = OledBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHighlight,
    surfaceContainerHighest = Color(0xFF262F47),
    outline = OutlineSubtle,
    outlineVariant = Color(0xFF1D2436),
    error = CrimsonError,
    onError = TextPrimary
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MediaPlayerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StudioOledColorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}

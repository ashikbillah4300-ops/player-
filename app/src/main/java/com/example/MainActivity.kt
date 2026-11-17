package com.example

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.core.app.PictureInPictureModeChangedInfo
import com.example.ui.MediaPlayerApp
import com.example.ui.theme.MediaPlayerTheme
import com.example.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {

    private val mediaViewModel: MediaViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)

    private val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
        isInPipMode = info.isInPictureInPictureMode
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isInPipMode = isInPictureInPictureMode
        }
        addOnPictureInPictureModeChangedListener(pipListener)

        handleViewIntent(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MediaPlayerTheme {
                MediaPlayerApp(
                    viewModel = mediaViewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    isInPipMode = isInPipMode
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        removeOnPictureInPictureModeChangedListener(pipListener)
        super.onDestroy()
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val dataUri = intent.data ?: return
            mediaViewModel.openExternalUri(dataUri, intent.type)
        }
    }
}

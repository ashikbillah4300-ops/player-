package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MediaRepository
import com.example.model.BUILT_IN_EQ_PRESETS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Media Player", appName)
    }

    @Test
    fun `built in equalizer presets are valid 10-band profiles`() {
        assertEquals(12, BUILT_IN_EQ_PRESETS.size)
        BUILT_IN_EQ_PRESETS.forEach { preset ->
            assertEquals(10, preset.bandsDb.size)
            preset.bandsDb.forEach { db ->
                assertTrue("Band dB out of range for ${preset.name}", db in -15f..15f)
            }
        }
    }

    @Test
    fun `repository seeds default playlists and blocks duplicates`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MediaRepository(context)
        repo.ensureDefaultPlaylistsSeeded()

        val playlists = repo.playlistsFlow.first()
        assertTrue(playlists.any { it.name == "Cinema & HDR Showcase" })
        assertTrue(playlists.any { it.name == "High-Fidelity Audio Gems" })

        val customId = repo.createPlaylist("Night Drive", "Late night synth")
        val demoVideo = repo.demoVideos().first()
        assertTrue(repo.addItemToPlaylist(customId, demoVideo))
        assertFalse(repo.addItemToPlaylist(customId, demoVideo))

        val savedPreset = repo.saveCustomEqPreset(
            name = "My Studio Curve",
            bandsDb = List(10) { 2.5f },
            bassBoostPercent = 40,
            virtualizerPercent = 30,
            loudnessDb = 4f
        )
        val allPresets = repo.eqPresetsFlow.first()
        assertTrue(allPresets.any { it.id == savedPreset.id && it.isCustom })
    }
}

package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.db.CustomEqPresetEntity
import com.example.data.db.MediaDatabase
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistItemEntity
import com.example.model.BUILT_IN_EQ_PRESETS
import com.example.model.EqPreset
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val db = MediaDatabase.getInstance(context)
    private val playlistDao = db.playlistDao()
    private val eqPresetDao = db.eqPresetDao()

    val playlistsFlow: Flow<List<Playlist>> = combine(
        playlistDao.observePlaylists(),
        playlistDao.observeAllPlaylistItems()
    ) { playlists, items ->
        val itemsByPlaylist = items.groupBy { it.playlistId }
        playlists.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                isDefault = entity.isDefault,
                items = itemsByPlaylist[entity.id]?.map { it.toMediaItem() }.orEmpty(),
                createdAt = entity.createdAt
            )
        }
    }

    val eqPresetsFlow: Flow<List<EqPreset>> = eqPresetDao.observeCustomPresets().map { customEntities ->
        val customPresets = customEntities.map { entity ->
            val bands = entity.bandsCsv.split(",")
                .mapNotNull { it.trim().toFloatOrNull() }
                .let { parsed ->
                    if (parsed.size == 10) parsed else List(10) { idx -> parsed.getOrElse(idx) { 0f } }
                }
            EqPreset(
                id = entity.id,
                name = entity.name,
                bandsDb = bands,
                bassBoostPercent = entity.bassBoostPercent,
                virtualizerPercent = entity.virtualizerPercent,
                loudnessDb = entity.loudnessDb,
                isCustom = true
            )
        }
        BUILT_IN_EQ_PRESETS + customPresets
    }

    suspend fun ensureDefaultPlaylistsSeeded() = withContext(Dispatchers.IO) {
        if (playlistDao.getPlaylistCount() == 0) {
            val cinemaId = "default_cinema_hdr"
            val audioId = "default_hifi_audio"

            playlistDao.insertPlaylist(
                PlaylistEntity(
                    id = cinemaId,
                    name = "Cinema & HDR Showcase",
                    description = "Curated 4K Ultra HD & 1080p FHD reference video streams",
                    isDefault = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    id = audioId,
                    name = "High-Fidelity Audio Gems",
                    description = "Studio lossless FLAC, WAV, AAC & 320kbps master tracks",
                    isDefault = true,
                    createdAt = System.currentTimeMillis() - 1000L
                )
            )

            demoVideos().take(3).forEach { video ->
                playlistDao.insertPlaylistItem(PlaylistItemEntity.fromMediaItem(cinemaId, video))
            }
            demoAudios().forEach { audio ->
                playlistDao.insertPlaylistItem(PlaylistItemEntity.fromMediaItem(audioId, audio))
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String): String = withContext(Dispatchers.IO) {
        val id = "pl_${UUID.randomUUID()}"
        playlistDao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name.trim(),
                description = description.trim().ifEmpty { "Custom Studio Collection" },
                isDefault = false,
                createdAt = System.currentTimeMillis()
            )
        )
        id
    }

    suspend fun addItemToPlaylist(playlistId: String, item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val rowId = playlistDao.insertPlaylistItem(PlaylistItemEntity.fromMediaItem(playlistId, item))
        rowId != -1L
    }

    suspend fun removeItemFromPlaylist(playlistId: String, mediaId: String) = withContext(Dispatchers.IO) {
        playlistDao.removePlaylistItem(playlistId, mediaId)
    }

    suspend fun deleteCustomPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistItems(playlistId)
        playlistDao.deleteCustomPlaylist(playlistId)
    }

    suspend fun saveCustomEqPreset(
        name: String,
        bandsDb: List<Float>,
        bassBoostPercent: Int,
        virtualizerPercent: Int,
        loudnessDb: Float
    ): EqPreset = withContext(Dispatchers.IO) {
        val id = "custom_${UUID.randomUUID()}"
        val entity = CustomEqPresetEntity(
            id = id,
            name = name.trim(),
            bandsCsv = bandsDb.joinToString(",") { "%.2f".format(it) },
            bassBoostPercent = bassBoostPercent.coerceIn(0, 100),
            virtualizerPercent = virtualizerPercent.coerceIn(0, 100),
            loudnessDb = loudnessDb.coerceIn(0f, 20f),
            createdAt = System.currentTimeMillis()
        )
        eqPresetDao.insertCustomPreset(entity)
        EqPreset(
            id = id,
            name = entity.name,
            bandsDb = bandsDb,
            bassBoostPercent = entity.bassBoostPercent,
            virtualizerPercent = entity.virtualizerPercent,
            loudnessDb = entity.loudnessDb,
            isCustom = true
        )
    }

    suspend fun deleteCustomEqPreset(presetId: String) = withContext(Dispatchers.IO) {
        eqPresetDao.deleteCustomPreset(presetId)
    }

    suspend fun scanDeviceVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val title = (rawTitle?.takeIf { it.isNotBlank() } ?: displayName ?: "Video $id")
                        .substringBeforeLast('.')
                    val duration = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null
                    val folderName = bucket?.takeIf { it.isNotBlank() }
                        ?: filePath?.let { File(it).parentFile?.name }
                        ?: "Device Videos"
                    val folderPath = filePath?.let { File(it).parent } ?: "/storage/emulated/0/$folderName"

                    scanned.add(
                        MediaItem(
                            id = "vid_local_$id",
                            title = title,
                            uri = contentUri.toString(),
                            type = MediaType.VIDEO,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedSec = dateAdded,
                            mimeType = mime,
                            formatLabel = deriveFormatLabel(displayName, mime, "MP4"),
                            resolutionLabel = deriveResolutionLabel(width, height),
                            width = width,
                            height = height,
                            folderName = folderName,
                            folderPath = folderPath,
                            isDemo = false
                        )
                    )
                }
            }
        }

        scanned + demoVideos()
    }

    suspend fun scanDeviceAudio(): List<MediaItem> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 5000",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val title = (rawTitle?.takeIf { it.isNotBlank() } ?: displayName ?: "Track $id")
                        .substringBeforeLast('.')
                    val artist = if (artistCol >= 0) {
                        cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                            ?: "Unknown Artist"
                    } else "Unknown Artist"
                    val album = if (albumCol >= 0) {
                        cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Single"
                    } else "Single"
                    val duration = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val folderName = filePath?.let { File(it).parentFile?.name } ?: "Music"
                    val folderPath = filePath?.let { File(it).parent } ?: "/storage/emulated/0/Music"

                    scanned.add(
                        MediaItem(
                            id = "aud_local_$id",
                            title = title,
                            uri = contentUri.toString(),
                            type = MediaType.AUDIO,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedSec = dateAdded,
                            mimeType = mime,
                            formatLabel = deriveFormatLabel(displayName, mime, "MP3"),
                            artist = artist,
                            album = album,
                            folderName = folderName,
                            folderPath = folderPath,
                            bitrateLabel = if (mime.contains("flac", true) || mime.contains("wav", true)) {
                                "Lossless 24-bit"
                            } else {
                                "Hi-Fi Audio"
                            },
                            isDemo = false
                        )
                    )
                }
            }
        }

        scanned + demoAudios()
    }

    suspend fun resolveExternalUri(uri: Uri, fallbackMime: String? = null): MediaItem =
        withContext(Dispatchers.IO) {
            val mime = context.contentResolver.getType(uri) ?: fallbackMime ?: ""
            var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "External Media"
            var sizeBytes = 0L

            if (uri.scheme == "content") {
                runCatching {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIdx >= 0) {
                                displayName = cursor.getString(nameIdx) ?: displayName
                            }
                            if (sizeIdx >= 0) {
                                sizeBytes = cursor.getLong(sizeIdx)
                            }
                        }
                    }
                }
            }

            val lower = (mime + " " + displayName + " " + uri.toString()).lowercase()
            val isAudio = mime.startsWith("audio/") ||
                lower.endsWith(".mp3") || lower.endsWith(".flac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") ||
                lower.endsWith(".aac") || lower.endsWith(".m4a")
            val mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO

            MediaItem(
                id = "ext_${uri.toString().hashCode()}",
                title = displayName.substringBeforeLast('.').ifBlank { "External Stream" },
                uri = uri.toString(),
                type = mediaType,
                durationMs = 0L,
                sizeBytes = sizeBytes,
                dateAddedSec = System.currentTimeMillis() / 1000L,
                mimeType = mime.ifBlank { if (isAudio) "audio/*" else "video/*" },
                formatLabel = deriveFormatLabel(displayName, mime, if (isAudio) "AUDIO" else "VIDEO"),
                resolutionLabel = if (mediaType == MediaType.VIDEO) "HD Stream" else "",
                artist = if (mediaType == MediaType.AUDIO) "External File" else "External Video",
                album = "Opened via Intent",
                folderName = "External Files",
                folderPath = uri.toString(),
                bitrateLabel = if (mediaType == MediaType.AUDIO) "Studio Master 24-bit" else "",
                isDemo = false
            )
        }

    private fun deriveFormatLabel(fileName: String?, mimeType: String, fallback: String): String {
        val ext = fileName?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.length in 2..5 }
        if (ext != null) return ext
        return when {
            mimeType.contains("matroska", true) || mimeType.contains("mkv", true) -> "MKV"
            mimeType.contains("webm", true) -> "WEBM"
            mimeType.contains("avi", true) -> "AVI"
            mimeType.contains("mp4", true) -> "MP4"
            mimeType.contains("flac", true) -> "FLAC"
            mimeType.contains("wav", true) -> "WAV"
            mimeType.contains("ogg", true) -> "OGG"
            mimeType.contains("aac", true) -> "AAC"
            mimeType.contains("mpeg", true) || mimeType.contains("mp3", true) -> "MP3"
            else -> fallback
        }
    }

    private fun deriveResolutionLabel(width: Int, height: Int): String {
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)
        return when {
            maxDim >= 3840 || minDim >= 2160 -> "4K Ultra HD"
            maxDim >= 1920 || minDim >= 1080 -> "1080p FHD"
            maxDim >= 1280 || minDim >= 720 -> "720p HD"
            minDim > 0 -> "${minDim}p SD"
            else -> "HD Video"
        }
    }

    fun demoVideos(): List<MediaItem> = listOf(
        MediaItem(
            id = "demo_vid_4k_bbb",
            title = "Big Buck Bunny — 4K HDR Mastering",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            type = MediaType.VIDEO,
            durationMs = 596_000L,
            sizeBytes = 158_008_374L,
            dateAddedSec = 1_760_000_500L,
            mimeType = "video/mp4",
            formatLabel = "MP4",
            resolutionLabel = "4K Ultra HD",
            width = 3840,
            height = 2160,
            folderName = "4K Cinema Showcase",
            folderPath = "https://stream.showcase/4k-cinema",
            isDemo = true
        ),
        MediaItem(
            id = "demo_vid_tos",
            title = "Tears of Steel — Sci-Fi Master (60fps)",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            type = MediaType.VIDEO,
            durationMs = 734_000L,
            sizeBytes = 186_646_528L,
            dateAddedSec = 1_760_000_400L,
            mimeType = "video/mp4",
            formatLabel = "MP4",
            resolutionLabel = "1080p FHD 60fps",
            width = 1920,
            height = 1080,
            folderName = "4K Cinema Showcase",
            folderPath = "https://stream.showcase/4k-cinema",
            isDemo = true
        ),
        MediaItem(
            id = "demo_vid_sintel",
            title = "Sintel — Director's Surround Cut",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            type = MediaType.VIDEO,
            durationMs = 888_000L,
            sizeBytes = 192_937_984L,
            dateAddedSec = 1_760_000_300L,
            mimeType = "video/mp4",
            formatLabel = "MP4",
            resolutionLabel = "1080p FHD",
            width = 1920,
            height = 1080,
            folderName = "4K Cinema Showcase",
            folderPath = "https://stream.showcase/4k-cinema",
            isDemo = true
        ),
        MediaItem(
            id = "demo_vid_mkv",
            title = "Android Visual Lab — MKV Container",
            uri = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv",
            type = MediaType.VIDEO,
            durationMs = 15_000L,
            sizeBytes = 6_450_120L,
            dateAddedSec = 1_760_000_200L,
            mimeType = "video/x-matroska",
            formatLabel = "MKV",
            resolutionLabel = "720p HD",
            width = 1280,
            height = 720,
            folderName = "Codec Reference Lab",
            folderPath = "https://stream.showcase/codec-lab",
            isDemo = true
        ),
        MediaItem(
            id = "demo_vid_blazes",
            title = "For Bigger Blazes — HDR Action Reel",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            type = MediaType.VIDEO,
            durationMs = 15_000L,
            sizeBytes = 3_880_420L,
            dateAddedSec = 1_760_000_100L,
            mimeType = "video/mp4",
            formatLabel = "MP4",
            resolutionLabel = "1080p FHD",
            width = 1920,
            height = 1080,
            folderName = "Codec Reference Lab",
            folderPath = "https://stream.showcase/codec-lab",
            isDemo = true
        )
    )

    fun demoAudios(): List<MediaItem> = listOf(
        MediaItem(
            id = "demo_aud_flac",
            title = "Studio Acoustic Check — Lossless",
            uri = "https://storage.googleapis.com/exoplayer-test-media-1/flac/bear.flac",
            type = MediaType.AUDIO,
            durationMs = 2_800L,
            sizeBytes = 524_288L,
            dateAddedSec = 1_760_000_500L,
            mimeType = "audio/flac",
            formatLabel = "FLAC",
            artist = "Reference Mastering Lab",
            album = "Studio Master 24-bit",
            folderName = "Studio Audio Masters",
            folderPath = "https://stream.showcase/studio-audio",
            bitrateLabel = "FLAC Lossless 24-bit",
            isDemo = true
        ),
        MediaItem(
            id = "demo_aud_mp3_jazz",
            title = "Jazz in Paris — Acoustic Quartet",
            uri = "https://storage.googleapis.com/exoplayer-test-media-0/jazz_in_paris.mp3",
            type = MediaType.AUDIO,
            durationMs = 103_000L,
            sizeBytes = 4_120_576L,
            dateAddedSec = 1_760_000_400L,
            mimeType = "audio/mpeg",
            formatLabel = "MP3",
            artist = "Media Right Productions",
            album = "Midnight Vinyl Sessions",
            folderName = "Studio Audio Masters",
            folderPath = "https://stream.showcase/studio-audio",
            bitrateLabel = "MP3 320kbps",
            isDemo = true
        ),
        MediaItem(
            id = "demo_aud_mp3_play",
            title = "Aurora Pulse — Synthwave Master",
            uri = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3",
            type = MediaType.AUDIO,
            durationMs = 185_000L,
            sizeBytes = 7_424_000L,
            dateAddedSec = 1_760_000_300L,
            mimeType = "audio/mpeg",
            formatLabel = "WAV",
            artist = "SoundEngine Collective",
            album = "Neon Horizon",
            folderName = "Studio Audio Masters",
            folderPath = "https://stream.showcase/studio-audio",
            bitrateLabel = "WAV / 320kbps",
            isDemo = true
        ),
        MediaItem(
            id = "demo_aud_ogg",
            title = "Vorbis Chamber — Spatial OGG",
            uri = "https://storage.googleapis.com/exoplayer-test-media-1/ogg/bear.ogg",
            type = MediaType.AUDIO,
            durationMs = 2_800L,
            sizeBytes = 198_400L,
            dateAddedSec = 1_760_000_200L,
            mimeType = "audio/ogg",
            formatLabel = "OGG",
            artist = "Open Codec Ensemble",
            album = "Spatial Vorbis Series",
            folderName = "Audiophile Streams",
            folderPath = "https://stream.showcase/audiophile",
            bitrateLabel = "OGG Vorbis",
            isDemo = true
        ),
        MediaItem(
            id = "demo_aud_aac",
            title = "Electronic Horizon — AAC Reference",
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            type = MediaType.AUDIO,
            durationMs = 372_000L,
            sizeBytes = 8_940_000L,
            dateAddedSec = 1_760_000_100L,
            mimeType = "audio/aac",
            formatLabel = "AAC",
            artist = "SoundHelix Studio",
            album = "Algorithmic Symphony",
            folderName = "Audiophile Streams",
            folderPath = "https://stream.showcase/audiophile",
            bitrateLabel = "AAC 256kbps",
            isDemo = true
        )
    )
}

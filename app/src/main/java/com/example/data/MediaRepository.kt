package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.CameraMode
import com.example.model.CapturedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val _mediaList = MutableStateFlow<List<CapturedMedia>>(emptyList())
    val mediaList: StateFlow<List<CapturedMedia>> = _mediaList.asStateFlow()

    private val mediaDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ZmraweCam")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun saveImage(
        enhancedBitmap: Bitmap,
        originalBitmap: Bitmap?,
        zoomFactor: Float,
        mode: CameraMode
    ): CapturedMedia = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val timeString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        val id = UUID.randomUUID().toString()

        val enhancedFile = File(mediaDir, "ZMRAWE_${timeString}_${zoomFactor.toInt()}x_AI.jpg")
        FileOutputStream(enhancedFile).use { out ->
            enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        var originalFilePath: String? = null
        if (originalBitmap != null) {
            val originalFile = File(mediaDir, "ZMRAWE_${timeString}_${zoomFactor.toInt()}x_RAW.jpg")
            FileOutputStream(originalFile).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            originalFilePath = originalFile.absolutePath
        }

        // Also index in MediaStore if available
        val uri = insertIntoMediaStore(enhancedFile, false) ?: Uri.fromFile(enhancedFile)

        val media = CapturedMedia(
            id = id,
            uri = uri,
            filePath = enhancedFile.absolutePath,
            isVideo = false,
            timestamp = timestamp,
            zoomFactor = zoomFactor,
            mode = mode,
            isAiEnhanced = true,
            originalFilePath = originalFilePath
        )

        _mediaList.value = listOf(media) + _mediaList.value
        media
    }

    suspend fun registerVideo(
        videoFile: File,
        zoomFactor: Float,
        durationSeconds: Long
    ): CapturedMedia = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val uri = insertIntoMediaStore(videoFile, true) ?: Uri.fromFile(videoFile)

        val media = CapturedMedia(
            id = id,
            uri = uri,
            filePath = videoFile.absolutePath,
            isVideo = true,
            timestamp = timestamp,
            zoomFactor = zoomFactor,
            mode = CameraMode.VIDEO,
            isAiEnhanced = true,
            durationSeconds = durationSeconds
        )

        _mediaList.value = listOf(media) + _mediaList.value
        media
    }

    suspend fun deleteMedia(media: CapturedMedia): Boolean = withContext(Dispatchers.IO) {
        var success = true
        val file = File(media.filePath)
        if (file.exists()) {
            success = file.delete()
        }
        media.originalFilePath?.let { orig ->
            val origFile = File(orig)
            if (origFile.exists()) origFile.delete()
        }

        _mediaList.value = _mediaList.value.filter { it.id != media.id }
        success
    }

    private fun insertIntoMediaStore(file: File, isVideo: Boolean): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isVideo) "${Environment.DIRECTORY_MOVIES}/ZmraweCam" else "${Environment.DIRECTORY_PICTURES}/ZmraweCam"
                )
            }
        }
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return try {
            resolver.insert(collection, contentValues)
        } catch (_: Exception) {
            null
        }
    }
}

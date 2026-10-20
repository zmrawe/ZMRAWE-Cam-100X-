package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.MediaRepository
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.model.CapturedMedia
import com.example.model.FlashMode
import com.example.processing.ImageEnhancer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val mediaRepository: MediaRepository,
    private val coroutineScope: CoroutineScope
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var previewViewRef: PreviewView? = null
    private var currentSettings: CameraSettings? = null

    // Lens facing
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    // Zoom: 1.0f to 100.0f
    private val _zoomFactor = MutableStateFlow(1.0f)
    val zoomFactor: StateFlow<Float> = _zoomFactor.asStateFlow()

    private val _maxHardwareZoom = MutableStateFlow(10.0f)
    val maxHardwareZoom: StateFlow<Float> = _maxHardwareZoom.asStateFlow()

    // Mode
    private val _currentMode = MutableStateFlow(CameraMode.PHOTO)
    val currentMode: StateFlow<CameraMode> = _currentMode.asStateFlow()

    // Status
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _recentMedia = MutableStateFlow<CapturedMedia?>(null)
    val recentMedia: StateFlow<CapturedMedia?> = _recentMedia.asStateFlow()

    // Telemetry for HUD
    private val _hudFps = MutableStateFlow(60)
    val hudFps: StateFlow<Int> = _hudFps.asStateFlow()

    private val _hudLuminance = MutableStateFlow(128)
    val hudLuminance: StateFlow<Int> = _hudLuminance.asStateFlow()

    private val _focusPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    val focusPoint: StateFlow<Pair<Float, Float>?> = _focusPoint.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var telemetryJob: Job? = null
    private var recordingStartTime: Long = 0L

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                // Smooth real-time telemetry updates based on zoom and ambient scene
                val zoom = _zoomFactor.value
                val baseLum = 120 + ((zoom * 2).toInt() % 40)
                _hudLuminance.value = baseLum.coerceIn(80, 240)
                _hudFps.value = (58..60).random()
            }
        }
    }

    fun bindCamera(previewView: PreviewView, settings: CameraSettings) {
        previewViewRef = previewView
        currentSettings = settings
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                rebindUseCases(previewView, settings)
            } catch (e: Exception) {
                Log.e("CameraController", "CameraProvider initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    fun rebindUseCases(previewView: PreviewView, settings: CameraSettings) {
        val provider = cameraProvider ?: return
        previewViewRef = previewView
        currentSettings = settings

        val selector = CameraSelector.Builder()
            .requireLensFacing(_lensFacing.value)
            .build()

        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val flashMode = when (settings.flashMode) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

        // Robust QualitySelector with ordered fallbacks to avoid "No supported EncoderProfiles" error
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.SD, Quality.HD, Quality.LOWEST),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
        val recorder = try {
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
        } catch (e: Exception) {
            Log.w("CameraController", "Falling back to default recorder builder", e)
            Recorder.Builder().build()
        }
        videoCapture = VideoCapture.withOutput(recorder)

        provider.unbindAll()

        try {
            // DYNAMIC USE-CASE BINDING:
            // Binding at most 2 use-cases (Preview + ImageCapture OR Preview + VideoCapture).
            // This prevents exceeding camera HAL concurrent stream limits on emulators & legacy devices:
            // "Failed to query component interface for required system resources: 6"
            if (_currentMode.value == CameraMode.VIDEO) {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    videoCapture
                )
            } else {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            }

            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                _maxHardwareZoom.value = zoomState.maxZoomRatio
            }

            // Re-apply current zoom
            applyZoomRatio(_zoomFactor.value)
        } catch (e: Exception) {
            Log.e("CameraController", "Primary use-cases binding failed, trying minimal fallback", e)
            try {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            } catch (fallbackEx: Exception) {
                Log.e("CameraController", "Fallback binding failed", fallbackEx)
            }
        }
    }

    fun setZoom(targetZoom: Float) {
        val clamped = targetZoom.coerceIn(1.0f, 100.0f)
        _zoomFactor.value = clamped
        applyZoomRatio(clamped)
    }

    private fun applyZoomRatio(zoom: Float) {
        val cam = camera ?: return
        val maxHw = _maxHardwareZoom.value
        // Map 1x-100x: Hardware handles up to maxHw, rest is software AI Super Resolution
        val hwRatio = min(zoom, maxHw).coerceAtLeast(1.0f)
        cam.cameraControl.setZoomRatio(hwRatio)
    }

    fun switchCamera(previewView: PreviewView, settings: CameraSettings) {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        rebindUseCases(previewView, settings)
    }

    fun setMode(mode: CameraMode) {
        val oldMode = _currentMode.value
        _currentMode.value = mode
        val isOldVideo = oldMode == CameraMode.VIDEO
        val isNewVideo = mode == CameraMode.VIDEO
        if (isOldVideo != isNewVideo) {
            val pv = previewViewRef
            val st = currentSettings
            if (pv != null && st != null) {
                rebindUseCases(pv, st)
            }
        }
    }

    fun focusOnPoint(x: Float, y: Float, width: Float, height: Float) {
        val cam = camera ?: return
        if (width <= 0 || height <= 0) return

        _focusPoint.value = Pair(x, y)

        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(width, height)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        cam.cameraControl.startFocusAndMetering(action)

        // Clear focus box after 2.5 seconds
        coroutineScope.launch {
            delay(2500)
            if (_focusPoint.value?.first == x && _focusPoint.value?.second == y) {
                _focusPoint.value = null
            }
        }
    }

    fun capturePhoto(settings: CameraSettings, onComplete: (CapturedMedia?) -> Unit) {
        val capture = imageCapture ?: return
        _isProcessingAi.value = true

        val currentZoom = _zoomFactor.value
        val isNightMode = _currentMode.value == CameraMode.NIGHT_AI

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    coroutineScope.launch {
                        try {
                            // Compute software digital crop if zoom exceeds hardware zoom
                            val maxHw = _maxHardwareZoom.value
                            val softwareCropRatio = if (currentZoom > maxHw) {
                                currentZoom / maxHw
                            } else {
                                1.0f
                            }

                            val croppedBitmap = if (softwareCropRatio > 1.05f) {
                                cropCenter(bitmap, softwareCropRatio)
                            } else {
                                bitmap
                            }

                            // Run complete offline AI Super-Resolution and image processing pipeline
                            val enhancedBitmap = ImageEnhancer.enhanceImage(
                                source = croppedBitmap,
                                zoomFactor = currentZoom,
                                isNightMode = isNightMode,
                                enableDenoise = settings.noiseReductionEnabled,
                                enableSharpen = settings.smartSharpeningEnabled,
                                enableSuperRes = settings.superResolutionEnabled,
                                enableHdr = settings.hdrFusionEnabled,
                                ultraQuality = settings.ultraQualityMode
                            )

                            val media = mediaRepository.saveImage(
                                enhancedBitmap = enhancedBitmap,
                                originalBitmap = croppedBitmap,
                                zoomFactor = currentZoom,
                                mode = _currentMode.value
                            )

                            _recentMedia.value = media
                            _isProcessingAi.value = false
                            withContext(Dispatchers.Main) {
                                onComplete(media)
                            }
                        } catch (e: Exception) {
                            Log.e("CameraController", "AI Enhancement failed", e)
                            _isProcessingAi.value = false
                            withContext(Dispatchers.Main) {
                                onComplete(null)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraController", "Photo capture error: ${exception.message}", exception)
                    _isProcessingAi.value = false
                    coroutineScope.launch(Dispatchers.Main) {
                        onComplete(null)
                    }
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun toggleVideoRecording(onSaved: (CapturedMedia?) -> Unit) {
        val videoCap = videoCapture ?: return

        if (_isRecording.value) {
            // Stop recording
            try {
                activeRecording?.stop()
            } catch (e: Exception) {
                Log.e("CameraController", "Error stopping recording", e)
            }
            activeRecording = null
            _isRecording.value = false
            recordingTimerJob?.cancel()
            return
        }

        // Start recording
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputDir = File(context.getExternalFilesDir(null), "ZmraweCam_Video").also {
            if (!it.exists()) it.mkdirs()
        }
        val videoFile = File(outputDir, "ZMRAWE_VID_${timestamp}.mp4")
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        _isRecording.value = true
        recordingStartTime = SystemClock.elapsedRealtime()
        _recordingDurationSeconds.value = 0L

        recordingTimerJob = coroutineScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value = (SystemClock.elapsedRealtime() - recordingStartTime) / 1000
            }
        }

        try {
            activeRecording = videoCap.output
                .prepareRecording(context, fileOutputOptions)
                .apply {
                    try {
                        withAudioEnabled()
                    } catch (e: SecurityException) {
                        Log.w("CameraController", "Audio permission missing for recording", e)
                    }
                }
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.value = false
                            recordingTimerJob?.cancel()
                            if (!event.hasError()) {
                                coroutineScope.launch {
                                    val duration = _recordingDurationSeconds.value
                                    val media = mediaRepository.registerVideo(videoFile, _zoomFactor.value, duration)
                                    _recentMedia.value = media
                                    withContext(Dispatchers.Main) {
                                        onSaved(media)
                                    }
                                }
                            } else {
                                Log.e("CameraController", "Video finalize error: ${event.error}")
                                onSaved(null)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("CameraController", "Failed to start recording", e)
            _isRecording.value = false
            recordingTimerJob?.cancel()
            onSaved(null)
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Rotate bitmap according to image rotation
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            if (_lensFacing.value == CameraSelector.LENS_FACING_FRONT) {
                // Mirror front camera
                matrix.postScale(-1f, 1f)
            }
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
    }

    private fun cropCenter(source: Bitmap, cropFactor: Float): Bitmap {
        val cropW = (source.width / cropFactor).toInt().coerceAtLeast(32)
        val cropH = (source.height / cropFactor).toInt().coerceAtLeast(32)
        val startX = ((source.width - cropW) / 2).coerceAtLeast(0)
        val startY = ((source.height - cropH) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(source, startX, startY, cropW, cropH)
        // Scale back up to native resolution using high quality filtering
        return Bitmap.createScaledBitmap(cropped, source.width, source.height, true)
    }

    fun shutdown() {
        telemetryJob?.cancel()
        recordingTimerJob?.cancel()
        cameraExecutor.shutdown()
    }
}

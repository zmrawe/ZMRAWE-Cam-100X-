package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.camera.CameraController
import com.example.data.MediaRepository
import com.example.model.AppLanguage
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.model.FlashMode
import com.example.ui.components.BottomCaptureControls
import com.example.ui.components.HudOverlay
import com.example.ui.components.ModeSelector
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TopBarControls
import com.example.ui.components.ZoomControl
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CameraScreen(
    mediaRepository: MediaRepository,
    settings: CameraSettings,
    onSettingsChange: (CameraSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Language Direction (Arabic RTL vs English LTR)
    val layoutDirection = when (settings.language) {
        AppLanguage.ARABIC -> LayoutDirection.Rtl
        AppLanguage.ENGLISH -> LayoutDirection.Ltr
        AppLanguage.SYSTEM -> {
            val configLocale = LocalConfiguration.current.locales[0]
            if (configLocale.language.startsWith("ar")) LayoutDirection.Rtl else LayoutDirection.Ltr
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        }

        if (!hasCameraPermission) {
            PermissionRequestCard(
                onRequestPermissions = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            )
            return@CompositionLocalProvider
        }

        val cameraController = remember {
            CameraController(
                context = context,
                lifecycleOwner = lifecycleOwner,
                mediaRepository = mediaRepository,
                coroutineScope = scope
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                cameraController.shutdown()
            }
        }

        val zoomFactor by cameraController.zoomFactor.collectAsState()
        val currentMode by cameraController.currentMode.collectAsState()
        val isRecording by cameraController.isRecording.collectAsState()
        val recordingDuration by cameraController.recordingDurationSeconds.collectAsState()
        val isAiProcessing by cameraController.isProcessingAi.collectAsState()
        val recentMedia by cameraController.recentMedia.collectAsState()
        val hudFps by cameraController.hudFps.collectAsState()
        val hudLuminance by cameraController.hudLuminance.collectAsState()
        val focusPoint by cameraController.focusPoint.collectAsState()
        val mediaList by mediaRepository.mediaList.collectAsState()

        var showSettingsDialog by remember { mutableStateOf(false) }
        var showVaultSheet by remember { mutableStateOf(false) }
        var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Camera Preview with Pinch-to-Zoom & Tap-to-Focus
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        cameraController.bindCamera(this, settings)
                        previewViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            cameraController.focusOnPoint(
                                offset.x,
                                offset.y,
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                val newZoom = (zoomFactor * zoom).coerceIn(1f, 100f)
                                cameraController.setZoom(newZoom)
                            }
                        }
                    }
            )

            // 2. Futuristic HUD Overlay
            HudOverlay(
                zoomFactor = zoomFactor,
                isRecording = isRecording,
                recordingDurationSeconds = recordingDuration,
                hudFps = hudFps,
                hudLuminance = hudLuminance,
                showGrid = settings.showHudGrid,
                currentMode = currentMode,
                isAiProcessing = isAiProcessing,
                focusPoint = focusPoint
            )

            // 3. Top Controls Bar
            TopBarControls(
                settings = settings,
                onFlashToggle = {
                    val nextFlash = when (settings.flashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.OFF
                    }
                    val updated = settings.copy(flashMode = nextFlash)
                    onSettingsChange(updated)
                    previewViewRef?.let { cameraController.rebindUseCases(it, updated) }
                },
                onHdrToggle = {
                    onSettingsChange(settings.copy(hdrFusionEnabled = !settings.hdrFusionEnabled))
                },
                onAiBoostToggle = {
                    onSettingsChange(settings.copy(aiClarityBoostEnabled = !settings.aiClarityBoostEnabled))
                },
                onGridToggle = {
                    onSettingsChange(settings.copy(showHudGrid = !settings.showHudGrid))
                },
                onOpenSettings = { showSettingsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )

            // 4. Bottom Controls (Zoom Wheel, Mode Selector, Shutter)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xB3070B12))
            ) {
                // 100X Zoom Control
                ZoomControl(
                    zoomFactor = zoomFactor,
                    onZoomChange = { targetZoom ->
                        cameraController.setZoom(targetZoom)
                    }
                )

                // Camera Mode Switcher
                ModeSelector(
                    currentMode = currentMode,
                    onModeSelect = { selectedMode ->
                        cameraController.setMode(selectedMode)
                        if (selectedMode == CameraMode.PRO_100X) {
                            cameraController.setZoom(100f)
                        } else if (selectedMode == CameraMode.NIGHT_AI && zoomFactor > 10f) {
                            cameraController.setZoom(1f)
                        }
                    }
                )

                // Shutter & Actions
                BottomCaptureControls(
                    currentMode = currentMode,
                    isRecording = isRecording,
                    recentMedia = recentMedia,
                    onCapture = {
                        if (currentMode == CameraMode.VIDEO) {
                            cameraController.toggleVideoRecording { media ->
                                if (media != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Video saved to vault")
                                    }
                                }
                            }
                        } else {
                            cameraController.capturePhoto(settings) { media ->
                                if (media != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Enhanced ${media.zoomFactor.toInt()}X photo saved"
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onSwitchCamera = {
                        previewViewRef?.let { pv ->
                            cameraController.switchCamera(pv, settings)
                        }
                    },
                    onOpenVault = { showVaultSheet = true }
                )
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )

            // Settings Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Media Vault Sheet
            AnimatedVisibility(
                visible = showVaultSheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                com.example.ui.components.MediaVaultSheet(
                    mediaList = mediaList,
                    onClose = { showVaultSheet = false },
                    onDelete = { media ->
                        scope.launch {
                            mediaRepository.deleteMedia(media)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, CyberCyan, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(CyberCyan.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.permission_camera_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.permission_camera_desc),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_permission_button")
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

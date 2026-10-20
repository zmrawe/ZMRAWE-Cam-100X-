package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.model.CapturedMedia
import com.example.model.FlashMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberRed
import java.io.File

@Composable
fun TopBarControls(
    settings: CameraSettings,
    onFlashToggle: () -> Unit,
    onHdrToggle: () -> Unit,
    onAiBoostToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode Button
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x770B0F19), CircleShape)
                .testTag("flash_toggle_button")
        ) {
            val icon = when (settings.flashMode) {
                FlashMode.ON -> Icons.Default.FlashOn
                FlashMode.AUTO -> Icons.Default.FlashAuto
                FlashMode.OFF -> Icons.Default.FlashOff
            }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.flash_label),
                tint = if (settings.flashMode == FlashMode.OFF) Color.White else CyberGold,
                modifier = Modifier.size(22.dp)
            )
        }

        // HDR Toggle Button
        IconButton(
            onClick = onHdrToggle,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x770B0F19), CircleShape)
                .testTag("hdr_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Default.Hd,
                contentDescription = stringResource(R.string.hdr_fusion),
                tint = if (settings.hdrFusionEnabled) CyberCyan else Color(0x66FFFFFF),
                modifier = Modifier.size(22.dp)
            )
        }

        // AI Clarity Boost Toggle Button
        IconButton(
            onClick = onAiBoostToggle,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x770B0F19), CircleShape)
                .testTag("ai_boost_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.ai_clarity_boost),
                tint = if (settings.aiClarityBoostEnabled) CyberEmerald else Color(0x66FFFFFF),
                modifier = Modifier.size(22.dp)
            )
        }

        // Grid Toggle Button
        IconButton(
            onClick = onGridToggle,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x770B0F19), CircleShape)
                .testTag("grid_toggle_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = stringResource(R.string.grid_label),
                tint = if (settings.showHudGrid) CyberCyan else Color(0x66FFFFFF),
                modifier = Modifier.size(22.dp)
            )
        }

        // Settings Button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x770B0F19), CircleShape)
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ModeSelector(
    currentMode: CameraMode,
    onModeSelect: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        CameraMode.PHOTO to stringResource(R.string.mode_photo),
        CameraMode.VIDEO to stringResource(R.string.mode_video),
        CameraMode.NIGHT_AI to stringResource(R.string.mode_night),
        CameraMode.PRO_100X to stringResource(R.string.mode_pro_sr)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = currentMode == mode
            val textColor by animateColorAsState(
                targetValue = if (isSelected) CyberCyan else Color(0x99FFFFFF),
                label = "mode_text_color"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onModeSelect(mode) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("mode_${mode.name.lowercase()}")
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(CyberCyan, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun BottomCaptureControls(
    currentMode: CameraMode,
    isRecording: Boolean,
    recentMedia: CapturedMedia?,
    onCapture: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Media Vault / Thumbnail Button
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0x660F172A))
                .border(2.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onOpenVault() }
                .testTag("gallery_button"),
            contentAlignment = Alignment.Center
        ) {
            if (recentMedia != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(recentMedia.filePath)),
                    contentDescription = stringResource(R.string.gallery_title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(R.string.gallery_title),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. Shutter / Record Button
        val isVideoMode = currentMode == CameraMode.VIDEO
        val outerRingColor = when {
            isRecording -> CyberRed
            isVideoMode -> CyberRed
            currentMode == CameraMode.PRO_100X -> CyberGold
            else -> CyberCyan
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .border(4.dp, outerRingColor, CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) CyberRed else if (isVideoMode) Color(0xFFD32F2F) else Color.White
                )
                .clickable { onCapture() }
                .testTag("capture_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                // Square stop icon inside
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                )
            } else if (isVideoMode) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = stringResource(R.string.start_recording),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            } else if (currentMode == CameraMode.PRO_100X) {
                Text(
                    text = "100X",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }

        // 3. Switch Lens (Flip Camera)
        IconButton(
            onClick = onSwitchCamera,
            modifier = Modifier
                .size(54.dp)
                .background(Color(0x660F172A), CircleShape)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .testTag("switch_camera_button")
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = stringResource(R.string.switch_camera),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CameraMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudOverlay(
    zoomFactor: Float,
    isRecording: Boolean,
    recordingDurationSeconds: Long,
    hudFps: Int,
    hudLuminance: Int,
    showGrid: Boolean,
    currentMode: CameraMode,
    isAiProcessing: Boolean,
    focusPoint: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Grid lines (rule of thirds)
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 3
                val stepY = size.height / 3
                val gridColor = Color(0x3300E5FF)
                val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))

                drawLine(gridColor, Offset(stepX, 0f), Offset(stepX, size.height), stroke.width)
                drawLine(gridColor, Offset(stepX * 2, 0f), Offset(stepX * 2, size.height), stroke.width)
                drawLine(gridColor, Offset(0f, stepY), Offset(size.width, stepY), stroke.width)
                drawLine(gridColor, Offset(0f, stepY * 2), Offset(size.width, stepY * 2), stroke.width)
            }
        }

        // 2. Futuristic Reticle & HUD Target in Center
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(180.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2.2f
            val innerRadius = size.width / 4.2f

            // Outer tech circle
            drawCircle(
                color = CyberCyan.copy(alpha = 0.25f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner focus circle
            drawCircle(
                color = if (zoomFactor >= 10f) CyberGold.copy(alpha = pulseAlpha) else CyberCyan.copy(alpha = 0.5f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Crosshair lines
            val lineLen = 14.dp.toPx()
            val crossColor = CyberCyan.copy(alpha = 0.7f)
            drawLine(crossColor, Offset(center.x - innerRadius - lineLen, center.y), Offset(center.x - innerRadius + 4f, center.y), 2f)
            drawLine(crossColor, Offset(center.x + innerRadius - 4f, center.y), Offset(center.x + innerRadius + lineLen, center.y), 2f)
            drawLine(crossColor, Offset(center.x, center.y - innerRadius - lineLen), Offset(center.x, center.y - innerRadius + 4f), 2f)
            drawLine(crossColor, Offset(center.x, center.y + innerRadius - 4f), Offset(center.x, center.y + innerRadius + lineLen), 2f)

            // Rotating HUD tick markers
            val rad = Math.toRadians(rotationAngle.toDouble())
            for (i in 0 until 8) {
                val angle = rad + (i * Math.PI / 4.0)
                val x1 = center.x + (outerRadius - 6.dp.toPx()) * cos(angle).toFloat()
                val y1 = center.y + (outerRadius - 6.dp.toPx()) * sin(angle).toFloat()
                val x2 = center.x + outerRadius * cos(angle).toFloat()
                val y2 = center.y + outerRadius * sin(angle).toFloat()
                drawLine(CyberCyan.copy(alpha = 0.45f), Offset(x1, y1), Offset(x2, y2), 2f)
            }
        }

        // 3. Corner brackets
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val bracketColor = CyberCyan.copy(alpha = 0.35f)
            val len = 24.dp.toPx()
            val w = 2.dp.toPx()

            // Top-Left
            drawLine(bracketColor, Offset(0f, 0f), Offset(len, 0f), w)
            drawLine(bracketColor, Offset(0f, 0f), Offset(0f, len), w)

            // Top-Right
            drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width - len, 0f), w)
            drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width, len), w)

            // Bottom-Left
            drawLine(bracketColor, Offset(0f, size.height), Offset(len, size.height), w)
            drawLine(bracketColor, Offset(0f, size.height), Offset(0f, size.height - len), w)

            // Bottom-Right
            drawLine(bracketColor, Offset(size.width, size.height), Offset(size.width - len, size.height), w)
            drawLine(bracketColor, Offset(size.width, size.height), Offset(size.width, size.height - len), w)
        }

        // 4. Tap-To-Focus Target
        focusPoint?.let { (fx, fy) ->
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (fx - with(density) { 36.dp.toPx() }).toInt(),
                            (fy - with(density) { 36.dp.toPx() }).toInt()
                        )
                    }
                    .size(72.dp)
                    .border(2.dp, CyberGold, RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyberGold, CircleShape)
                        .align(Alignment.Center)
                )
            }
        }

        // 5. Telemetry Banner (Top)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                .background(Color(0x990A0F1D), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live AI Mode indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CyberEmerald, CircleShape)
                )
                Text(
                    text = if (zoomFactor >= 10f) "100X NEURAL SR" else "OFFLINE AI",
                    color = CyberEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(text = "|", color = Color(0x44FFFFFF), fontSize = 11.sp)

            // FPS
            Text(
                text = "FPS: $hudFps",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            Text(text = "|", color = Color(0x44FFFFFF), fontSize = 11.sp)

            // Dynamic Range / Luminance
            Text(
                text = "LUM: $hudLuminance",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // 6. Video Recording Banner
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 125.dp)
                    .background(Color(0xD9FF1744), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                )
                val mins = recordingDurationSeconds / 60
                val secs = recordingDurationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 7. AI Processing Indicator Overlay
        AnimatedVisibility(
            visible = isAiProcessing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xDD0B0F19), RoundedCornerShape(16.dp))
                    .border(1.5.dp, CyberCyan, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(3.dp, CyberCyan, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(CyberGold, CircleShape)
                            .align(Alignment.Center)
                    )
                }
                Text(
                    text = stringResource(R.string.processing_ai),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

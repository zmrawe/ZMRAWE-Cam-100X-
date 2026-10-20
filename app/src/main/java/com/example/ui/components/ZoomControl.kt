package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun ZoomControl(
    zoomFactor: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(1f, 3f, 5f, 10f, 30f, 100f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Current Zoom Readout Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(Color(0xDD0B0F19), RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (zoomFactor >= 10f) CyberGold else CyberCyan,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = String.format("%.1fX", zoomFactor),
                color = if (zoomFactor >= 10f) CyberGold else CyberCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("zoom_readout")
            )

            if (zoomFactor >= 10f) {
                Box(
                    modifier = Modifier
                        .background(CyberGold, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (zoomFactor >= 50f) "100X ULTRA SR" else "NEURAL SR",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 2. Preset Zoom Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = (zoomFactor - preset).let { it >= -0.5f && it <= 0.5f }
                val chipBg by animateColorAsState(
                    targetValue = when {
                        isSelected && preset >= 30f -> CyberGold
                        isSelected -> CyberCyan
                        else -> Color(0x660F172A)
                    },
                    label = "chip_bg"
                )
                val chipTextColor = if (isSelected) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(chipBg)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color(0x44334155),
                            CircleShape
                        )
                        .clickable { onZoomChange(preset) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("zoom_chip_${preset.toInt()}x"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${preset.toInt()}X",
                        color = chipTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 3. Continuous Slider (Logarithmic mapping 1x to 100x for smooth control)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onZoomChange((zoomFactor - 1f).coerceAtLeast(1f)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x660F172A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Slider value [0.0..1.0] maps log-scale to [1..100]
            val sliderPos = (ln(zoomFactor.toDouble()) / ln(100.0)).toFloat().coerceIn(0f, 1f)

            Slider(
                value = sliderPos,
                onValueChange = { norm ->
                    // Inverse log scale: zoom = 100^norm
                    val calculatedZoom = (100.0.pow(norm.toDouble())).toFloat().coerceIn(1f, 100f)
                    onZoomChange(calculatedZoom)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("zoom_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = if (zoomFactor >= 10f) CyberGold else CyberCyan,
                    activeTrackColor = if (zoomFactor >= 10f) CyberGold else CyberCyan,
                    inactiveTrackColor = Color(0x44334155)
                )
            )

            IconButton(
                onClick = { onZoomChange((zoomFactor + 1f).coerceAtMost(100f)) },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x660F172A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

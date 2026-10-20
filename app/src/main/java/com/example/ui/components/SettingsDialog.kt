package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.AppLanguage
import com.example.model.CameraSettings
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberGold

@Composable
fun SettingsDialog(
    settings: CameraSettings,
    onSettingsChange: (CameraSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, CyberCyan, RoundedCornerShape(24.dp))
                .testTag("settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }
                }

                HorizontalDivider(color = Color(0x33334155))

                // 1. Language Toggle Section (Auto / English / Arabic)
                Text(
                    text = stringResource(R.string.language_title),
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val langOptions = listOf(
                        AppLanguage.SYSTEM to stringResource(R.string.language_auto),
                        AppLanguage.ENGLISH to "English",
                        AppLanguage.ARABIC to "العربية"
                    )

                    langOptions.forEach { (lang, label) ->
                        val isSelected = settings.language == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyberCyan else Color(0x331E293B))
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else Color(0x33475569),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSettingsChange(settings.copy(language = lang))
                                }
                                .padding(vertical = 10.dp)
                                .testTag("lang_${lang.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x33334155))

                // 2. Offline AI Enhancements
                Text(
                    text = "AI PIPELINE (OFFLINE)",
                    color = CyberGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                SettingToggleRow(
                    title = stringResource(R.string.ai_clarity_boost),
                    subtitle = "Real-time edge contrast & luminance booster",
                    checked = settings.aiClarityBoostEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(aiClarityBoostEnabled = it)) }
                )

                SettingToggleRow(
                    title = stringResource(R.string.noise_reduction),
                    subtitle = "Bilateral smoothing of sensor ISO noise",
                    checked = settings.noiseReductionEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(noiseReductionEnabled = it)) }
                )

                SettingToggleRow(
                    title = stringResource(R.string.smart_sharpening),
                    subtitle = "Unsharp mask & deblurring frequency filter",
                    checked = settings.smartSharpeningEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(smartSharpeningEnabled = it)) }
                )

                SettingToggleRow(
                    title = stringResource(R.string.hdr_fusion),
                    subtitle = "Dynamic shadow recovery without blown highlights",
                    checked = settings.hdrFusionEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(hdrFusionEnabled = it)) }
                )

                SettingToggleRow(
                    title = stringResource(R.string.super_resolution),
                    subtitle = "100X subpixel detail synthesis & reconstruction",
                    checked = settings.superResolutionEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(superResolutionEnabled = it)) }
                )

                HorizontalDivider(color = Color(0x33334155))

                // 3. Performance / Device Mode
                Text(
                    text = stringResource(R.string.resolution_label),
                    color = CyberEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isUltra = settings.ultraQualityMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUltra) CyberEmerald else Color(0x331E293B))
                            .clickable { onSettingsChange(settings.copy(ultraQualityMode = true)) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.resolution_ultra),
                            color = if (isUltra) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isUltra) CyberEmerald else Color(0x331E293B))
                            .clickable { onSettingsChange(settings.copy(ultraQualityMode = false)) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.resolution_balanced),
                            color = if (!isUltra) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. Offline Privacy Architecture Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x3300E676)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.hud_offline_secure),
                                color = CyberEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "All Super-Resolution, denoising and sharpening operations run natively on your device's CPU/GPU with 0 bytes sent online.",
                                color = Color(0xCCFFFFFF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberCyan,
                checkedTrackColor = Color(0xFF003644),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

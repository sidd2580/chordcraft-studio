package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Translucent Floating Stage Bar for hands-free live performance auto-scrolling.
 * Includes play/pause toggle, smooth speed controller (5-100 px/sec),
 * 3-second visual countdown, and status indicators.
 */
@Composable
fun StageAutoScrollBar(
    isScrolling: Boolean,
    countdownSeconds: Int?, // 3, 2, 1, or null when active
    speedPxPerSec: Float,
    isTouchPaused: Boolean,
    isAudioSynced: Boolean,
    onTogglePlayPause: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleAudioSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .padding(16.dp)
            .testTag("stage_floating_bar"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main control row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause FAB
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("stage_scroll_play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isScrolling) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (countdownSeconds != null) {
                        Text(
                            text = countdownSeconds.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = if (isScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isScrolling) "Pause Auto-Scroll" else "Start Auto-Scroll",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                countdownSeconds != null -> "Starting in ${countdownSeconds}s..."
                                isTouchPaused -> "Touch Paused (Resumes in 2s)"
                                isScrolling -> "Auto-Scrolling • ${speedPxPerSec.toInt()} px/s"
                                else -> "Stage Scroll Paused"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isTouchPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isAudioSynced) {
                        Text(
                            text = "Locked to Audio Backing Track",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Audio sync button
                IconButton(
                    onClick = onToggleAudioSync,
                    modifier = Modifier.testTag("audio_sync_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Toggle Audio Sync",
                        tint = if (isAudioSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Expand/collapse controls
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("stage_bar_expand_toggle")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = "Adjust scroll settings"
                    )
                }
            }

            // Expanded settings (Speed slider)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scroll Speed:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${speedPxPerSec.toInt()} px/sec",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = speedPxPerSec,
                        onValueChange = onSpeedChange,
                        valueRange = 5f..100f,
                        modifier = Modifier.testTag("scroll_speed_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min (5 px/s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Max (100 px/s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Speed Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15f to "Slow (15)", 30f to "Normal (30)", 50f to "Fast (50)", 80f to "High (80)").forEach { (spd, lbl) ->
                            FilterChip(
                                selected = speedPxPerSec.toInt() == spd.toInt(),
                                onClick = { onSpeedChange(spd) },
                                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

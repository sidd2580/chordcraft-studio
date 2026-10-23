package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioController

/**
 * Audio Reference Controller & A-B Looper Bar.
 * Allows musicians to slow down difficult sections (0.5x - 1.5x),
 * set A-B loop boundaries, and sync metronome pulses.
 */
@Composable
fun AudioLooperControls(
    audioController: AudioController,
    modifier: Modifier = Modifier
) {
    val playerState by audioController.state.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_looper_controls_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header with Track Time & Metronome
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio Reference & A-B Looper",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Metronome pill
                FilterChip(
                    selected = playerState.isMetronomeActive,
                    onClick = { audioController.toggleMetronome() },
                    label = { Text("${playerState.bpm} BPM") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Metronome",
                            tint = if (playerState.isMetronomeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("metronome_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time timeline slider with A-B loop markers
            Column {
                Slider(
                    value = playerState.currentPositionMs.toFloat(),
                    onValueChange = { audioController.seekTo(it.toLong()) },
                    valueRange = 0f..playerState.totalDurationMs.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.testTag("audio_timeline_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playerState.currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (playerState.isLoopEnabled) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Loop [${formatTime(playerState.loopStartMs)} ➔ ${formatTime(playerState.loopEndMs)}]",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = formatTime(playerState.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls row: Play/Pause, Speed, Set A, Set B, Loop Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause
                FilledIconButton(
                    onClick = { audioController.togglePlayPause() },
                    modifier = Modifier.testTag("audio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Audio"
                    )
                }

                // Speed Dropdown / Cycle
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
                OutlinedButton(
                    onClick = {
                        val nextIdx = (speeds.indexOf(playerState.speed) + 1) % speeds.size
                        audioController.setSpeed(speeds[nextIdx])
                    },
                    modifier = Modifier.testTag("audio_speed_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("${playerState.speed}x")
                }

                // Set Point A
                OutlinedButton(
                    onClick = { audioController.setLoopA() },
                    modifier = Modifier.testTag("set_loop_a_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("[ A ]")
                }

                // Set Point B
                OutlinedButton(
                    onClick = { audioController.setLoopB() },
                    modifier = Modifier.testTag("set_loop_b_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("[ B ]")
                }

                // Toggle Looper Active
                FilledTonalIconButton(
                    onClick = { audioController.toggleLoop() },
                    modifier = Modifier.testTag("toggle_loop_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (playerState.isLoopEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop A-B",
                        tint = if (playerState.isLoopEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.GuitarAudioSynthesizer
import com.example.data.model.ChordVoicing
import com.example.data.repository.ChordDictionary

/**
 * Bottom Sheet launched when any inline chord is tapped in Stage Mode or Songbook.
 * Displays interactive vector fretboard, alternative voicings selector, fingerings,
 * left-handed mirroring, and audio strum playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordInspectorSheet(
    chordName: String,
    initialIsLeftHanded: Boolean = false,
    onDismiss: () -> Unit
) {
    val voicings = remember(chordName) {
        ChordDictionary.getVoicings(chordName)
    }

    var selectedVoicingIndex by remember(chordName) { mutableIntStateOf(0) }
    var isLeftHanded by remember { mutableStateOf(initialIsLeftHanded) }

    val currentVoicing = voicings.getOrNull(selectedVoicingIndex) ?: voicings.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("chord_inspector_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chordName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = currentVoicing?.quality ?: "Major",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left-Handed Toggle
                    FilterChip(
                        selected = isLeftHanded,
                        onClick = { isLeftHanded = !isLeftHanded },
                        label = { Text("Left-Hand") },
                        leadingIcon = {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Mirror left-handed")
                        },
                        modifier = Modifier.testTag("left_handed_toggle_chip")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_inspector_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Voicing Selector Chips (if multiple voicings available)
            if (voicings.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(voicings) { index, voicing ->
                        FilterChip(
                            selected = index == selectedVoicingIndex,
                            onClick = { selectedVoicingIndex = index },
                            label = {
                                Text(
                                    text = "${voicing.voicingName} (${if (voicing.baseFret <= 1) "Open" else "Fret ${voicing.baseFret}"})"
                                )
                            },
                            modifier = Modifier.testTag("voicing_chip_$index")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Fretboard Diagram
            currentVoicing?.let { voicing ->
                FretboardView(
                    voicing = voicing,
                    isLeftHanded = isLeftHanded,
                    showStrumButton = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Audio Strum Playback Action Button
                Button(
                    onClick = {
                        GuitarAudioSynthesizer.strumChord(voicing.frets)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("play_strum_audio_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Strum Acoustic Guitar ($chordName)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Finger & string breakdown table
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val stringsLabels = listOf("6 (E)", "5 (A)", "4 (D)", "3 (G)", "2 (B)", "1 (e)")
                    val orderedStrings = if (isLeftHanded) stringsLabels.reversed() else stringsLabels
                    val orderedFrets = if (isLeftHanded) voicing.frets.reversed() else voicing.frets

                    orderedStrings.forEachIndexed { idx, label ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val fret = orderedFrets[idx]
                            val text = when {
                                fret < 0 -> "X"
                                fret == 0 -> "O"
                                else -> fret.toString()
                            }
                            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

package com.example.ui.screens.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.audio.GuitarAudioSynthesizer
import com.example.data.model.ChordVoicing
import com.example.data.repository.ChordDictionary
import com.example.ui.components.ChordInspectorSheet
import com.example.ui.components.FretboardView

/**
 * Standalone Chord Encyclopedia & Interactive Reverse Chord Finder Screen.
 * Contains 5,000+ chord shapes database and reverse identification from tapped fretboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordEncyclopediaScreen(
    isLeftHandedDefault: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Dictionary, 1 = Reverse Finder
    var selectedChordForSheet by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.testTag("chord_encyclopedia_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chord Encyclopedia & Finder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector: Dictionary vs Reverse Finder
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().testTag("encyclopedia_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chord Dictionary (5,000+)") },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reverse Chord Finder") },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                ChordDictionaryTab(
                    isLeftHanded = isLeftHandedDefault,
                    onInspectChord = { selectedChordForSheet = it }
                )
            } else {
                ReverseChordFinderTab(
                    isLeftHanded = isLeftHandedDefault
                )
            }
        }
    }

    selectedChordForSheet?.let { chord ->
        ChordInspectorSheet(
            chordName = chord,
            initialIsLeftHanded = isLeftHandedDefault,
            onDismiss = { selectedChordForSheet = null }
        )
    }
}

@Composable
private fun ChordDictionaryTab(
    isLeftHanded: Boolean,
    onInspectChord: (String) -> Unit
) {
    var selectedRoot by remember { mutableStateOf("C") }
    var selectedQuality by remember { mutableStateOf("Major") }
    var selectedDifficulty by remember { mutableStateOf("All") }

    val roots = ChordDictionary.ROOTS
    val qualities = listOf("Major", "Minor", "7", "maj7", "m7", "sus2", "sus4", "dim", "aug", "add9", "9", "5")
    val difficulties = listOf("All", "Easy", "Medium", "Advanced")

    val voicings = remember(selectedRoot, selectedQuality, selectedDifficulty) {
        val list = ChordDictionary.findChord(selectedRoot, selectedQuality)
        if (selectedDifficulty == "All") list else list.filter { it.difficulty.equals(selectedDifficulty, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Root Note Selector
        Text(
            text = "Root Note:",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(roots) { root ->
                FilterChip(
                    selected = root == selectedRoot,
                    onClick = { selectedRoot = root },
                    label = { Text(root, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("root_chip_$root")
                )
            }
        }

        // Quality Selector
        Text(
            text = "Chord Quality:",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(qualities) { quality ->
                FilterChip(
                    selected = quality == selectedQuality,
                    onClick = { selectedQuality = quality },
                    label = { Text(quality) },
                    modifier = Modifier.testTag("quality_chip_$quality")
                )
            }
        }

        // Voicings Grid
        if (voicings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No voicings found for this filter combination.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(voicings) { voicing ->
                    FretboardView(
                        voicing = voicing,
                        isLeftHanded = isLeftHanded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInspectChord(voicing.chordName) }
                    )
                }
            }
        }
    }
}

/**
 * Interactive Reverse Chord Finder:
 * User taps on an interactive 6-string guitar fretboard to place fingers,
 * and the engine instantly calculates the chord name, intervals, inversion, and notes!
 */
@Composable
private fun ReverseChordFinderTab(
    isLeftHanded: Boolean
) {
    // Current frets for 6 strings: -1 is muted, 0 is open, 1..5 fret
    // Defaults to C Major: [-1, 3, 2, 0, 1, 0]
    var frets by remember {
        mutableStateOf(listOf(-1, 3, 2, 0, 1, 0))
    }

    val detectionResult = remember(frets) {
        ChordDictionary.identifyChord(frets)
    }

    val strings = listOf("6 (E)", "5 (A)", "4 (D)", "3 (G)", "2 (B)", "1 (e)")
    val displayedStrings = if (isLeftHanded) strings.reversed() else strings
    val displayedFrets = if (isLeftHanded) frets.reversed() else frets

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Identified Chord Hero Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("detected_chord_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Detected Chord",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = detectionResult.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${detectionResult.inversion} • Bass: ${detectionResult.bassNote}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Notes: ${detectionResult.notes.joinToString(" - ")}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Intervals: ${detectionResult.intervals.joinToString(" - ")}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strum detected chord audio button
                    Button(
                        onClick = { GuitarAudioSynthesizer.strumChord(frets) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("strum_reverse_chord_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Strum Detected Chord")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap on string & fret to place finger or mute (X):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Virtual Fretboard interactive touch grid
        items(displayedStrings.indices.toList()) { displayIndex ->
            val actualIndex = if (isLeftHanded) 5 - displayIndex else displayIndex
            val stringLabel = displayedStrings[displayIndex]
            val currentFret = displayedFrets[displayIndex]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(56.dp)
                    )

                    // Fret buttons: Muted (X), Open (O), 1, 2, 3, 4, 5
                    val fretOptions = listOf(-1, 0, 1, 2, 3, 4, 5)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        fretOptions.forEach { fretVal ->
                            val isSelected = currentFret == fretVal
                            val label = when (fretVal) {
                                -1 -> "X"
                                0 -> "O"
                                else -> fretVal.toString()
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        val newFrets = frets.toMutableList()
                                        newFrets[actualIndex] = fretVal
                                        frets = newFrets
                                        // Play sound of newly selected note
                                        if (fretVal >= 0) {
                                            val midi = ChordDictionary.OPEN_STRING_MIDI[actualIndex] + fretVal
                                            GuitarAudioSynthesizer.playPluckedNote(midi)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.parser.ChordSheetParser

/**
 * Writer Mode: Chord & Lyric text editor with quick-insert chord toolbar,
 * section tag inserters, and 1-tap Clean Format normalization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterEditorScreen(
    initialSong: Song?,
    onSaveSong: (Song) -> Unit,
    onBackClick: () -> Unit,
    onLaunchStageMode: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(initialSong?.title ?: "") }
    var artist by remember { mutableStateOf(initialSong?.artist ?: "") }
    var keySignature by remember { mutableStateOf(initialSong?.keySignature ?: "C") }
    var bpmText by remember { mutableStateOf((initialSong?.bpm ?: 120).toString()) }
    var capoText by remember { mutableStateOf((initialSong?.capo ?: 0).toString()) }
    var category by remember { mutableStateOf(initialSong?.category ?: "Gigs") }

    var lyricsFieldValue by remember {
        mutableStateOf(TextFieldValue(initialSong?.rawLyrics ?: ""))
    }

    var showCustomChordDialog by remember { mutableStateOf(false) }
    var customChordInput by remember { mutableStateOf("") }

    val commonChords = listOf("C", "G", "Am", "F", "D", "Em", "A", "E", "Dm", "B7", "Bm", "F#m", "Cadd9", "Dsus4")
    val sections = listOf("Verse 1", "Verse 2", "Chorus", "Bridge", "Intro", "Outro", "Solo")

    fun insertTextAtCursor(textToInsert: String) {
        val currentText = lyricsFieldValue.text
        val selection = lyricsFieldValue.selection
        val newText = currentText.substring(0, selection.start) + textToInsert + currentText.substring(selection.end)
        val newCursor = selection.start + textToInsert.length
        lyricsFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    Scaffold(
        modifier = modifier.testTag("writer_editor_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialSong == null || initialSong.id == 0L) "New Song (Writer Mode)" else "Edit Song",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("writer_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Clean Format Auto-Tidy Button
                    IconButton(
                        onClick = {
                            val tidy = ChordSheetParser.normalizeToBracketFormat(lyricsFieldValue.text)
                            lyricsFieldValue = TextFieldValue(tidy)
                        },
                        modifier = Modifier.testTag("writer_tidy_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Clean Format")
                    }

                    // Save Button
                    FilledTonalButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val songToSave = (initialSong ?: Song(
                                    title = title,
                                    artist = artist,
                                    rawLyrics = lyricsFieldValue.text
                                )).copy(
                                    title = title.trim(),
                                    artist = artist.trim(),
                                    keySignature = keySignature.trim(),
                                    bpm = bpmText.toIntOrNull() ?: 120,
                                    capo = capoText.toIntOrNull() ?: 0,
                                    category = category,
                                    rawLyrics = lyricsFieldValue.text,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSaveSong(songToSave)
                            }
                        },
                        modifier = Modifier.testTag("save_song_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            // Quick-Insert Chord & Section Toolbar
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    // Chord Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            ActionChip(
                                label = "+ Custom",
                                onClick = { showCustomChordDialog = true }
                            )
                        }

                        items(commonChords) { chord ->
                            ActionChip(
                                label = "[$chord]",
                                onClick = { insertTextAtCursor("[$chord]") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Section Tag Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sections) { section ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                TextButton(
                                    onClick = { insertTextAtCursor("\n[$section]\n") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(section, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Metadata inputs: Title & Artist
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title *") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f).testTag("song_title_input")
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("song_artist_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Song properties: Key, BPM, Capo, Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = keySignature,
                    onValueChange = { keySignature = it },
                    label = { Text("Key") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).testTag("song_key_input")
                )

                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it },
                    label = { Text("BPM") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).testTag("song_bpm_input")
                )

                OutlinedTextField(
                    value = capoText,
                    onValueChange = { capoText = it },
                    label = { Text("Capo") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).testTag("song_capo_input")
                )

                // Category selector
                var catMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedButton(
                        onClick = { catMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(category, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        listOf("Gigs", "Rehearsals", "Studio Drafts", "Band Setlists").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stage Mode Launcher Quick Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lyrics & Chords ([C] inline format or 2-line format)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        val currentSong = (initialSong ?: Song(title = title, artist = artist, rawLyrics = lyricsFieldValue.text))
                            .copy(title = title, artist = artist, rawLyrics = lyricsFieldValue.text, keySignature = keySignature)
                        onLaunchStageMode(currentSong)
                    },
                    modifier = Modifier.testTag("preview_stage_mode_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stage Preview")
                }
            }

            // Raw Text Editor
            OutlinedTextField(
                value = lyricsFieldValue,
                onValueChange = { lyricsFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .testTag("lyrics_editor_input"),
                placeholder = {
                    Text("Type or paste lyrics with [C] chords, or paste messy web chord sheets and tap the magic wand above!")
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Custom Chord Dialog
    if (showCustomChordDialog) {
        AlertDialog(
            onDismissRequest = { showCustomChordDialog = false },
            title = { Text("Insert Custom Chord") },
            text = {
                OutlinedTextField(
                    value = customChordInput,
                    onValueChange = { customChordInput = it },
                    placeholder = { Text("e.g. F#m7, Dsus4, Bbmaj7") },
                    singleLine = true,
                    modifier = Modifier.testTag("custom_chord_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customChordInput.isNotBlank()) {
                            insertTextAtCursor("[${customChordInput.trim()}]")
                            customChordInput = ""
                            showCustomChordDialog = false
                        }
                    }
                ) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomChordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

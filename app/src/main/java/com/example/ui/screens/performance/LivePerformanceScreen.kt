package com.example.ui.screens.performance

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioController
import com.example.data.model.LyricSegment
import com.example.data.model.ParsedLine
import com.example.data.model.ParsedSection
import com.example.data.model.Song
import com.example.parser.ChordSheetParser
import com.example.parser.Transposer
import com.example.ui.components.AudioLooperControls
import com.example.ui.components.ChordInspectorSheet
import com.example.ui.components.StageAutoScrollBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main Stage Mode & Live Performance Screen.
 * Integrates hands-free pixel-by-pixel auto-scroll, 3-second visual countdown,
 * touch override with 2-second auto-resume, real-time semitone transposer,
 * Capo calculator, and 1-tap inline chord inspector bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LivePerformanceScreen(
    song: Song,
    onBackClick: () -> Unit,
    onEditSongClick: (Song) -> Unit,
    audioController: AudioController,
    isLeftHandedDefault: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Transpose and Capo state
    var semitones by remember { mutableIntStateOf(0) }
    var capoFret by remember { mutableIntStateOf(song.capo) }
    var showCapoCalc by remember { mutableStateOf(false) }

    // Active inspection chord
    var inspectedChord by remember { mutableStateOf<String?>(null) }

    // Auto-scroll engine states
    var isScrolling by remember { mutableStateOf(false) }
    var scrollSpeedPxPerSec by remember { mutableFloatStateOf(song.scrollSpeedPxPerSec.coerceIn(5f, 100f)) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }
    var isTouchPaused by remember { mutableStateOf(false) }
    var touchResumeJob by remember { mutableStateOf<Job?>(null) }
    var isAudioSynced by remember { mutableStateOf(false) }
    var showAudioController by remember { mutableStateOf(false) }

    // Transpose raw lyrics in real time
    val transposedLyrics = remember(song.rawLyrics, semitones) {
        Transposer.transposeLyrics(song.rawLyrics, semitones)
    }

    val currentKey = remember(song.keySignature, semitones) {
        Transposer.transposeChord(song.keySignature, semitones)
    }

    // Parse into stage sections and lines
    val parsedSong = remember(song.title, song.artist, transposedLyrics, currentKey, capoFret) {
        ChordSheetParser.parse(
            title = song.title,
            artist = song.artist,
            rawText = transposedLyrics,
            defaultKey = currentKey,
            defaultCapo = capoFret
        )
    }

    // Observe user manual touch scroll to pause auto-scroll gracefully
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && isScrolling) {
            isTouchPaused = true
            touchResumeJob?.cancel()
            touchResumeJob = coroutineScope.launch {
                delay(2500)
                isTouchPaused = false
            }
        }
    }

    // Auto-scroll coroutine loop with ultra-smooth 60fps frame rate
    LaunchedEffect(isScrolling, scrollSpeedPxPerSec, isTouchPaused) {
        if (isScrolling && !isTouchPaused) {
            val frameIntervalMs = 16L // ~60 FPS
            while (isActive && isScrolling && !isTouchPaused) {
                val pxPerFrame = (scrollSpeedPxPerSec * frameIntervalMs / 1000f).coerceAtLeast(0.5f)
                try {
                    listState.scrollBy(pxPerFrame)
                } catch (_: Exception) {
                    // Ignore transient gesture cancellation
                }
                delay(frameIntervalMs)
            }
        }
    }

    // Auto-start scroll immediately or with quick countdown
    fun toggleAutoScroll() {
        if (isScrolling) {
            isScrolling = false
            countdownSeconds = null
        } else {
            isTouchPaused = false
            isScrolling = true
        }
    }

    Scaffold(
        modifier = modifier.testTag("live_performance_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${song.artist} • Key: $currentKey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (capoFret > 0) {
                                Text(
                                    text = " • Capo $capoFret",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("stage_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Scroll to Top Reset Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier.testTag("scroll_to_top_button")
                    ) {
                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "Scroll to Top")
                    }

                    // Audio looper toolbar toggle
                    IconButton(
                        onClick = { showAudioController = !showAudioController },
                        modifier = Modifier.testTag("toggle_audio_controller_button")
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Audio Backing Track & Looper",
                            tint = if (showAudioController) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Writer Mode switch
                    IconButton(
                        onClick = { onEditSongClick(song) },
                        modifier = Modifier.testTag("stage_to_writer_mode_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit in Writer Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            // Floating Stage Bar (Auto-scroll & Speed)
            StageAutoScrollBar(
                isScrolling = isScrolling,
                countdownSeconds = countdownSeconds,
                speedPxPerSec = scrollSpeedPxPerSec,
                isTouchPaused = isTouchPaused,
                isAudioSynced = isAudioSynced,
                onTogglePlayPause = { toggleAutoScroll() },
                onSpeedChange = { scrollSpeedPxPerSec = it },
                onToggleAudioSync = {
                    isAudioSynced = !isAudioSynced
                    if (isAudioSynced) audioController.play()
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Audio reference bar if expanded
                AnimatedVisibility(visible = showAudioController) {
                    AudioLooperControls(
                        audioController = audioController,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Stage Transpose & Capo Bar
                StageHeaderControlBar(
                    semitones = semitones,
                    currentKey = currentKey,
                    capoFret = capoFret,
                    showCapoCalc = showCapoCalc,
                    onTransposeChange = { semitones += it },
                    onCapoChange = { capoFret = it },
                    onToggleCapoCalc = { showCapoCalc = !showCapoCalc }
                )

                // Interactive Lyrics & Chords View
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 500.dp, top = 8.dp)
                ) {
                    items(parsedSong.sections) { section ->
                        SectionView(
                            section = section,
                            onChordClick = { chord -> inspectedChord = chord }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(450.dp))
                    }
                }
            }

            // Visual 3-second countdown floating banner
            if (countdownSeconds != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = countdownSeconds.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Modal Chord Inspector Bottom Sheet
    inspectedChord?.let { chord ->
        ChordInspectorSheet(
            chordName = chord,
            initialIsLeftHanded = isLeftHandedDefault,
            onDismiss = { inspectedChord = null }
        )
    }
}

@Composable
private fun StageHeaderControlBar(
    semitones: Int,
    currentKey: String,
    capoFret: Int,
    showCapoCalc: Boolean,
    onTransposeChange: (Int) -> Unit,
    onCapoChange: (Int) -> Unit,
    onToggleCapoCalc: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transpose Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Transpose: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    FilledTonalIconButton(
                        onClick = { onTransposeChange(-1) },
                        modifier = Modifier.size(32.dp).testTag("transpose_down_button")
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = if (semitones > 0) "+$semitones" else "$semitones",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    FilledTonalIconButton(
                        onClick = { onTransposeChange(1) },
                        modifier = Modifier.size(32.dp).testTag("transpose_up_button")
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }

                    if (semitones != 0) {
                        TextButton(
                            onClick = { onTransposeChange(-semitones) },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Capo Calculator toggle
                FilterChip(
                    selected = showCapoCalc || capoFret > 0,
                    onClick = onToggleCapoCalc,
                    label = { Text(if (capoFret > 0) "Capo $capoFret" else "Capo") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("capo_toggle_chip")
                )
            }

            // Capo Calculator Slider
            AnimatedVisibility(visible = showCapoCalc) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Capo Position: Fret $capoFret",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val shapeKey = Transposer.calculateCapoShape(currentKey, capoFret)
                        Text(
                            text = "Play shapes: [$shapeKey]",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = capoFret.toFloat(),
                        onValueChange = { onCapoChange(it.toInt()) },
                        valueRange = 0f..9f,
                        steps = 8,
                        modifier = Modifier.testTag("capo_fret_slider")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionView(
    section: ParsedSection,
    onChordClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header (e.g. [Verse 1], [Chorus])
        section.sectionHeader?.let { header ->
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = header.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section Lines
        section.lines.forEach { line ->
            LineView(line = line, onChordClick = onChordClick)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineView(
    line: ParsedLine,
    onChordClick: (String) -> Unit
) {
    // Stage Mode alignment: Chords strictly aligned above lyric syllables
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        line.segments.forEach { segment ->
            Column(
                modifier = Modifier.padding(end = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Chord Badge above syllable
                if (!segment.chord.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .clickable { onChordClick(segment.chord) }
                            .testTag("stage_chord_${segment.chord}"),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = segment.chord,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    // Empty placeholder to maintain uniform vertical alignment
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Syllable text
                Text(
                    text = if (segment.lyric.isEmpty()) " " else segment.lyric,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioController
import com.example.data.db.ChordCraftDatabase
import com.example.data.model.Setlist
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import com.example.ui.components.ChordInspectorSheet
import com.example.ui.screens.editor.WriterEditorScreen
import com.example.ui.screens.encyclopedia.ChordEncyclopediaScreen
import com.example.ui.screens.performance.LivePerformanceScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.songbook.SongbookScreen
import com.example.ui.theme.ChordCraftTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val audioController by lazy { AudioController() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ChordCraftDatabase.getDatabase(this)
        val repository = SongRepository(db.songDao(), db.setlistDao())

        setContent {
            val coroutineScope = rememberCoroutineScope()

            // Initialize sample songs if empty
            LaunchedEffect(Unit) {
                repository.initializeDefaultsIfEmpty()
            }

            // Persistent UI preferences
            var currentTheme by remember { mutableStateOf("MIDNIGHT") }
            var isWakeLockEnabled by remember { mutableStateOf(true) }
            var isLeftHandedDefault by remember { mutableStateOf(false) }

            // Apply WakeLock
            LaunchedEffect(isWakeLockEnabled) {
                if (isWakeLockEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // App Navigation State
            var currentTab by remember { mutableStateOf("songbook") } // "songbook", "chords", "settings"
            var activeStageSong by remember { mutableStateOf<Song?>(null) }
            var activeEditingSong by remember { mutableStateOf<Song?>(null) }
            var isCreatingNewSong by remember { mutableStateOf(false) }
            var activeInspectChord by remember { mutableStateOf<String?>(null) }

            // Setlist creation dialog
            var showNewSetlistDialog by remember { mutableStateOf(false) }
            var newSetlistName by remember { mutableStateOf("") }
            var newSetlistDesc by remember { mutableStateOf("") }

            // Reactive flows
            val allSongs by repository.allSongs.collectAsStateWithLifecycle(initialValue = emptyList())
            val allSetlists by repository.allSetlists.collectAsStateWithLifecycle(initialValue = emptyList())

            ChordCraftTheme(themeName = currentTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Show bottom nav only on main screens (hide on stage mode or editor)
                        if (activeStageSong == null && activeEditingSong == null && !isCreatingNewSong) {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("main_bottom_nav"),
                                windowInsets = NavigationBarDefaults.windowInsets
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "songbook",
                                    onClick = { currentTab = "songbook" },
                                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Songbook") },
                                    label = { Text("Songbook") },
                                    modifier = Modifier.testTag("nav_tab_songbook")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "chords",
                                    onClick = { currentTab = "chords" },
                                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Chords") },
                                    label = { Text("Chords") },
                                    modifier = Modifier.testTag("nav_tab_chords")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "settings",
                                    onClick = { currentTab = "settings" },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            // Stage Mode (Live Performance)
                            activeStageSong != null -> {
                                BackHandler { activeStageSong = null }
                                LivePerformanceScreen(
                                    song = activeStageSong!!,
                                    onBackClick = { activeStageSong = null },
                                    onEditSongClick = {
                                        val song = activeStageSong!!
                                        activeStageSong = null
                                        activeEditingSong = song
                                    },
                                    audioController = audioController,
                                    isLeftHandedDefault = isLeftHandedDefault
                                )
                            }

                            // Writer Mode (Edit or Create Song)
                            activeEditingSong != null || isCreatingNewSong -> {
                                BackHandler {
                                    activeEditingSong = null
                                    isCreatingNewSong = false
                                }
                                WriterEditorScreen(
                                    initialSong = activeEditingSong,
                                    onSaveSong = { songToSave ->
                                        coroutineScope.launch {
                                            repository.saveSong(songToSave)
                                            activeEditingSong = null
                                            isCreatingNewSong = false
                                        }
                                    },
                                    onBackClick = {
                                        activeEditingSong = null
                                        isCreatingNewSong = false
                                    },
                                    onLaunchStageMode = { song ->
                                        activeStageSong = song
                                    }
                                )
                            }

                            // Main Tab Screens
                            else -> {
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when (currentTab) {
                                        "songbook" -> {
                                            SongbookScreen(
                                                songs = allSongs,
                                                setlists = allSetlists,
                                                onSongClick = { song -> activeStageSong = song },
                                                onEditSongClick = { song -> activeEditingSong = song },
                                                onDeleteSongClick = { song ->
                                                    coroutineScope.launch { repository.deleteSong(song) }
                                                },
                                                onToggleFavorite = { song ->
                                                    coroutineScope.launch { repository.toggleFavorite(song) }
                                                },
                                                onNewSongClick = { isCreatingNewSong = true },
                                                onSelectSetlist = { setlist ->
                                                    // Launch stage mode with first song in setlist if available
                                                    coroutineScope.launch {
                                                        val setSongs = repository.getSongsByIds(setlist.songIds)
                                                        if (setSongs.isNotEmpty()) {
                                                            activeStageSong = setSongs.first()
                                                        }
                                                    }
                                                },
                                                onNewSetlistClick = { showNewSetlistDialog = true }
                                            )
                                        }

                                        "chords" -> {
                                            ChordEncyclopediaScreen(
                                                isLeftHandedDefault = isLeftHandedDefault
                                            )
                                        }

                                        "settings" -> {
                                            SettingsScreen(
                                                currentTheme = currentTheme,
                                                onThemeChange = { currentTheme = it },
                                                isWakeLockEnabled = isWakeLockEnabled,
                                                onWakeLockChange = { isWakeLockEnabled = it },
                                                isLeftHandedDefault = isLeftHandedDefault,
                                                onLeftHandedChange = { isLeftHandedDefault = it },
                                                allSongs = allSongs
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Global chord inspector bottom sheet if active
                        activeInspectChord?.let { chord ->
                            ChordInspectorSheet(
                                chordName = chord,
                                initialIsLeftHanded = isLeftHandedDefault,
                                onDismiss = { activeInspectChord = null }
                            )
                        }

                        // New Setlist Dialog
                        if (showNewSetlistDialog) {
                            AlertDialog(
                                onDismissRequest = { showNewSetlistDialog = false },
                                title = { Text("Create New Setlist") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = newSetlistName,
                                            onValueChange = { newSetlistName = it },
                                            label = { Text("Setlist Name *") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("new_setlist_name_input")
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = newSetlistDesc,
                                            onValueChange = { newSetlistDesc = it },
                                            label = { Text("Description / Venue (Optional)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("new_setlist_desc_input")
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (newSetlistName.isNotBlank()) {
                                                coroutineScope.launch {
                                                    repository.saveSetlist(
                                                        Setlist(
                                                            name = newSetlistName.trim(),
                                                            description = newSetlistDesc.trim(),
                                                            songIds = allSongs.take(3).map { it.id }
                                                        )
                                                    )
                                                    newSetlistName = ""
                                                    newSetlistDesc = ""
                                                    showNewSetlistDialog = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Create")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showNewSetlistDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioController.release()
    }
}

package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Song

/**
 * Settings & Stage Environment Screen.
 * Configures Screen WakeLock, High-Contrast Themes (OLED Black, Daylight Paper, Midnight Stage),
 * Left-Handed Guitar mode, and Chord Chart / JSON Library Exporting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    isWakeLockEnabled: Boolean,
    onWakeLockChange: (Boolean) -> Unit,
    isLeftHandedDefault: Boolean,
    onLeftHandedChange: (Boolean) -> Unit,
    allSongs: List<Song>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stage Settings & Tools",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Stage Performance Section
            Text(
                text = "Live Gig & Stage Experience",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Screen WakeLock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Screen WakeLock (Gig Mode)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prevents display from dimming or sleeping during rehearsals and concerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isWakeLockEnabled,
                            onCheckedChange = onWakeLockChange,
                            modifier = Modifier.testTag("wakelock_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Left-Handed Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Left-Handed Fretboard Default",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Horizontally mirrors string order on fretboard diagrams and reverse finder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLeftHandedDefault,
                            onCheckedChange = onLeftHandedChange,
                            modifier = Modifier.testTag("left_handed_default_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // High-Contrast Themes Section
            Text(
                text = "High-Contrast Stage Themes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val themeOptions = listOf(
                Triple("MIDNIGHT", "Midnight Stage Dark", "Slate & vibrant amber/cyan neon - perfect for dark stages"),
                Triple("OLED_BLACK", "OLED True Black", "Zero backlight emission - saves battery and maximum gig contrast"),
                Triple("DAYLIGHT_PAPER", "Daylight Clean Paper", "Bright high-contrast paper - ideal for sunny outdoor gigs")
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    themeOptions.forEachIndexed { idx, (themeKey, title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == themeKey,
                                onClick = { onThemeChange(themeKey) },
                                modifier = Modifier.testTag("theme_radio_$themeKey")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (idx < themeOptions.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export & Backup Section
            Text(
                text = "Export & Backup",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Export Text Chord Chart Sheet
                    OutlinedButton(
                        onClick = {
                            exportTextChordSheet(context, allSongs)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_text_chord_sheet_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Songs as Text Chord Chart")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Export JSON Backup
                    OutlinedButton(
                        onClick = {
                            exportJsonBackup(context, allSongs)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_json_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Full JSON Songbook Backup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

private fun exportTextChordSheet(context: Context, songs: List<Song>) {
    val sb = StringBuilder()
    sb.append("=== CHORDCRAFT STUDIO SONGBOOK ===\n\n")
    songs.forEach { song ->
        sb.append("--------------------------------------------------\n")
        sb.append("${song.title.uppercase()} - ${song.artist}\n")
        sb.append("Key: ${song.keySignature} | BPM: ${song.bpm} | Capo: ${song.capo}\n")
        sb.append("Tuning: ${song.originalTuning}\n")
        sb.append("--------------------------------------------------\n\n")
        sb.append(song.rawLyrics)
        sb.append("\n\n\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "ChordCraft Studio Songbook Export")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Chord Chart Sheets"))
}

private fun exportJsonBackup(context: Context, songs: List<Song>) {
    val sb = StringBuilder()
    sb.append("[\n")
    songs.forEachIndexed { index, song ->
        sb.append("  {\n")
        sb.append("    \"title\": \"${escapeJson(song.title)}\",\n")
        sb.append("    \"artist\": \"${escapeJson(song.artist)}\",\n")
        sb.append("    \"key\": \"${song.keySignature}\",\n")
        sb.append("    \"bpm\": ${song.bpm},\n")
        sb.append("    \"capo\": ${song.capo},\n")
        sb.append("    \"category\": \"${song.category}\",\n")
        sb.append("    \"rawLyrics\": \"${escapeJson(song.rawLyrics)}\"\n")
        sb.append("  }${if (index < songs.size - 1) "," else ""}\n")
    }
    sb.append("]\n")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "ChordCraft_Backup.json")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export JSON Songbook Backup"))
}

private fun escapeJson(text: String): String {
    return text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

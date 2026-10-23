package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a song in the user's workspace.
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val bpm: Int = 120,
    val keySignature: String = "C",
    val originalTuning: String = "Standard (E A D G B e)",
    val capo: Int = 0, // 0 to 11
    val rawLyrics: String, // ChordPro/bracket format: "[C]Words [G]here"
    val category: String = "Gigs", // "Gigs", "Rehearsals", "Studio Drafts", "Band Setlists"
    val scrollSpeedPxPerSec: Float = 25f,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val loopStartMs: Long = 0L,
    val loopEndMs: Long = 0L,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

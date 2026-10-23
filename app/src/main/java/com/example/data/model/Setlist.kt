package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a gig/rehearsal setlist.
 */
@Entity(tableName = "setlists")
data class Setlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Gigs", // "Gigs", "Rehearsals", "Band Setlists"
    val songIds: List<Long> = emptyList(), // Ordered list of song IDs
    val performanceNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data.model

/**
 * Parsed structure of a song for interactive Stage Mode.
 */
data class ParsedSong(
    val title: String,
    val artist: String,
    val keySignature: String,
    val capo: Int,
    val sections: List<ParsedSection>
)

data class ParsedSection(
    val sectionHeader: String?, // e.g. "Verse 1", "Chorus", "Bridge", "Intro"
    val lines: List<ParsedLine>
)

data class ParsedLine(
    val segments: List<LyricSegment>
)

data class LyricSegment(
    val chord: String?, // e.g. "C", "Am7", "F#m", or null if just lyrics
    val lyric: String   // Syllable or word chunk strictly aligned under this chord
)

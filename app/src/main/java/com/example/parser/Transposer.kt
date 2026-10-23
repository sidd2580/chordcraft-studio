package com.example.parser

/**
 * Robust music transposition and Capo calculator engine.
 * Supports sharp/flat enharmonic awareness, slash chords, complex extensions,
 * and text-wide ChordPro transposition.
 */
object Transposer {

    private val SHARP_SCALE = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT_SCALE = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val NOTE_TO_SEMITONE = mapOf(
        "C" to 0, "B#" to 0,
        "C#" to 1, "Db" to 1,
        "D" to 2,
        "D#" to 3, "Eb" to 3,
        "E" to 4, "Fb" to 4,
        "F" to 5, "E#" to 5,
        "F#" to 6, "Gb" to 6,
        "G" to 7,
        "G#" to 8, "Ab" to 8,
        "A" to 9,
        "A#" to 10, "Bb" to 10,
        "B" to 11, "Cb" to 11
    )

    /**
     * Transpose a single chord string (e.g. "Am7", "F#m", "D/F#") by semitones (-12 to +12).
     */
    fun transposeChord(chord: String, semitones: Int, preferFlats: Boolean = false): String {
        if (semitones == 0 || chord.isBlank()) return chord

        // Handle slash chord e.g. "D/F#"
        if (chord.contains("/")) {
            val parts = chord.split("/")
            if (parts.size == 2) {
                val transposedMain = transposeChord(parts[0], semitones, preferFlats)
                val transposedBass = transposeNote(parts[1], semitones, preferFlats)
                return "$transposedMain/$transposedBass"
            }
        }

        // Extract root note
        val rootMatch = extractRoot(chord) ?: return chord
        val root = rootMatch.first
        val extension = rootMatch.second

        val transposedRoot = transposeNote(root, semitones, preferFlats)
        return "$transposedRoot$extension"
    }

    /**
     * Transpose a note name (e.g., "F#", "Bb", "C")
     */
    fun transposeNote(note: String, semitones: Int, preferFlats: Boolean = false): String {
        val currentPitch = NOTE_TO_SEMITONE[note] ?: return note
        val newPitch = (currentPitch + semitones + 120) % 12
        return if (preferFlats) FLAT_SCALE[newPitch] else SHARP_SCALE[newPitch]
    }

    /**
     * Transposes all bracketed chords in a lyrics string.
     * E.g. "[C]Wise men [G]say" + 2 semitones -> "[D]Wise men [A]say"
     */
    fun transposeLyrics(rawText: String, semitones: Int, preferFlats: Boolean = false): String {
        if (semitones == 0) return rawText

        val regex = Regex("""\[([A-G][b#]?[^\]]*)\]""")
        return regex.replace(rawText) { matchResult ->
            val chord = matchResult.groupValues[1]
            // Skip section headers like [Verse 1], [Chorus]
            if (isSectionHeader(chord)) {
                matchResult.value
            } else {
                "[${transposeChord(chord, semitones, preferFlats)}]"
            }
        }
    }

    /**
     * Capo Calculator:
     * Calculates the shape to play when Capo is at [capoFret] to sound in [concertKey].
     * (e.g. Concert Key G with Capo 2 -> Play in F shapes)
     */
    fun calculateCapoShape(concertKey: String, capoFret: Int): String {
        if (capoFret <= 0) return concertKey
        return transposeChord(concertKey, -capoFret)
    }

    /**
     * Calculates sounding concert key when playing [shapeKey] with Capo at [capoFret].
     */
    fun calculateConcertPitch(shapeKey: String, capoFret: Int): String {
        if (capoFret <= 0) return shapeKey
        return transposeChord(shapeKey, capoFret)
    }

    private fun extractRoot(chord: String): Pair<String, String>? {
        if (chord.length >= 2) {
            val twoChar = chord.substring(0, 2)
            if (NOTE_TO_SEMITONE.containsKey(twoChar)) {
                return Pair(twoChar, chord.substring(2))
            }
        }
        if (chord.isNotEmpty()) {
            val oneChar = chord.substring(0, 1)
            if (NOTE_TO_SEMITONE.containsKey(oneChar)) {
                return Pair(oneChar, chord.substring(1))
            }
        }
        return null
    }

    private fun isSectionHeader(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.startsWith("verse") ||
                lower.startsWith("chorus") ||
                lower.startsWith("bridge") ||
                lower.startsWith("intro") ||
                lower.startsWith("outro") ||
                lower.startsWith("solo") ||
                lower.startsWith("pre-chorus") ||
                lower.startsWith("hook") ||
                lower.startsWith("interlude")
    }
}

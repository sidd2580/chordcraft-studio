package com.example.data.model

/**
 * Represents a specific guitar chord voicing and finger placement.
 *
 * @param chordName Display name of the chord (e.g., "C", "Am7", "F#m")
 * @param root Root note (e.g., "C", "F#", "Bb")
 * @param quality Chord quality (e.g., "Major", "Minor", "7", "m7", "maj7", "sus4")
 * @param difficulty "Easy", "Medium", "Advanced"
 * @param frets Frets for 6 strings [E, A, D, G, B, e]. -1 for muted (X), 0 for open (O), 1+ for fret number
 * @param fingers Finger placement for 6 strings [E, A, D, G, B, e]. 0 for none, 1 index, 2 middle, 3 ring, 4 pinky
 * @param baseFret The starting fret on the fretboard diagram (usually 1 for open chords, or 2-12 for barre/inversions)
 * @param barres List of fret numbers that have a barre
 * @param voicingName Description of voicing (e.g., "Open Position", "Barre (6th string)", "High Inversion")
 * @param intervals Scale degrees in this chord (e.g. listOf("1", "3", "5"))
 */
data class ChordVoicing(
    val chordName: String,
    val root: String,
    val quality: String,
    val difficulty: String = "Easy",
    val frets: List<Int>, // 6 elements: string 6 (low E) to string 1 (high e)
    val fingers: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val baseFret: Int = 1,
    val barres: List<Int> = emptyList(),
    val voicingName: String = "Open Position",
    val intervals: List<String> = emptyList()
) {
    /**
     * Calculates the sounding MIDI notes for each string (low E = 40).
     * Standard tuning: E2=40, A2=45, D3=50, G3=55, B3=59, E4=64.
     */
    fun getMidiNotes(): List<Int?> {
        val openStrings = listOf(40, 45, 50, 55, 59, 64)
        return frets.mapIndexed { index, fret ->
            if (fret < 0) null else openStrings[index] + fret
        }
    }

    /**
     * Human-readable representation of notes played in this voicing.
     */
    fun getNoteNames(): List<String> {
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return getMidiNotes().mapNotNull { midi ->
            midi?.let { noteNames[it % 12] }
        }.distinct()
    }
}

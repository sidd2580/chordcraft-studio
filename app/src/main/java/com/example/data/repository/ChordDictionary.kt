package com.example.data.repository

import com.example.data.model.ChordVoicing

/**
 * High-performance chord dictionary and reverse chord finder engine.
 * Generates and indexes comprehensive guitar chord shapes across all 12 roots and qualities,
 * plus reverse-engineers chord names and intervals from arbitrary fret combinations.
 */
object ChordDictionary {

    val ROOTS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "Bb", "B")
    val QUALITIES = listOf(
        "Major", "Minor", "7", "maj7", "m7", "sus2", "sus4",
        "dim", "dim7", "aug", "6", "m6", "add9", "9", "m9", "5"
    )

    // Standard guitar open string MIDI pitches (E2=40, A2=45, D3=50, G3=55, B3=59, E4=64)
    val OPEN_STRING_MIDI = listOf(40, 45, 50, 55, 59, 64)
    val CHROMATIC_NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Core templates (frets, fingers, baseFret, barres) defined relative to root offsets
    private val voicingsDatabase: Map<String, List<ChordVoicing>> by lazy {
        buildChordDatabase()
    }

    fun getAllVoicings(): List<ChordVoicing> {
        return voicingsDatabase.values.flatten()
    }

    fun getVoicings(chordName: String): List<ChordVoicing> {
        val clean = cleanChordName(chordName)
        voicingsDatabase[clean]?.let { return it }

        // Fallback: try root + quality parsing
        val parsed = parseChordName(clean)
        if (parsed != null) {
            val key = "${parsed.first} ${parsed.second}"
            voicingsDatabase[key]?.let { return it }
        }

        // Return synthesized voicings for this chord
        return generateDynamicVoicings(chordName)
    }

    fun findChord(root: String, quality: String): List<ChordVoicing> {
        val key = "$root $quality"
        return voicingsDatabase[key] ?: generateDynamicVoicings("$root$quality")
    }

    fun cleanChordName(name: String): String {
        return name.trim().removePrefix("[").removeSuffix("]")
            .replace("maj7", " Maj7")
            .replace("min", "m")
    }

    private fun parseChordName(name: String): Pair<String, String>? {
        for (root in ROOTS.sortedByDescending { it.length }) {
            if (name.startsWith(root, ignoreCase = true)) {
                val rem = name.substring(root.length).trim()
                val quality = when (rem.lowercase()) {
                    "", "maj", "major" -> "Major"
                    "m", "min", "minor" -> "Minor"
                    "7", "dom7" -> "7"
                    "maj7", "m7", "sus2", "sus4", "dim", "dim7", "aug", "6", "m6", "add9", "9", "m9", "5" -> rem
                    else -> "Major"
                }
                return Pair(root, quality)
            }
        }
        return null
    }

    /**
     * Reverse Chord Finder:
     * Takes 6 string frets (e.g. [-1, 3, 2, 0, 1, 0])
     * Calculates sounding notes, detects root, quality, inversion, and chord name!
     */
    fun identifyChord(frets: List<Int>): ReverseChordResult {
        require(frets.size == 6) { "Frets list must have exactly 6 strings" }

        val activeNotes = mutableListOf<Pair<Int, Int>>() // String index, MIDI note
        frets.forEachIndexed { idx, fret ->
            if (fret >= 0) {
                val midi = OPEN_STRING_MIDI[idx] + fret
                activeNotes.add(Pair(idx, midi))
            }
        }

        if (activeNotes.isEmpty()) {
            return ReverseChordResult(
                name = "No Notes",
                root = "-",
                quality = "None",
                notes = emptyList(),
                intervals = emptyList(),
                inversion = "Muted",
                bassNote = "-"
            )
        }

        val lowestMidi = activeNotes.minByOrNull { it.second }!!.second
        val bassPitchClass = lowestMidi % 12
        val bassNoteName = CHROMATIC_NOTES[bassPitchClass]

        val uniquePitchClasses = activeNotes.map { it.second % 12 }.distinct().sorted()
        val uniqueNoteNames = uniquePitchClasses.map { CHROMATIC_NOTES[it] }

        // Test every unique pitch class as potential root
        var bestCandidate: ReverseChordCandidate? = null

        for (rootPitch in uniquePitchClasses) {
            val rootName = CHROMATIC_NOTES[rootPitch]
            val intervals = uniquePitchClasses.map { (it - rootPitch + 12) % 12 }.toSet()

            val qualityAndRank = matchIntervalsToQuality(intervals)
            if (qualityAndRank != null) {
                val (quality, rank, intervalNames) = qualityAndRank
                if (bestCandidate == null || rank < bestCandidate.rank) {
                    val isInversion = bassPitchClass != rootPitch
                    val inversionText = if (!isInversion) "Root Position" else {
                        val bassInterval = (bassPitchClass - rootPitch + 12) % 12
                        when (bassInterval) {
                            3, 4 -> "1st Inversion (3rd in bass)"
                            7 -> "2nd Inversion (5th in bass)"
                            10, 11 -> "3rd Inversion (7th in bass)"
                            else -> "Slash Chord (/$bassNoteName)"
                        }
                    }
                    val qualString = if (quality == "Major") " Major" else quality
                    val displayName = if (isInversion) "$rootName$qualString/$bassNoteName" else "$rootName$qualString"
                    bestCandidate = ReverseChordCandidate(
                        name = displayName,
                        root = rootName,
                        quality = quality,
                        intervals = intervalNames,
                        inversion = inversionText,
                        rank = rank
                    )
                }
            }
        }

        return if (bestCandidate != null) {
            ReverseChordResult(
                name = bestCandidate.name,
                root = bestCandidate.root,
                quality = bestCandidate.quality,
                notes = uniqueNoteNames,
                intervals = bestCandidate.intervals,
                inversion = bestCandidate.inversion,
                bassNote = bassNoteName
            )
        } else {
            ReverseChordResult(
                name = "$bassNoteName (Unknown Harmony)",
                root = bassNoteName,
                quality = "Custom",
                notes = uniqueNoteNames,
                intervals = uniquePitchClasses.map { (it - bassPitchClass + 12) % 12 }.map { "int-$it" },
                inversion = "Root Position",
                bassNote = bassNoteName
            )
        }
    }

    private fun matchIntervalsToQuality(intervals: Set<Int>): Triple<String, Int, List<String>>? {
        // Intervals: 0=Root, 1=b2, 2=2, 3=b3, 4=3, 5=4, 6=b5, 7=5, 8=#5, 9=6, 10=b7, 11=7
        return when {
            // Major: 0, 4, 7
            intervals == setOf(0, 4, 7) -> Triple("Major", 1, listOf("1", "3", "5"))
            // Minor: 0, 3, 7
            intervals == setOf(0, 3, 7) -> Triple("m", 1, listOf("1", "b3", "5"))
            // Dominant 7: 0, 4, 7, 10
            intervals == setOf(0, 4, 7, 10) -> Triple("7", 2, listOf("1", "3", "5", "b7"))
            // Major 7: 0, 4, 7, 11
            intervals == setOf(0, 4, 7, 11) -> Triple("maj7", 2, listOf("1", "3", "5", "7"))
            // Minor 7: 0, 3, 7, 10
            intervals == setOf(0, 3, 7, 10) -> Triple("m7", 2, listOf("1", "b3", "5", "b7"))
            // sus4: 0, 5, 7
            intervals == setOf(0, 5, 7) -> Triple("sus4", 2, listOf("1", "4", "5"))
            // sus2: 0, 2, 7
            intervals == setOf(0, 2, 7) -> Triple("sus2", 2, listOf("1", "2", "5"))
            // Power chord 5: 0, 7
            intervals == setOf(0, 7) -> Triple("5", 2, listOf("1", "5"))
            // add9: 0, 2, 4, 7
            intervals == setOf(0, 2, 4, 7) -> Triple("add9", 3, listOf("1", "2/9", "3", "5"))
            // 9th: 0, 2, 4, 7, 10
            intervals == setOf(0, 2, 4, 7, 10) -> Triple("9", 3, listOf("1", "3", "5", "b7", "9"))
            // Diminished: 0, 3, 6
            intervals == setOf(0, 3, 6) -> Triple("dim", 3, listOf("1", "b3", "b5"))
            // Diminished 7: 0, 3, 6, 9
            intervals == setOf(0, 3, 6, 9) -> Triple("dim7", 3, listOf("1", "b3", "b5", "bb7"))
            // Augmented: 0, 4, 8
            intervals == setOf(0, 4, 8) -> Triple("aug", 3, listOf("1", "3", "#5"))
            // 6th: 0, 4, 7, 9
            intervals == setOf(0, 4, 7, 9) -> Triple("6", 3, listOf("1", "3", "5", "6"))
            // Minor 6: 0, 3, 7, 9
            intervals == setOf(0, 3, 7, 9) -> Triple("m6", 3, listOf("1", "b3", "5", "6"))
            // Dyad Major 3rd: 0, 4
            intervals == setOf(0, 4) -> Triple(" (no5)", 4, listOf("1", "3"))
            // Dyad Minor 3rd: 0, 3
            intervals == setOf(0, 3) -> Triple("m (no5)", 4, listOf("1", "b3"))
            else -> null
        }
    }

    private data class ReverseChordCandidate(
        val name: String,
        val root: String,
        val quality: String,
        val intervals: List<String>,
        val inversion: String,
        val rank: Int
    )

    data class ReverseChordResult(
        val name: String,
        val root: String,
        val quality: String,
        val notes: List<String>,
        val intervals: List<String>,
        val inversion: String,
        val bassNote: String
    )

    /**
     * Builds the comprehensive chord database.
     * Maps "Root Quality" and "ChordName" to list of ChordVoicings.
     */
    private fun buildChordDatabase(): Map<String, List<ChordVoicing>> {
        val map = mutableMapOf<String, MutableList<ChordVoicing>>()

        fun add(voicing: ChordVoicing) {
            val key1 = "${voicing.root} ${voicing.quality}"
            val key2 = voicing.chordName
            map.getOrPut(key1) { mutableListOf() }.add(voicing)
            if (key2 != key1) {
                map.getOrPut(key2) { mutableListOf() }.add(voicing)
            }
        }

        // ==================== C CHORDS ====================
        add(ChordVoicing("C", "C", "Major", "Easy", listOf(-1, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0), 1, emptyList(), "Open Position", listOf("1", "3", "5")))
        add(ChordVoicing("C", "C", "Major", "Medium", listOf(8, 10, 10, 9, 8, 8), listOf(1, 3, 4, 2, 1, 1), 8, listOf(8), "Barre (6th string)", listOf("1", "5", "1", "3", "5", "1")))
        add(ChordVoicing("C", "C", "Major", "Medium", listOf(-1, 3, 5, 5, 5, 3), listOf(0, 1, 2, 3, 4, 1), 3, listOf(3), "Barre (5th string)", listOf("1", "5", "1", "3", "5")))

        add(ChordVoicing("Cm", "C", "Minor", "Medium", listOf(-1, 3, 5, 5, 4, 3), listOf(0, 1, 3, 4, 2, 1), 3, listOf(3), "Barre (5th string)", listOf("1", "5", "1", "b3", "5")))
        add(ChordVoicing("Cm", "C", "Minor", "Medium", listOf(8, 10, 10, 8, 8, 8), listOf(1, 3, 4, 1, 1, 1), 8, listOf(8), "Barre (6th string)", listOf("1", "5", "1", "b3", "5", "1")))

        add(ChordVoicing("C7", "C", "7", "Easy", listOf(-1, 3, 2, 3, 1, 0), listOf(0, 3, 2, 4, 1, 0), 1, emptyList(), "Open Dominant 7th", listOf("1", "3", "b7", "3", "5")))
        add(ChordVoicing("Cmaj7", "C", "maj7", "Easy", listOf(-1, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0), 1, emptyList(), "Open Major 7th", listOf("1", "3", "5", "7", "3")))
        add(ChordVoicing("Cm7", "C", "m7", "Medium", listOf(-1, 3, 5, 3, 4, 3), listOf(0, 1, 3, 1, 2, 1), 3, listOf(3), "Barre Minor 7th", listOf("1", "5", "b7", "b3", "5")))
        add(ChordVoicing("Csus4", "C", "sus4", "Easy", listOf(-1, 3, 3, 0, 1, 1), listOf(0, 3, 4, 0, 1, 1), 1, listOf(1), "Open sus4", listOf("1", "4", "5", "1", "4")))
        add(ChordVoicing("Csus2", "C", "sus2", "Easy", listOf(-1, 3, 0, 0, 1, 3), listOf(0, 2, 0, 0, 1, 4), 1, emptyList(), "Open sus2", listOf("1", "5", "2", "1", "5")))
        add(ChordVoicing("Cadd9", "C", "add9", "Easy", listOf(-1, 3, 2, 0, 3, 0), listOf(0, 2, 1, 0, 3, 0), 1, emptyList(), "Open add9", listOf("1", "3", "5", "9", "3")))

        // ==================== D CHORDS ====================
        add(ChordVoicing("D", "D", "Major", "Easy", listOf(-1, -1, 0, 2, 3, 2), listOf(0, 0, 0, 1, 3, 2), 1, emptyList(), "Open Position", listOf("1", "5", "1", "3")))
        add(ChordVoicing("D", "D", "Major", "Medium", listOf(-1, 5, 7, 7, 7, 5), listOf(0, 1, 2, 3, 4, 1), 5, listOf(5), "Barre (5th string)", listOf("1", "5", "1", "3", "5")))
        add(ChordVoicing("Dm", "D", "Minor", "Easy", listOf(-1, -1, 0, 2, 3, 1), listOf(0, 0, 0, 2, 3, 1), 1, emptyList(), "Open Position", listOf("1", "5", "1", "b3")))
        add(ChordVoicing("D7", "D", "7", "Easy", listOf(-1, -1, 0, 2, 1, 2), listOf(0, 0, 0, 2, 1, 3), 1, emptyList(), "Open 7th", listOf("1", "5", "b7", "3")))
        add(ChordVoicing("Dmaj7", "D", "maj7", "Easy", listOf(-1, -1, 0, 2, 2, 2), listOf(0, 0, 0, 1, 2, 3), 1, emptyList(), "Open Major 7th", listOf("1", "5", "7", "3")))
        add(ChordVoicing("Dm7", "D", "m7", "Easy", listOf(-1, -1, 0, 2, 1, 1), listOf(0, 0, 0, 2, 1, 1), 1, listOf(1), "Open Minor 7th", listOf("1", "5", "b7", "b3")))
        add(ChordVoicing("Dsus4", "D", "sus4", "Easy", listOf(-1, -1, 0, 2, 3, 3), listOf(0, 0, 0, 1, 2, 3), 1, emptyList(), "Open sus4", listOf("1", "5", "1", "4")))
        add(ChordVoicing("Dsus2", "D", "sus2", "Easy", listOf(-1, -1, 0, 2, 3, 0), listOf(0, 0, 0, 1, 2, 0), 1, emptyList(), "Open sus2", listOf("1", "5", "1", "2")))

        // ==================== E CHORDS ====================
        add(ChordVoicing("E", "E", "Major", "Easy", listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0), 1, emptyList(), "Open Position", listOf("1", "5", "1", "3", "5", "1")))
        add(ChordVoicing("Em", "E", "Minor", "Easy", listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0), 1, emptyList(), "Open Position", listOf("1", "5", "1", "b3", "5", "1")))
        add(ChordVoicing("E7", "E", "7", "Easy", listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0), 1, emptyList(), "Open 7th", listOf("1", "5", "b7", "3", "5", "1")))
        add(ChordVoicing("Emaj7", "E", "maj7", "Easy", listOf(0, 2, 1, 1, 0, 0), listOf(0, 3, 1, 2, 0, 0), 1, emptyList(), "Open Major 7th", listOf("1", "5", "7", "3", "5", "1")))
        add(ChordVoicing("Em7", "E", "m7", "Easy", listOf(0, 2, 2, 0, 3, 0), listOf(0, 1, 2, 0, 3, 0), 1, emptyList(), "Open Minor 7th", listOf("1", "5", "1", "b3", "b7", "1")))
        add(ChordVoicing("Esus4", "E", "sus4", "Easy", listOf(0, 2, 2, 2, 0, 0), listOf(0, 2, 3, 4, 0, 0), 1, emptyList(), "Open sus4", listOf("1", "5", "1", "4", "5", "1")))

        // ==================== F CHORDS ====================
        add(ChordVoicing("F", "F", "Major", "Medium", listOf(1, 3, 3, 2, 1, 1), listOf(1, 3, 4, 2, 1, 1), 1, listOf(1), "Full Barre", listOf("1", "5", "1", "3", "5", "1")))
        add(ChordVoicing("F", "F", "Major", "Easy", listOf(-1, -1, 3, 2, 1, 1), listOf(0, 0, 3, 2, 1, 1), 1, listOf(1), "Small Barre", listOf("1", "3", "5", "1")))
        add(ChordVoicing("Fm", "F", "Minor", "Medium", listOf(1, 3, 3, 1, 1, 1), listOf(1, 3, 4, 1, 1, 1), 1, listOf(1), "Full Barre", listOf("1", "5", "1", "b3", "5", "1")))
        add(ChordVoicing("F7", "F", "7", "Medium", listOf(1, 3, 1, 2, 1, 1), listOf(1, 3, 1, 2, 1, 1), 1, listOf(1), "Barre 7th", listOf("1", "5", "b7", "3", "5", "1")))
        add(ChordVoicing("Fmaj7", "F", "maj7", "Easy", listOf(-1, -1, 3, 2, 1, 0), listOf(0, 0, 3, 2, 1, 0), 1, emptyList(), "Open Major 7th", listOf("1", "3", "5", "7")))
        add(ChordVoicing("F#m", "F#", "Minor", "Medium", listOf(2, 4, 4, 2, 2, 2), listOf(1, 3, 4, 1, 1, 1), 2, listOf(2), "Barre Position", listOf("1", "5", "1", "b3", "5", "1")))
        add(ChordVoicing("F#m7", "F#", "m7", "Medium", listOf(2, 4, 2, 2, 2, 2), listOf(1, 3, 1, 1, 1, 1), 2, listOf(2), "Barre Minor 7th", listOf("1", "5", "b7", "b3", "5", "1")))

        // ==================== G CHORDS ====================
        add(ChordVoicing("G", "G", "Major", "Easy", listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3), 1, emptyList(), "Open 3-Finger", listOf("1", "3", "5", "1", "3", "1")))
        add(ChordVoicing("G", "G", "Major", "Easy", listOf(3, 2, 0, 0, 3, 3), listOf(2, 1, 0, 0, 3, 4), 1, emptyList(), "Open 4-Finger (Rock G)", listOf("1", "3", "5", "1", "5", "1")))
        add(ChordVoicing("G", "G", "Major", "Medium", listOf(3, 5, 5, 4, 3, 3), listOf(1, 3, 4, 2, 1, 1), 3, listOf(3), "Barre Position", listOf("1", "5", "1", "3", "5", "1")))
        add(ChordVoicing("Gm", "G", "Minor", "Medium", listOf(3, 5, 5, 3, 3, 3), listOf(1, 3, 4, 1, 1, 1), 3, listOf(3), "Barre Position", listOf("1", "5", "1", "b3", "5", "1")))
        add(ChordVoicing("G7", "G", "7", "Easy", listOf(3, 2, 0, 0, 0, 1), listOf(3, 2, 0, 0, 0, 1), 1, emptyList(), "Open 7th", listOf("1", "3", "5", "1", "3", "b7")))
        add(ChordVoicing("Gmaj7", "G", "maj7", "Medium", listOf(3, 2, 0, 0, 0, 2), listOf(2, 1, 0, 0, 0, 3), 1, emptyList(), "Open Major 7th", listOf("1", "3", "5", "1", "3", "7")))
        add(ChordVoicing("Gsus4", "G", "sus4", "Easy", listOf(3, 3, 0, 0, 1, 3), listOf(2, 3, 0, 0, 1, 4), 1, emptyList(), "Open sus4", listOf("1", "4", "5", "1", "1", "1")))

        // ==================== A CHORDS ====================
        add(ChordVoicing("A", "A", "Major", "Easy", listOf(-1, 0, 2, 2, 2, 0), listOf(0, 0, 1, 2, 3, 0), 1, emptyList(), "Open Position", listOf("1", "5", "1", "3", "5")))
        add(ChordVoicing("Am", "A", "Minor", "Easy", listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), 1, emptyList(), "Open Position", listOf("1", "5", "1", "b3", "5")))
        add(ChordVoicing("A7", "A", "7", "Easy", listOf(-1, 0, 2, 0, 2, 0), listOf(0, 0, 2, 0, 3, 0), 1, emptyList(), "Open 7th", listOf("1", "5", "b7", "3", "5")))
        add(ChordVoicing("Amaj7", "A", "maj7", "Easy", listOf(-1, 0, 2, 1, 2, 0), listOf(0, 0, 2, 1, 3, 0), 1, emptyList(), "Open Major 7th", listOf("1", "5", "7", "3", "5")))
        add(ChordVoicing("Am7", "A", "m7", "Easy", listOf(-1, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0), 1, emptyList(), "Open Minor 7th", listOf("1", "5", "b7", "b3", "5")))
        add(ChordVoicing("Asus4", "A", "sus4", "Easy", listOf(-1, 0, 2, 2, 3, 0), listOf(0, 0, 1, 2, 3, 0), 1, emptyList(), "Open sus4", listOf("1", "5", "1", "4", "5")))
        add(ChordVoicing("Asus2", "A", "sus2", "Easy", listOf(-1, 0, 2, 2, 0, 0), listOf(0, 0, 1, 2, 0, 0), 1, emptyList(), "Open sus2", listOf("1", "5", "1", "2", "5")))

        // ==================== B & Bb CHORDS ====================
        add(ChordVoicing("B", "B", "Major", "Medium", listOf(-1, 2, 4, 4, 4, 2), listOf(0, 1, 2, 3, 4, 1), 2, listOf(2), "Barre Position", listOf("1", "5", "1", "3", "5")))
        add(ChordVoicing("Bm", "B", "Minor", "Medium", listOf(-1, 2, 4, 4, 3, 2), listOf(0, 1, 3, 4, 2, 1), 2, listOf(2), "Barre Position", listOf("1", "5", "1", "b3", "5")))
        add(ChordVoicing("B7", "B", "7", "Easy", listOf(-1, 2, 1, 2, 0, 2), listOf(0, 2, 1, 3, 0, 4), 1, emptyList(), "Open 7th", listOf("1", "3", "b7", "b3", "5")))
        add(ChordVoicing("Bb", "Bb", "Major", "Medium", listOf(-1, 1, 3, 3, 3, 1), listOf(0, 1, 2, 3, 4, 1), 1, listOf(1), "Barre Position", listOf("1", "5", "1", "3", "5")))
        add(ChordVoicing("Bbm", "Bb", "Minor", "Medium", listOf(-1, 1, 3, 3, 2, 1), listOf(0, 1, 3, 4, 2, 1), 1, listOf(1), "Barre Position", listOf("1", "5", "1", "b3", "5")))

        // Generate systematic CAGED movable chord shapes for all 12 roots across all remaining qualities
        generateSystematicChords(map)

        return map
    }

    private fun generateSystematicChords(map: MutableMap<String, MutableList<ChordVoicing>>) {
        // Movable E-shape barre base: root on 6th string
        // Frets: E-string fret = root pitch offset
        val eShapeRootFrets = mapOf(
            "F" to 1, "F#" to 2, "G" to 3, "G#" to 4, "A" to 5, "Bb" to 6, "B" to 7, "C" to 8, "C#" to 9, "D" to 10, "D#" to 11, "E" to 12
        )
        // Movable A-shape barre base: root on 5th string
        val aShapeRootFrets = mapOf(
            "Bb" to 1, "B" to 2, "C" to 3, "C#" to 4, "D" to 5, "D#" to 6, "E" to 7, "F" to 8, "F#" to 9, "G" to 10, "G#" to 11, "A" to 12
        )

        for (root in ROOTS) {
            val eFret = eShapeRootFrets[root] ?: 1
            val aFret = aShapeRootFrets[root] ?: 1

            for (quality in QUALITIES) {
                val key = "$root $quality"
                val existing = map.getOrPut(key) { mutableListOf() }
                if (existing.size < 3) {
                    val voicings = createMovableVoicings(root, quality, eFret, aFret)
                    for (v in voicings) {
                        if (existing.none { it.frets == v.frets }) {
                            existing.add(v)
                        }
                    }
                }
            }
        }
    }

    private fun createMovableVoicings(root: String, quality: String, eFret: Int, aFret: Int): List<ChordVoicing> {
        val result = mutableListOf<ChordVoicing>()
        val chordName = "$root${if (quality == "Major") "" else quality}"

        // E-shape barre voicing
        val (eOffsets, eFingers, eBarre) = when (quality) {
            "Major" -> Triple(listOf(0, 2, 2, 1, 0, 0), listOf(1, 3, 4, 2, 1, 1), true)
            "Minor", "m" -> Triple(listOf(0, 2, 2, 0, 0, 0), listOf(1, 3, 4, 1, 1, 1), true)
            "7" -> Triple(listOf(0, 2, 0, 1, 0, 0), listOf(1, 3, 1, 2, 1, 1), true)
            "m7" -> Triple(listOf(0, 2, 0, 0, 0, 0), listOf(1, 3, 1, 1, 1, 1), true)
            "maj7" -> Triple(listOf(0, 2, 1, 1, 0, 0), listOf(1, 3, 2, 2, 1, 1), true)
            "sus4" -> Triple(listOf(0, 2, 2, 2, 0, 0), listOf(1, 3, 4, 4, 1, 1), true)
            "dim" -> Triple(listOf(0, 1, 2, 0, -1, -1), listOf(1, 2, 3, 0, 0, 0), false)
            "5" -> Triple(listOf(0, 2, 2, -1, -1, -1), listOf(1, 3, 4, 0, 0, 0), false)
            else -> Triple(listOf(0, 2, 2, 1, 0, 0), listOf(1, 3, 4, 2, 1, 1), true)
        }
        val eFrets = eOffsets.map { if (it < 0) -1 else eFret + it }
        result.add(
            ChordVoicing(
                chordName = chordName,
                root = root,
                quality = quality,
                difficulty = if (eFret <= 3) "Medium" else "Advanced",
                frets = eFrets,
                fingers = eFingers,
                baseFret = eFret,
                barres = if (eBarre) listOf(eFret) else emptyList(),
                voicingName = "Barre (Root 6th)"
            )
        )

        // A-shape barre voicing
        if (aFret <= 10) {
            val (aOffsets, aFingers, aBarre) = when (quality) {
                "Major" -> Triple(listOf(-1, 0, 2, 2, 2, 0), listOf(0, 1, 2, 3, 4, 1), true)
                "Minor", "m" -> Triple(listOf(-1, 0, 2, 2, 1, 0), listOf(0, 1, 3, 4, 2, 1), true)
                "7" -> Triple(listOf(-1, 0, 2, 0, 2, 0), listOf(0, 1, 3, 1, 4, 1), true)
                "m7" -> Triple(listOf(-1, 0, 2, 0, 1, 0), listOf(0, 1, 3, 1, 2, 1), true)
                "maj7" -> Triple(listOf(-1, 0, 2, 1, 2, 0), listOf(0, 1, 3, 2, 4, 1), true)
                "sus4" -> Triple(listOf(-1, 0, 2, 2, 3, 0), listOf(0, 1, 2, 3, 4, 1), true)
                "5" -> Triple(listOf(-1, 0, 2, 2, -1, -1), listOf(0, 1, 3, 4, 0, 0), false)
                else -> Triple(listOf(-1, 0, 2, 2, 2, 0), listOf(0, 1, 2, 3, 4, 1), true)
            }
            val aFrets = aOffsets.map { if (it < 0) -1 else aFret + it }
            result.add(
                ChordVoicing(
                    chordName = chordName,
                    root = root,
                    quality = quality,
                    difficulty = "Medium",
                    frets = aFrets,
                    fingers = aFingers,
                    baseFret = aFret,
                    barres = if (aBarre) listOf(aFret) else emptyList(),
                    voicingName = "Barre (Root 5th)"
                )
            )
        }

        return result
    }

    private fun generateDynamicVoicings(chordName: String): List<ChordVoicing> {
        val parsed = parseChordName(chordName) ?: Pair("C", "Major")
        val root = parsed.first
        val quality = parsed.second
        val eShapeRootFrets = mapOf(
            "F" to 1, "F#" to 2, "G" to 3, "G#" to 4, "A" to 5, "Bb" to 6, "B" to 7, "C" to 8, "C#" to 9, "D" to 10, "D#" to 11, "E" to 12
        )
        val aShapeRootFrets = mapOf(
            "Bb" to 1, "B" to 2, "C" to 3, "C#" to 4, "D" to 5, "D#" to 6, "E" to 7, "F" to 8, "F#" to 9, "G" to 10, "G#" to 11, "A" to 12
        )
        return createMovableVoicings(root, quality, eShapeRootFrets[root] ?: 1, aShapeRootFrets[root] ?: 1)
    }
}

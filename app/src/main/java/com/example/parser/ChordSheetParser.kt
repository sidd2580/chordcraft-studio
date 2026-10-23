package com.example.parser

import com.example.data.model.LyricSegment
import com.example.data.model.ParsedLine
import com.example.data.model.ParsedSection
import com.example.data.model.ParsedSong

/**
 * Intelligent parser that converts web chord sheets (both inline brackets and two-line chord/lyric formats)
 * into structured stage-ready data with chords strictly aligned above syllables.
 */
object ChordSheetParser {

    private val CHORD_REGEX = Regex("""\b[A-G][b#]?(?:m|maj|min|dim|aug|sus[24]?|[0-9]|add[0-9]|M[0-9])*(?:/[A-G][b#]?)?\b""")
    private val BRACKET_CHORD_REGEX = Regex("""\[([A-G][b#]?[^\]]*)\]""")
    private val SECTION_HEADER_REGEX = Regex("""^\[?(Verse(?:\s*\d+)?|Chorus(?:\s*\d+)?|Bridge|Intro|Outro|Solo|Pre-Chorus|Hook|Interlude)\]?:?$""", RegexOption.IGNORE_CASE)

    /**
     * Parses raw text into a ParsedSong structure.
     */
    fun parse(
        title: String,
        artist: String,
        rawText: String,
        defaultKey: String = "C",
        defaultCapo: Int = 0
    ): ParsedSong {
        // Step 1: Normalize any 2-line chord/lyric format to inline brackets
        val normalizedText = normalizeToBracketFormat(rawText)

        val sections = mutableListOf<ParsedSection>()
        var currentSectionHeader: String? = null
        var currentLines = mutableListOf<ParsedLine>()

        val rawLines = normalizedText.lines()

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (currentLines.isNotEmpty()) {
                    sections.add(ParsedSection(currentSectionHeader, currentLines))
                    currentLines = mutableListOf()
                    currentSectionHeader = null
                }
                continue
            }

            // Check if section header
            val headerMatch = SECTION_HEADER_REGEX.find(trimmed)
            if (headerMatch != null) {
                if (currentLines.isNotEmpty()) {
                    sections.add(ParsedSection(currentSectionHeader, currentLines))
                    currentLines = mutableListOf()
                }
                currentSectionHeader = headerMatch.groupValues[1]
                continue
            }

            // Parse bracketed line into LyricSegments
            val parsedLine = parseBracketedLine(line)
            currentLines.add(parsedLine)
        }

        if (currentLines.isNotEmpty()) {
            sections.add(ParsedSection(currentSectionHeader, currentLines))
        }

        return ParsedSong(
            title = title,
            artist = artist,
            keySignature = defaultKey,
            capo = defaultCapo,
            sections = sections
        )
    }

    /**
     * Converts a single line with bracketed chords into a list of LyricSegments.
     * e.g., "[C]Wise men [G]say only [Am]fools rush in"
     */
    fun parseBracketedLine(line: String): ParsedLine {
        val segments = mutableListOf<LyricSegment>()
        var cursor = 0
        val matches = BRACKET_CHORD_REGEX.findAll(line).toList()

        if (matches.isEmpty()) {
            return ParsedLine(listOf(LyricSegment(null, line)))
        }

        for (match in matches) {
            // Text before this chord
            if (match.range.first > cursor) {
                val textBefore = line.substring(cursor, match.range.first)
                if (segments.isEmpty()) {
                    segments.add(LyricSegment(null, textBefore))
                } else {
                    val last = segments.removeAt(segments.lastIndex)
                    segments.add(last.copy(lyric = last.lyric + textBefore))
                }
            }

            val chord = match.groupValues[1]
            // Lyric after chord until next chord or end of line
            val nextCursor = match.range.last + 1
            cursor = nextCursor
            segments.add(LyricSegment(chord, ""))
        }

        // Add remaining trailing lyrics
        if (cursor < line.length) {
            val trailing = line.substring(cursor)
            if (segments.isNotEmpty()) {
                val last = segments.removeAt(segments.lastIndex)
                segments.add(last.copy(lyric = last.lyric + trailing))
            } else {
                segments.add(LyricSegment(null, trailing))
            }
        }

        return ParsedLine(segments)
    }

    /**
     * Detects if raw text is in 2-line format (chords on line 1, lyrics on line 2),
     * and converts it into standard [Chord] bracket notation.
     */
    fun normalizeToBracketFormat(rawText: String): String {
        // If already has plenty of bracketed chords, return as is
        if (BRACKET_CHORD_REGEX.findAll(rawText).count() > 3) {
            return rawText
        }

        val lines = rawText.lines()
        val result = StringBuilder()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val nextLine = lines.getOrNull(i + 1)

            if (isChordLine(line)) {
                if (nextLine != null && !isChordLine(nextLine) && nextLine.isNotBlank()) {
                    // Merge chord line with next lyrics line
                    result.append(mergeChordAndLyricLines(line, nextLine)).append("\n")
                    i += 2
                    continue
                } else {
                    // Chords only line (e.g. intro chords "C  G  Am  F")
                    val chordsOnly = line.split(Regex("""\s+""")).filter { it.isNotBlank() }
                        .joinToString(" ") { "[$it]" }
                    result.append(chordsOnly).append("\n")
                    i++
                    continue
                }
            } else {
                result.append(line).append("\n")
                i++
            }
        }

        return result.toString().trimEnd()
    }

    private fun isChordLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val words = trimmed.split(Regex("""\s+"""))
        val chordWords = words.count { CHORD_REGEX.matches(it) }
        return chordWords >= 1 && (chordWords.toDouble() / words.size) >= 0.65
    }

    private fun mergeChordAndLyricLines(chordLine: String, lyricLine: String): String {
        val chordsWithPos = mutableListOf<Pair<Int, String>>()
        CHORD_REGEX.findAll(chordLine).forEach { match ->
            chordsWithPos.add(Pair(match.range.first, match.value))
        }

        chordsWithPos.sortBy { it.first }

        val sb = StringBuilder()
        var lyricIdx = 0

        for ((chordPos, chord) in chordsWithPos) {
            while (lyricIdx < chordPos && lyricIdx < lyricLine.length) {
                sb.append(lyricLine[lyricIdx])
                lyricIdx++
            }
            sb.append("[$chord]")
        }

        if (lyricIdx < lyricLine.length) {
            sb.append(lyricLine.substring(lyricIdx))
        }

        return sb.toString()
    }

    /**
     * Extracts metadata such as Key, BPM, and Tuning if included in the sheet.
     */
    fun extractMetadata(text: String): Triple<String, Int, String> {
        var key = "C"
        var bpm = 120
        var tuning = "Standard (E A D G B e)"

        val keyMatch = Regex("""(?:Key|Key Signature):\s*([A-G][b#]?(?:m)?)""", RegexOption.IGNORE_CASE).find(text)
        if (keyMatch != null) key = keyMatch.groupValues[1]

        val bpmMatch = Regex("""(?:BPM|Tempo):\s*(\d+)""", RegexOption.IGNORE_CASE).find(text)
        if (bpmMatch != null) bpm = bpmMatch.groupValues[1].toIntOrNull() ?: 120

        val tuningMatch = Regex("""(?:Tuning):\s*([^\n\r]+)""", RegexOption.IGNORE_CASE).find(text)
        if (tuningMatch != null) tuning = tuningMatch.groupValues[1].trim()

        return Triple(key, bpm, tuning)
    }
}

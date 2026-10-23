package com.example

import com.example.data.repository.ChordDictionary
import com.example.parser.ChordSheetParser
import com.example.parser.Transposer
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTransposeBasicChords() {
        assertEquals("D", Transposer.transposeChord("C", 2))
        assertEquals("B", Transposer.transposeChord("C", -1))
        assertEquals("F#m", Transposer.transposeChord("Em", 2))
        assertEquals("Am7", Transposer.transposeChord("Gm7", 2))
    }

    @Test
    fun testTransposeSlashChords() {
        assertEquals("E/G#", Transposer.transposeChord("D/F#", 2))
    }

    @Test
    fun testTransposeLyrics() {
        val raw = "[C]Wise men [G]say only [Am]fools rush [F]in"
        val transposed = Transposer.transposeLyrics(raw, 2)
        assertEquals("[D]Wise men [A]say only [Bm]fools rush [G]in", transposed)
    }

    @Test
    fun testCapoCalculator() {
        // Concert Key G with Capo 2 -> play in F shapes
        val shape = Transposer.calculateCapoShape("G", 2)
        assertEquals("F", shape)

        // Shape C with Capo 2 -> sounds in D
        val concert = Transposer.calculateConcertPitch("C", 2)
        assertEquals("D", concert)
    }

    @Test
    fun testChordSheetParser() {
        val raw = """
[Verse 1]
[C]Wise men [G]say
        """.trimIndent()

        val parsed = ChordSheetParser.parse("Can't Help Falling in Love", "Elvis", raw)
        assertEquals(1, parsed.sections.size)
        assertEquals("Verse 1", parsed.sections[0].sectionHeader)
        assertEquals(1, parsed.sections[0].lines.size)
        val line = parsed.sections[0].lines[0]
        assertEquals("C", line.segments[0].chord)
        assertEquals("Wise men ", line.segments[0].lyric)
        assertEquals("G", line.segments[1].chord)
        assertEquals("say", line.segments[1].lyric)
    }

    @Test
    fun testReverseChordFinderCMajor() {
        // C Major: x 3 2 0 1 0
        val result = ChordDictionary.identifyChord(listOf(-1, 3, 2, 0, 1, 0))
        assertEquals("C Major", result.name)
        assertEquals("C", result.root)
        assertEquals("Root Position", result.inversion)
        assertTrue(result.notes.contains("C"))
        assertTrue(result.notes.contains("E"))
        assertTrue(result.notes.contains("G"))
    }

    @Test
    fun testReverseChordFinderGInversion() {
        // C/E (1st inversion): 0 3 2 0 1 0 -> E is in the bass
        val result = ChordDictionary.identifyChord(listOf(0, 3, 2, 0, 1, 0))
        assertTrue(result.name.contains("C") && result.name.contains("E"))
    }
}

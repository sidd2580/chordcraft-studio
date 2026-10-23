package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Realistic acoustic guitar audio synthesizer using Android AudioTrack
 * and Karplus-Strong / harmonic overtone modeling.
 * Strums complete chords with realistic string sweeps or individual notes.
 */
object GuitarAudioSynthesizer {

    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Strums a chord given a list of 6 string frets.
     * e.g. [-1, 3, 2, 0, 1, 0] for C Major.
     * @param frets Frets on strings 6 to 1 (low E to high e).
     * @param downStrum true for down-strum (6 to 1), false for up-strum (1 to 6).
     * @param strumDurationMs sweep time across strings (e.g. 120ms).
     */
    fun strumChord(frets: List<Int>, downStrum: Boolean = true, strumDurationMs: Long = 120L) {
        scope.launch {
            val openNotes = listOf(40, 45, 50, 55, 59, 64) // E2, A2, D3, G3, B3, E4
            val activeStrings = mutableListOf<Pair<Int, Int>>() // String index, MIDI note

            val stringIndices = if (downStrum) (0..5).toList() else (5 downTo 0).toList()
            for (i in stringIndices) {
                val fret = frets.getOrNull(i) ?: -1
                if (fret >= 0) {
                    val midi = openNotes[i] + fret
                    activeStrings.add(Pair(i, midi))
                }
            }

            if (activeStrings.isEmpty()) return@launch

            val totalDurationSec = 2.5f
            val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
            val mixedBuffer = FloatArray(totalSamples)

            val delayBetweenStringsSec = (strumDurationMs.toFloat() / 1000f) / activeStrings.size

            activeStrings.forEachIndexed { index, (_, midi) ->
                val startSample = (index * delayBetweenStringsSec * SAMPLE_RATE).toInt()
                val freq = midiToFreq(midi)
                val decayRate = 3.0f + (midi - 40) * 0.05f // higher notes decay faster

                val stringSamples = totalSamples - startSample
                for (s in 0 until stringSamples) {
                    val t = s.toFloat() / SAMPLE_RATE
                    val envelope = exp(-decayRate * t)

                    // Acoustic guitar harmonics (Fundamental + 2nd, 3rd, 4th, 5th harmonics)
                    val sample = (
                        0.50f * sin(2.0 * PI * freq * t) +
                        0.25f * sin(2.0 * PI * (freq * 2) * t) +
                        0.15f * sin(2.0 * PI * (freq * 3) * t) +
                        0.07f * sin(2.0 * PI * (freq * 4) * t) +
                        0.03f * sin(2.0 * PI * (freq * 5) * t)
                    ) * envelope

                    mixedBuffer[startSample + s] += sample.toFloat()
                }
            }

            // Normalize and convert to 16-bit PCM
            playPcmBuffer(mixedBuffer)
        }
    }

    /**
     * Plays a single plucked guitar string note.
     */
    fun playPluckedNote(midiNote: Int, durationSec: Float = 2.0f) {
        scope.launch {
            val totalSamples = (SAMPLE_RATE * durationSec).toInt()
            val buffer = FloatArray(totalSamples)
            val freq = midiToFreq(midiNote)
            val decay = 3.5f

            for (s in 0 until totalSamples) {
                val t = s.toFloat() / SAMPLE_RATE
                val envelope = exp(-decay * t)
                val sample = (
                    0.55f * sin(2.0 * PI * freq * t) +
                    0.25f * sin(2.0 * PI * (freq * 2) * t) +
                    0.15f * sin(2.0 * PI * (freq * 3) * t) +
                    0.05f * sin(2.0 * PI * (freq * 4) * t)
                ) * envelope
                buffer[s] = sample.toFloat()
            }
            playPcmBuffer(buffer)
        }
    }

    private fun midiToFreq(midi: Int): Double {
        return 440.0 * 2.0.pow((midi - 69.0) / 12.0)
    }

    private fun playPcmBuffer(floatBuffer: FloatArray) {
        // Find peak for normalization to avoid clipping
        var maxVal = 0.001f
        for (v in floatBuffer) {
            val abs = kotlin.math.abs(v)
            if (abs > maxVal) maxVal = abs
        }

        val pcmShorts = ShortArray(floatBuffer.size)
        val gain = if (maxVal > 0.8f) (0.8f / maxVal) else 1.0f
        for (i in floatBuffer.indices) {
            val scaled = (floatBuffer[i] * gain * 32767.0f).coerceIn(-32768.0f, 32767.0f)
            pcmShorts[i] = scaled.toInt().toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmShorts.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmShorts, 0, pcmShorts.size)
            track.play()
            // Clean up after playback finished
            scope.launch {
                kotlinx.coroutines.delay((floatBuffer.size * 1000L / SAMPLE_RATE) + 100)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // AudioTrack failure fallback
        }
    }
}

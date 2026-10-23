package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Controller for audio reference playback, speed adjustment (0.5x to 1.5x),
 * A-B Looper for practicing difficult guitar passages, and interactive metronome clicks.
 */
class AudioController {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null
    private var metronomeJob: Job? = null

    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0L,
        val totalDurationMs: Long = 180000L, // 3 minutes default backing track
        val speed: Float = 1.0f,
        val isLoopEnabled: Boolean = false,
        val loopStartMs: Long = 0L,
        val loopEndMs: Long = 60000L,
        val isMetronomeActive: Boolean = false,
        val bpm: Int = 120
    )

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        playbackJob?.cancel()
        _state.value = _state.value.copy(isPlaying = true)

        playbackJob = scope.launch {
            val stepMs = 50L
            while (isActive && _state.value.isPlaying) {
                delay(stepMs)
                val current = _state.value.currentPositionMs
                val speed = _state.value.speed
                val next = current + (stepMs * speed).toLong()

                val s = _state.value
                if (s.isLoopEnabled && next >= s.loopEndMs) {
                    _state.value = s.copy(currentPositionMs = s.loopStartMs)
                } else if (next >= s.totalDurationMs) {
                    _state.value = s.copy(currentPositionMs = 0L, isPlaying = false)
                    break
                } else {
                    _state.value = s.copy(currentPositionMs = next)
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _state.value.totalDurationMs)
        _state.value = _state.value.copy(currentPositionMs = bounded)
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 1.5f)
        _state.value = _state.value.copy(speed = clamped)
    }

    fun setLoopA() {
        val current = _state.value.currentPositionMs
        val currentB = _state.value.loopEndMs
        val newB = if (current >= currentB) (current + 5000L).coerceAtMost(_state.value.totalDurationMs) else currentB
        _state.value = _state.value.copy(
            loopStartMs = current,
            loopEndMs = newB,
            isLoopEnabled = true
        )
    }

    fun setLoopB() {
        val current = _state.value.currentPositionMs
        val currentA = _state.value.loopStartMs
        val newA = if (current <= currentA) (current - 5000L).coerceAtLeast(0L) else currentA
        _state.value = _state.value.copy(
            loopStartMs = newA,
            loopEndMs = current,
            isLoopEnabled = true
        )
    }

    fun toggleLoop() {
        _state.value = _state.value.copy(isLoopEnabled = !_state.value.isLoopEnabled)
    }

    fun clearLoop() {
        _state.value = _state.value.copy(isLoopEnabled = false, loopStartMs = 0L, loopEndMs = _state.value.totalDurationMs)
    }

    fun setBpm(bpm: Int) {
        _state.value = _state.value.copy(bpm = bpm.coerceIn(40, 240))
        if (_state.value.isMetronomeActive) {
            startMetronome()
        }
    }

    fun toggleMetronome() {
        if (_state.value.isMetronomeActive) {
            metronomeJob?.cancel()
            _state.value = _state.value.copy(isMetronomeActive = false)
        } else {
            _state.value = _state.value.copy(isMetronomeActive = true)
            startMetronome()
        }
    }

    private fun startMetronome() {
        metronomeJob?.cancel()
        metronomeJob = scope.launch(Dispatchers.Default) {
            var beat = 0
            while (isActive && _state.value.isMetronomeActive) {
                val intervalMs = (60000.0 / _state.value.bpm).toLong()
                playClickSound(isHigh = (beat % 4 == 0))
                beat++
                delay(intervalMs)
            }
        }
    }

    private fun playClickSound(isHigh: Boolean) {
        val sampleRate = 44100
        val durationSec = 0.04f
        val numSamples = (sampleRate * durationSec).toInt()
        val freq = if (isHigh) 1800.0 else 1200.0
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-80.0f * t)
            val sample = (sin(2.0 * PI * freq * t) * env * 24000.0).toInt().toShort()
            buffer[i] = sample
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                delay(60)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun release() {
        playbackJob?.cancel()
        metronomeJob?.cancel()
        scope.cancel()
    }
}

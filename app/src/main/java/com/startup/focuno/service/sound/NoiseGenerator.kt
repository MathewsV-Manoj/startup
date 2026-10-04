package com.startup.focuno.service.sound

import com.startup.focuno.domain.model.FocusSound
import java.util.SplittableRandom
import kotlin.math.PI
import kotlin.math.sin

/**
 * Makes calm noise one sample at a time, in the range -1..1.
 *
 * - White: every frequency equally loud (bright, like a fan).
 * - Pink: Paul Kellet's filter, softer highs (like steady rain).
 * - Brown: a gentle random walk, deep and soft (like a distant waterfall).
 * - Waves: brown noise that swells and fades every few seconds.
 */
class NoiseGenerator(
    private val sound: FocusSound,
    private val sampleRate: Int,
    seed: Long = System.nanoTime(),
) {
    private val random = SplittableRandom(seed)
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var b3 = 0.0
    private var b4 = 0.0
    private var b5 = 0.0
    private var b6 = 0.0
    private var brown = 0.0
    private var wavePhase = 0.0

    fun next(): Double {
        val white = random.nextDouble() * 2.0 - 1.0
        val value = when (sound) {
            FocusSound.OFF -> 0.0
            FocusSound.WHITE -> white * 0.35
            FocusSound.PINK -> pink(white)
            FocusSound.BROWN -> brown(white)
            FocusSound.WAVES -> brown(white) * swell()
        }
        return value.coerceIn(-1.0, 1.0)
    }

    fun fill(buffer: ShortArray) {
        for (i in buffer.indices) buffer[i] = (next() * Short.MAX_VALUE).toInt().toShort()
    }

    private fun pink(white: Double): Double {
        b0 = 0.99886 * b0 + white * 0.0555179
        b1 = 0.99332 * b1 + white * 0.0750759
        b2 = 0.96900 * b2 + white * 0.1538520
        b3 = 0.86650 * b3 + white * 0.3104856
        b4 = 0.55000 * b4 + white * 0.5329522
        b5 = -0.7616 * b5 - white * 0.0168980
        val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
        b6 = white * 0.115926
        return pink * 0.09
    }

    private fun brown(white: Double): Double {
        brown = (brown + 0.02 * white) / 1.02
        return brown * 3.0
    }

    /** Between 0.25 and 1, one slow rise and fall every [WAVE_SECONDS]. */
    private fun swell(): Double {
        wavePhase += 1.0 / (sampleRate * WAVE_SECONDS)
        if (wavePhase >= 1.0) wavePhase -= 1.0
        return 0.625 + 0.375 * sin(2.0 * PI * wavePhase)
    }

    private companion object {
        const val WAVE_SECONDS = 9.0
    }
}

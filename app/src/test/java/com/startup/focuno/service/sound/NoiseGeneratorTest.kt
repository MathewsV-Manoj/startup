package com.startup.focuno.service.sound

import com.startup.focuno.domain.model.FocusSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class NoiseGeneratorTest {

    private val rate = 22_050

    private fun samples(sound: FocusSound, count: Int = rate * 4): DoubleArray {
        val generator = NoiseGenerator(sound, rate, seed = 42L)
        return DoubleArray(count) { generator.next() }
    }

    private fun rms(values: DoubleArray) = sqrt(values.sumOf { it * it } / values.size)

    /** Average jump between neighbouring samples: small for deep, smooth sounds. */
    private fun roughness(values: DoubleArray) = (1 until values.size).sumOf { abs(values[it] - values[it - 1]) } / (values.size - 1)

    @Test
    fun everySoundStaysInRange() {
        FocusSound.entries.forEach { sound ->
            assertTrue(sound.name, samples(sound).all { it in -1.0..1.0 })
        }
    }

    @Test
    fun offIsSilent() {
        assertEquals(0.0, rms(samples(FocusSound.OFF)), 0.0)
    }

    @Test
    fun noisesAreAudibleButNotHarsh() {
        listOf(FocusSound.WHITE, FocusSound.PINK, FocusSound.BROWN, FocusSound.WAVES).forEach { sound ->
            val level = rms(samples(sound))
            assertTrue("$sound too quiet: $level", level > 0.02)
            assertTrue("$sound too loud: $level", level < 0.6)
        }
    }

    @Test
    fun brownAndPinkAreSmootherThanWhite() {
        val white = roughness(samples(FocusSound.WHITE))
        assertTrue(roughness(samples(FocusSound.PINK)) < white)
        assertTrue(roughness(samples(FocusSound.BROWN)) < white / 4)
    }

    @Test
    fun fillWritesEverySample() {
        val buffer = ShortArray(1_000)
        NoiseGenerator(FocusSound.WHITE, rate, seed = 1L).fill(buffer)
        assertTrue(buffer.count { it != 0.toShort() } > 900)
    }
}

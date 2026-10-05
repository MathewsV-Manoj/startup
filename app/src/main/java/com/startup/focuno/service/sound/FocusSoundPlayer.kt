package com.startup.focuno.service.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.startup.focuno.domain.model.FocusSound
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays generated focus noise on a background thread until [stop] or a set end time.
 *
 * Each [play] starts a new "generation". A thread keeps writing only while its generation is current, so
 * switching sounds never leaves an old thread playing, and nothing ever waits on the main thread.
 * It gives the sound up when another app takes audio focus, for example an incoming call.
 */
@Singleton
class FocusSoundPlayer @Inject constructor(@ApplicationContext context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val generation = AtomicInteger(0)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var focusRequest: AudioFocusRequest? = null

    private val _playing = MutableStateFlow(FocusSound.OFF)

    /** The sound playing right now, or OFF. */
    val playing: StateFlow<FocusSound> = _playing.asStateFlow()

    @Synchronized
    fun play(sound: FocusSound, untilMs: Long) {
        stop()
        if (sound == FocusSound.OFF || untilMs <= System.currentTimeMillis()) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
            }
            .build()
        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        focusRequest = request
        val myGeneration = generation.incrementAndGet()
        _playing.value = sound
        Thread({ loop(sound, untilMs, myGeneration) }, "focus-sound").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        generation.incrementAndGet()
        releaseFocus()
        _playing.value = FocusSound.OFF
    }

    private fun loop(sound: FocusSound, untilMs: Long, myGeneration: Int) {
        var track: AudioTrack? = null
        try {
            val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuffer, SAMPLE_RATE))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val generator = NoiseGenerator(sound, SAMPLE_RATE)
            val buffer = ShortArray(SAMPLE_RATE / 10)
            track.setVolume(0f)
            track.play()
            var step = 0
            while (generation.get() == myGeneration && System.currentTimeMillis() < untilMs) {
                generator.fill(buffer)
                // Fade in over the first second so it never starts with a burst.
                if (step <= FADE_STEPS) track.setVolume(step.toFloat() / FADE_STEPS)
                step++
                if (track.write(buffer, 0, buffer.size) < 0) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Focus sound stopped", e)
        } finally {
            try {
                track?.stop()
            } catch (_: IllegalStateException) {
                // Never started; nothing to stop.
            }
            track?.release()
            finishIfCurrent(myGeneration)
        }
    }

    /** A thread that ran out of time (or failed) tidies up, unless a newer sound has already taken over. */
    @Synchronized
    private fun finishIfCurrent(myGeneration: Int) {
        if (generation.get() != myGeneration) return
        releaseFocus()
        _playing.value = FocusSound.OFF
    }

    private fun releaseFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private companion object {
        const val TAG = "FocusSound"
        const val SAMPLE_RATE = 22_050
        const val FADE_STEPS = 10
    }
}

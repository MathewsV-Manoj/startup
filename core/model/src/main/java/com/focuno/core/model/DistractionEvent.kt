package com.focuno.core.model

import java.time.Instant

/**
 * A predicted moment of attention loss.
 *
 * @param at when the distraction was detected
 * @param probability model confidence in `[0, 1]`
 * @param reason human readable explanation surfaced in the UI
 */
data class DistractionEvent(
    val at: Instant,
    val probability: Float,
    val reason: String,
) {
    val isActionable: Boolean get() = probability >= ACTION_THRESHOLD

    companion object {
        const val ACTION_THRESHOLD = 0.65f
    }
}

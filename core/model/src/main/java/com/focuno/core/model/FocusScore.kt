package com.focuno.core.model

import kotlin.math.roundToInt

/**
 * Daily focus score with a fully explainable breakdown. Every component is in
 * `[0, 1]`; [score] is the weighted sum scaled to `0..100`.
 */
data class FocusScore(
    val date: java.time.LocalDate,
    val score: Int,
    val deepWorkRatio: Float,
    val switchStability: Float,
    val distractionResistance: Float,
    val consistency: Float,
) {
    /** Rank from 0 (worst) to 4 (best) for UI copy. */
    val band: FocusBand
        get() = when {
            score >= 85 -> FocusBand.EXCELLENT
            score >= 70 -> FocusBand.GOOD
            score >= 50 -> FocusBand.FAIR
            score >= 30 -> FocusBand.LOW
            else -> FocusBand.POOR
        }

    companion object {
        const val WEIGHT_DEEP_WORK = 0.45f
        const val WEIGHT_SWITCH = 0.30f
        const val WEIGHT_DISTRACTION = 0.15f
        const val WEIGHT_CONSISTENCY = 0.10f

        fun from(
            date: java.time.LocalDate,
            deepWorkRatio: Float,
            switchStability: Float,
            distractionResistance: Float,
            consistency: Float,
        ): FocusScore {
            val c = { v: Float -> v.coerceIn(0f, 1f) }
            val weighted =
                WEIGHT_DEEP_WORK * c(deepWorkRatio) +
                    WEIGHT_SWITCH * c(switchStability) +
                    WEIGHT_DISTRACTION * c(distractionResistance) +
                    WEIGHT_CONSISTENCY * c(consistency)
            return FocusScore(
                date = date,
                score = (weighted * 100f).roundToInt(),
                deepWorkRatio = c(deepWorkRatio),
                switchStability = c(switchStability),
                distractionResistance = c(distractionResistance),
                consistency = c(consistency),
            )
        }
    }
}

enum class FocusBand { EXCELLENT, GOOD, FAIR, LOW, POOR }

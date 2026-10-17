package com.focuno.core.domain.classifier

import com.focuno.core.model.DistractionEvent
import com.focuno.core.model.DistractionFeatures
import java.time.Clock
import java.time.Instant
import kotlin.math.exp

/**
 * Deterministic, explainable classifier used until a trained model exists.
 *
 * Combines three normalized risk signals with a logistic squash:
 *  - switch rate (rapid context switching),
 *  - late-night usage (fatigue),
 *  - short sessions (fragmented attention).
 *
 * Weights and thresholds are intentionally conservative; tune them against real
 * data in Milestone 3 of the roadmap.
 */
class HeuristicDistractionClassifier(
    private val clock: Clock = Clock.systemDefaultZone(),
) : DistractionClassifier {

    override fun classify(features: DistractionFeatures): DistractionEvent {
        val switchRisk = (features.switchRatePerHour / SWITCH_CAP).coerceIn(0f, 1f)
        val lateNightRisk = features.lateNightRatio.coerceIn(0f, 1f)
        val fragmentationRisk =
            (1f - (features.avgSessionMinutes / HEALTHY_SESSION_MINUTES)).coerceIn(0f, 1f)

        val logit =
            BIAS +
                W_SWITCH * switchRisk +
                W_LATE_NIGHT * lateNightRisk +
                W_FRAGMENTATION * fragmentationRisk

        val probability = (1.0 / (1.0 + exp(-logit))).toFloat()
        return DistractionEvent(
            at = Instant.now(clock),
            probability = probability,
            reason = reasonFor(switchRisk, lateNightRisk, fragmentationRisk),
        )
    }

    private fun reasonFor(switchRisk: Float, lateNightRisk: Float, fragmentationRisk: Float): String {
        val dominant = listOf(
            "Frequent app switching" to switchRisk,
            "Late-night usage" to lateNightRisk,
            "Short, fragmented sessions" to fragmentationRisk,
        ).maxBy { it.second }
        return dominant.first
    }

    companion object {
        private const val SWITCH_CAP = 30f
        private const val HEALTHY_SESSION_MINUTES = 20f
        private const val BIAS = -1.2
        private const val W_SWITCH = 3.0 * 0.5
        private const val W_LATE_NIGHT = 3.0 * 0.2
        private const val W_FRAGMENTATION = 3.0 * 0.3
    }
}

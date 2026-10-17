package com.focuno.core.domain.classifier

import com.focuno.core.model.DistractionFeatures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HeuristicDistractionClassifierTest {

    private val classifier = HeuristicDistractionClassifier()

    @Test
    fun `healthy focused usage yields low probability`() {
        val event = classifier.classify(
            DistractionFeatures(
                switchRatePerHour = 2f,
                unlockCount = 1,
                avgSessionMinutes = 45f,
                hourOfDay = 10,
                screenOnMinutes = 120f,
                lateNightRatio = 0f,
            ),
        )
        assertThat(event.probability).isLessThan(0.5f)
        assertThat(event.isActionable).isFalse()
    }

    @Test
    fun `chaotic late night usage yields high probability`() {
        val event = classifier.classify(
            DistractionFeatures(
                switchRatePerHour = 60f,
                unlockCount = 40,
                avgSessionMinutes = 2f,
                hourOfDay = 1,
                screenOnMinutes = 30f,
                lateNightRatio = 1f,
            ),
        )
        assertThat(event.probability).isGreaterThan(0.7f)
        assertThat(event.isActionable).isTrue()
    }

    @Test
    fun `probability is always within zero and one`() {
        val extreme = classifier.classify(
            DistractionFeatures(
                switchRatePerHour = 1_000f,
                unlockCount = 1_000,
                avgSessionMinutes = 0f,
                hourOfDay = 3,
                screenOnMinutes = 1f,
                lateNightRatio = 1f,
            ),
        )
        assertThat(extreme.probability).isAtLeast(0f)
        assertThat(extreme.probability).isAtMost(1f)
    }

    @Test
    fun `reason names the dominant signal`() {
        val event = classifier.classify(
            DistractionFeatures(
                switchRatePerHour = 60f,
                unlockCount = 0,
                avgSessionMinutes = 45f,
                hourOfDay = 10,
                screenOnMinutes = 60f,
                lateNightRatio = 0f,
            ),
        )
        assertThat(event.reason).isEqualTo("Frequent app switching")
    }
}

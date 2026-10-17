package com.focuno.core.domain.score

import com.focuno.core.model.AppCategory
import com.focuno.core.model.AppUsage
import com.focuno.core.model.FocusScore
import com.focuno.core.model.FocusSession
import com.focuno.core.model.UserFocusProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class FocusScoreEngineTest {

    private val date = LocalDate.of(2026, 1, 1)
    private val profile = UserFocusProfile(deepWorkThresholdMinutes = 25)

    private fun usage(minutes: Long, launches: Int, category: AppCategory) = AppUsage(
        packageName = "app.$minutes.$category",
        appLabel = null,
        date = date,
        totalTimeInForeground = Duration.ofMinutes(minutes),
        launchCount = launches,
        category = category,
    )

    private fun session(minutes: Long, interrupted: Boolean) = FocusSession(
        startedAt = Instant.parse("2026-01-01T09:00:00Z"),
        endedAt = Instant.parse("2026-01-01T09:00:00Z").plusSeconds(minutes * 60),
        dominantPackage = "com.focuno.study",
        interrupted = interrupted,
    )

    @Test
    fun `no usage yields zero score`() {
        val score = FocusScoreEngine.compute(date, emptyList(), emptyList(), 0f, profile)
        assertThat(score.score).isEqualTo(0)
        assertThat(score.deepWorkRatio).isEqualTo(0f)
    }

    @Test
    fun `long uninterrupted deep work scores high`() {
        val score = FocusScoreEngine.compute(
            date = date,
            usages = listOf(usage(minutes = 120, launches = 4, category = AppCategory.PRODUCTIVE)),
            sessions = listOf(session(minutes = 60, interrupted = false)),
            consistency = 1f,
            profile = profile,
        )
        assertThat(score.score).isAtLeast(85)
        assertThat(score.band).isEqualTo(FocusBand.EXCELLENT)
    }

    @Test
    fun `fragmented distracting usage scores low`() {
        val score = FocusScoreEngine.compute(
            date = date,
            usages = listOf(usage(minutes = 180, launches = 500, category = AppCategory.DISTRACTING)),
            sessions = listOf(
                session(minutes = 5, interrupted = true),
                session(minutes = 5, interrupted = true),
            ),
            consistency = 0f,
            profile = profile,
        )
        assertThat(score.score).isAtMost(30)
        assertThat(score.band).isIn(setOf(FocusBand.POOR, FocusBand.LOW))
    }

    @Test
    fun `deep work ratio falls back to productive usage when no sessions`() {
        val score = FocusScoreEngine.compute(
            date = date,
            usages = listOf(
                usage(minutes = 30, launches = 2, category = AppCategory.PRODUCTIVE),
                usage(minutes = 90, launches = 2, category = AppCategory.DISTRACTING),
            ),
            sessions = emptyList(),
            consistency = 0f,
            profile = profile,
        )
        assertThat(score.deepWorkRatio).isWithin(0.001f).of(0.25f)
    }

    @Test
    fun `components are clamped to valid range`() {
        val score = FocusScoreEngine.compute(
            date = date,
            usages = listOf(usage(minutes = 60, launches = 10_000, category = AppCategory.PRODUCTIVE)),
            sessions = emptyList(),
            consistency = 5f,
            profile = profile,
        )
        assertThat(score.consistency).isEqualTo(1f)
        assertThat(score.switchStability).isAtLeast(0f)
        assertThat(score.switchStability).isAtMost(1f)
        assertThat(score.score).isIn(0..100)
    }
}

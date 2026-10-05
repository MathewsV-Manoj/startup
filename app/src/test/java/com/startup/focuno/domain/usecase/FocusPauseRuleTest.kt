package com.startup.focuno.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPauseRuleTest {

    private val essentials = setOf("com.android.settings", "com.google.android.dialer")

    @Test
    fun timeEatersArePausedInBothModes() {
        assertTrue(FocusPauseRule.pauses("ig", isTimeEater = true, lockAll = false, allowed = emptySet(), essentials = essentials))
        assertTrue(FocusPauseRule.pauses("ig", isTimeEater = true, lockAll = true, allowed = emptySet(), essentials = essentials))
    }

    @Test
    fun otherAppsArePausedOnlyInLockMode() {
        assertFalse(FocusPauseRule.pauses("chrome", isTimeEater = false, lockAll = false, allowed = emptySet(), essentials = essentials))
        assertTrue(FocusPauseRule.pauses("chrome", isTimeEater = false, lockAll = true, allowed = emptySet(), essentials = essentials))
    }

    @Test
    fun allowedAppsAndEssentialsStayOpenInLockMode() {
        assertFalse(FocusPauseRule.pauses("unacademy", isTimeEater = false, lockAll = true, allowed = setOf("unacademy"), essentials = essentials))
        assertFalse(FocusPauseRule.pauses("com.android.settings", isTimeEater = false, lockAll = true, allowed = emptySet(), essentials = essentials))
        assertFalse(FocusPauseRule.pauses("com.google.android.dialer", isTimeEater = false, lockAll = true, allowed = emptySet(), essentials = essentials))
    }
}

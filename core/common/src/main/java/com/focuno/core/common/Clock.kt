package com.focuno.core.common

import java.time.Clock

/**
 * Injectable clock so score/aggregation logic is deterministic in tests.
 */
fun systemClock(): Clock = Clock.systemDefaultZone()

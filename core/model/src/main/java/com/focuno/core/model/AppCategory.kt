package com.focuno.core.model

/**
 * How an app is classified for focus scoring. Users can override the default
 * classification in the analytics screen; the override is stored in the data layer.
 */
enum class AppCategory {
    PRODUCTIVE,
    NEUTRAL,
    DISTRACTING,
}

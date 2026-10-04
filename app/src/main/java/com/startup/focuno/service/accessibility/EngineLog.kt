package com.startup.focuno.service.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A short rolling log of what the blocker saw and decided (package names and decisions only).
 * Shown in the Health check so a misbehaving block can be diagnosed from a screenshot.
 */
@Singleton
class EngineLog @Inject constructor() {

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    fun add(message: String) {
        val line = "${LocalTime.now().format(FORMAT)}  $message"
        _entries.update { (it + line).takeLast(MAX_LINES) }
    }

    private companion object {
        const val MAX_LINES = 80
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}

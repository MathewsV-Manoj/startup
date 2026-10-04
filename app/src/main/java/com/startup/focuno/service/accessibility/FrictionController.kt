package com.startup.focuno.service.accessibility

import com.startup.focuno.domain.model.BypassOutcome
import com.startup.focuno.domain.usecase.BypassPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FrictionState {
    data object Idle : FrictionState
    data class Countdown(val secondsLeft: Int) : FrictionState
    data class Reason(val text: String) : FrictionState
}

/**
 * The cost of "I need this": a 15 second countdown that cannot be skipped, then a typed reason of at
 * least 15 characters, then a 5 minute unlock. Every stage is reported through [onEvent], including
 * quitting part-way, because the abandon rate is the number that shows the friction is working.
 */
class FrictionController(
    private val scope: CoroutineScope,
    private val onEvent: (BypassOutcome, String?) -> Unit,
    private val onGranted: (String) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<FrictionState>(FrictionState.Idle)
    val state: StateFlow<FrictionState> = _state.asStateFlow()
    private var countdownJob: Job? = null

    fun begin() {
        if (_state.value != FrictionState.Idle) return
        val total = BypassPolicy.FRICTION_COUNTDOWN_SECONDS
        _state.value = FrictionState.Countdown(total)
        val endAt = clock() + total * 1_000L
        countdownJob = scope.launch {
            while (true) {
                val remainingMs = endAt - clock()
                if (remainingMs <= 0L) break
                _state.value = FrictionState.Countdown(((remainingMs + 999) / 1_000).toInt())
                delay(200)
            }
            _state.value = FrictionState.Reason("")
            onEvent(BypassOutcome.COMPLETED_FRICTION, null)
        }
    }

    fun onReasonChange(text: String) {
        if (_state.value is FrictionState.Reason) _state.value = FrictionState.Reason(text)
    }

    fun unlock() {
        val current = _state.value as? FrictionState.Reason ?: return
        if (!BypassPolicy.isReasonValid(current.text)) return
        val reason = current.text.trim()
        _state.value = FrictionState.Idle
        onGranted(reason)
    }

    /** Called when the person leaves, goes home, or the screen turns off before unlocking. */
    fun abandon() {
        val stage = when (_state.value) {
            is FrictionState.Countdown -> "countdown"
            is FrictionState.Reason -> "reason"
            FrictionState.Idle -> return
        }
        countdownJob?.cancel()
        _state.value = FrictionState.Idle
        onEvent(BypassOutcome.ABANDONED, "stage:$stage")
    }

    fun reset() {
        countdownJob?.cancel()
        _state.value = FrictionState.Idle
    }
}

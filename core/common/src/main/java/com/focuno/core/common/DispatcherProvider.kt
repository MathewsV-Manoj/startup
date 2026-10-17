package com.focuno.core.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Injected everywhere coroutines are launched, so tests can supply
 * `UnconfinedTestDispatcher` and production supplies Android dispatchers.
 */
interface DispatcherProvider {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
}

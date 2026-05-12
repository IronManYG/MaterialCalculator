package dev.gaddal.sifr.core.data.calculator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-shot bus that lets the History feature push an expression string back
 * to the Calculator without either feature importing the other. Calculator's
 * ViewModel collects [events] in init; History sends via [emit] right before
 * navigating back.
 *
 * Why a Channel and not a StateFlow: this is a one-shot signal — we don't
 * want it to replay on configuration change.
 */
class CalculatorInputBus {
    private val _events = Channel<String>(capacity = Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    suspend fun emit(expression: String) {
        _events.send(expression)
    }
}

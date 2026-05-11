package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent

sealed interface CalculatorEvent {
    data object NavigateToSettings : CalculatorEvent
    data object NavigateToHistory : CalculatorEvent
    data class PlayFeedback(val intent: FeedbackIntent) : CalculatorEvent
}

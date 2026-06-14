package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent

sealed interface CalculatorEvent {
    data object NavigateToSettings : CalculatorEvent
    data object NavigateToHistory : CalculatorEvent
    data object NavigateToTools : CalculatorEvent
    data class PlayFeedback(val intent: FeedbackIntent) : CalculatorEvent
    // v1.5 result actions
    data class CopyToClipboard(val text: String) : CalculatorEvent
    data class ShareText(val text: String) : CalculatorEvent
}

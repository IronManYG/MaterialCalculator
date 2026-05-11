package dev.gaddal.sifr.feature.history.ui

import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent

sealed interface HistoryEvent {
    data object NavigateBack : HistoryEvent
    data class PlayFeedback(val intent: FeedbackIntent) : HistoryEvent
}

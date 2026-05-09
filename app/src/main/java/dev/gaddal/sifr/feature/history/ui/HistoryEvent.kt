package dev.gaddal.sifr.feature.history.ui

sealed interface HistoryEvent {
    data object NavigateBack : HistoryEvent
}

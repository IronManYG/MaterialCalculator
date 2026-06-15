package dev.gaddal.sifr.feature.tools.ui

sealed interface ToolsEvent {
    data object NavigateBack : ToolsEvent
    /** Emitted after a background refresh fails and the UI should show a brief note. */
    data object RatesRefreshFailed : ToolsEvent
}

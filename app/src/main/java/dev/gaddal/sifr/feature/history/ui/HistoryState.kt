package dev.gaddal.sifr.feature.history.ui

import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.RestoreTarget

data class HistoryState(
    val entries: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false,
    val restoreTarget: RestoreTarget = RestoreTarget.Result, // v1.7.x — mirror of AppSettings; drives entry-tap restore
)

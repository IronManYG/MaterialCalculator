package dev.gaddal.sifr.feature.history.ui

import dev.gaddal.sifr.core.domain.history.HistoryEntry

data class HistoryState(
    val entries: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false,
)

package dev.gaddal.sifr.feature.history.ui

import dev.gaddal.sifr.core.domain.history.HistoryEntry

sealed interface HistoryAction {
    data class EntryClicked(val entry: HistoryEntry) : HistoryAction
    data class DeleteEntry(val id: Long) : HistoryAction
    data object ClearAllClicked : HistoryAction
    data object ConfirmClearAll : HistoryAction
    data object DismissClearConfirm : HistoryAction
    data object BackClicked : HistoryAction
}

package dev.gaddal.sifr.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: HistoryRepository,
    private val bus: CalculatorInputBus,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _events = Channel<HistoryEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { entries ->
                _state.update { it.copy(entries = entries, isLoading = false) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                _state.update { it.copy(restoreTarget = settings.restoreTarget) }
            }
        }
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.EntryClicked -> viewModelScope.launch {
                // Restore the result (default) or the original expression, per the
                // user's Settings choice. Restoring an expression re-shows its
                // `= result` live preview back on the calculator.
                val value = if (_state.value.restoreTarget == RestoreTarget.Expression) {
                    action.entry.expression
                } else {
                    action.entry.result
                }
                bus.emit(value)
                _events.send(HistoryEvent.PlayFeedback(FeedbackIntent.Selection))
                _events.send(HistoryEvent.NavigateBack)
            }
            is HistoryAction.DeleteEntry -> viewModelScope.launch {
                repository.delete(action.id)
                _events.send(HistoryEvent.PlayFeedback(FeedbackIntent.Destructive))
            }
            HistoryAction.ClearAllClicked -> _state.update { it.copy(showClearConfirm = true) }
            HistoryAction.ConfirmClearAll -> {
                _state.update { it.copy(showClearConfirm = false) }
                viewModelScope.launch {
                    repository.clear()
                    _events.send(HistoryEvent.PlayFeedback(FeedbackIntent.Destructive))
                }
            }
            HistoryAction.DismissClearConfirm -> _state.update { it.copy(showClearConfirm = false) }
            HistoryAction.BackClicked -> viewModelScope.launch {
                _events.send(HistoryEvent.NavigateBack)
            }
        }
    }
}

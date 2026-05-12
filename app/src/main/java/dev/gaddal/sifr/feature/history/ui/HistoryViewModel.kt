package dev.gaddal.sifr.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
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
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.EntryClicked -> viewModelScope.launch {
                bus.emit(action.entry.result)
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

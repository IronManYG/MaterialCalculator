package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(
    private val writer: ExpressionWriter,
    private val historyRepository: HistoryRepository,
    private val inputBus: CalculatorInputBus,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private val _events = Channel<CalculatorEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            inputBus.events.collect { expression ->
                onAction(CalculatorAction.RestoreExpression(expression))
            }
        }
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            CalculatorAction.SettingsClicked -> emit(CalculatorEvent.NavigateToSettings)
            CalculatorAction.HistoryClicked -> emit(CalculatorEvent.NavigateToHistory)
            else -> applyToWriter(action)
        }
    }

    private fun applyToWriter(action: CalculatorAction) {
        val pre = writer.expression
        writer.processAction(action)
        val post = writer.expression
        _state.update { it.copy(expression = post) }
        if (action == CalculatorAction.Calculate &&
            post != "Error" &&
            pre.isNotBlank() &&
            pre != post
        ) {
            viewModelScope.launch { historyRepository.add(pre, post) }
        }
    }

    private fun emit(event: CalculatorEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

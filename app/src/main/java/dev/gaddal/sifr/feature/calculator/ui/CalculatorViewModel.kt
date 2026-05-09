package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private val _events = Channel<CalculatorEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CalculatorAction) {
        when (action) {
            CalculatorAction.SettingsClicked -> {
                viewModelScope.launch { _events.send(CalculatorEvent.NavigateToSettings) }
            }
            else -> {
                writer.processAction(action)
                _state.update { it.copy(expression = writer.expression) }
            }
        }
    }
}

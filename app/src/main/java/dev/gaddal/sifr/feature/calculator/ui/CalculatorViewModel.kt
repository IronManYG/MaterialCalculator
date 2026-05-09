package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.ViewModel
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalculatorViewModel(
    private val writer: ExpressionWriter = ExpressionWriter(),
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    // Removed in Task 5 once CalculatorScreen reads from `state` instead.
    val expression: String get() = _state.value.expression

    fun onAction(action: CalculatorAction) {
        writer.processAction(action)
        _state.update { it.copy(expression = writer.expression) }
    }
}

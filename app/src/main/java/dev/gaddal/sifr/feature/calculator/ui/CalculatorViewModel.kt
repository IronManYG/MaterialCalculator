package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.core.domain.util.onFailure
import dev.gaddal.sifr.core.domain.util.onSuccess
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import dev.gaddal.sifr.feature.calculator.domain.operationSymbols
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
            .onSuccess {
                val post = writer.expression
                _state.update {
                    it.copy(
                        expression = post,
                        livePreview = computePreview(),
                        error = null,
                    )
                }
                onSuccessSideEffects(action, pre, post)
            }
            .onFailure { error ->
                _state.update { it.copy(livePreview = null, error = error.toUiText()) }
                emit(CalculatorEvent.PlayFeedback(FeedbackIntent.Error))
            }
    }

    private fun computePreview(): String? {
        val expr = writer.expression
        if (expr.isBlank()) return null
        // Suppress preview while mid-token: trailing operator, opening paren, or unfinished decimal
        if (expr.last() in "$operationSymbols(.") return null
        // Nothing to evaluate when the expression is a single literal number.
        // Without this gate, 16+ digit entries hit Double precision and the
        // formatted result echoes back a rounded variant of the input.
        if (expr.none { it in "$operationSymbols()" }) return null
        return when (val preview = writer.tryEvaluate()) {
            is Result.Success -> preview.data.takeIf { it != expr }
            is Result.Error -> null
        }
    }

    private fun onSuccessSideEffects(action: CalculatorAction, pre: String, post: String) {
        when (action) {
            CalculatorAction.Calculate -> {
                if (pre.isNotBlank() && pre != post) {
                    viewModelScope.launch { historyRepository.add(pre, post) }
                    emit(CalculatorEvent.PlayFeedback(FeedbackIntent.CalculateSuccess))
                }
            }
            CalculatorAction.Clear -> {
                if (pre.isNotEmpty()) {
                    emit(CalculatorEvent.PlayFeedback(FeedbackIntent.Destructive))
                }
            }
            else -> Unit
        }
    }

    private fun emit(event: CalculatorEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

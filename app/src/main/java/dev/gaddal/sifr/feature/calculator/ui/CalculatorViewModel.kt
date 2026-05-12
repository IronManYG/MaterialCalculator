package dev.gaddal.sifr.feature.calculator.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.core.domain.util.onFailure
import dev.gaddal.sifr.core.domain.util.onSuccess
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
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
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state: MutableStateFlow<CalculatorState>
    val state: StateFlow<CalculatorState>

    private val _events = Channel<CalculatorEvent>()
    val events = _events.receiveAsFlow()

    init {
        val restoredExpression: String = savedStateHandle[KEY_EXPRESSION] ?: ""
        val restoredCursor: Int = savedStateHandle[KEY_CURSOR] ?: 0
        val restoredMemory: Double? = savedStateHandle[KEY_MEMORY]
        if (restoredExpression.isNotEmpty()) {
            writer.restoreState(restoredExpression, restoredCursor)
        }
        _state = MutableStateFlow(
            CalculatorState(
                expression = writer.expression,
                cursor = writer.cursor,
                selectionStart = writer.selectionStart,
                livePreview = computePreview(),
                memoryValue = restoredMemory,
            )
        )
        state = _state.asStateFlow()

        viewModelScope.launch {
            inputBus.events.collect { expression ->
                onAction(CalculatorAction.RestoreExpression(expression))
            }
        }
        viewModelScope.launch {
            settingsRepository.observe().collect { settings ->
                writer.angleUnit = settings.angleUnit
                _state.update {
                    it.copy(
                        mode = settings.calculatorMode,
                        angleUnit = settings.angleUnit,
                        livePreview = computePreview(),
                    )
                }
            }
        }
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            CalculatorAction.SettingsClicked -> emit(CalculatorEvent.NavigateToSettings)
            CalculatorAction.HistoryClicked -> emit(CalculatorEvent.NavigateToHistory)
            CalculatorAction.ToggleMode -> viewModelScope.launch {
                settingsRepository.update {
                    copy(
                        calculatorMode = if (calculatorMode == CalculatorMode.Basic)
                            CalculatorMode.Scientific else CalculatorMode.Basic
                    )
                }
            }
            CalculatorAction.ToggleAngleUnit -> viewModelScope.launch {
                settingsRepository.update {
                    copy(
                        angleUnit = if (angleUnit == AngleUnit.Degrees)
                            AngleUnit.Radians else AngleUnit.Degrees
                    )
                }
            }
            CalculatorAction.MemoryClear -> updateMemory(null)
            CalculatorAction.MemoryAdd -> applyMemoryOp { current, value -> (current ?: 0.0) + value }
            CalculatorAction.MemorySubtract -> applyMemoryOp { current, value -> (current ?: 0.0) - value }
            CalculatorAction.MemoryRecall -> {
                val v = _state.value.memoryValue ?: return
                applyToWriter(CalculatorAction.InsertText(numberToInsert(v)))
            }
            else -> applyToWriter(action)
        }
    }

    private fun applyMemoryOp(combine: (Double?, Double) -> Double) {
        when (val r = writer.tryEvaluate()) {
            is Result.Success -> {
                val parsed = r.data.toDoubleOrNull()
                if (parsed != null) {
                    updateMemory(combine(_state.value.memoryValue, parsed))
                }
            }
            is Result.Error -> emit(CalculatorEvent.PlayFeedback(FeedbackIntent.Error))
        }
    }

    private fun updateMemory(newValue: Double?) {
        _state.update { it.copy(memoryValue = newValue) }
        savedStateHandle[KEY_MEMORY] = newValue
    }

    private fun numberToInsert(v: Double): String {
        // Render with the same formatter the display uses so MR -> AC -> MR
        // round-trips cleanly. For now, defer to Double.toString — the parser
        // already handles scientific notation as a single Number token (2.9).
        return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }

    private fun applyToWriter(action: CalculatorAction) {
        val pre = writer.expression
        writer.processAction(action)
            .onSuccess {
                val post = writer.expression
                _state.update {
                    it.copy(
                        expression = post,
                        cursor = writer.cursor,
                        selectionStart = writer.selectionStart,
                        livePreview = computePreview(),
                        error = null,
                    )
                }
                persistForRestore(post, writer.cursor)
                onSuccessSideEffects(action, pre, post)
            }
            .onFailure { error ->
                _state.update { it.copy(livePreview = null, error = error.toUiText()) }
                emit(CalculatorEvent.PlayFeedback(FeedbackIntent.Error))
            }
    }

    private fun persistForRestore(expression: String, cursor: Int) {
        savedStateHandle[KEY_EXPRESSION] = expression
        savedStateHandle[KEY_CURSOR] = cursor
    }

    private fun computePreview(): String? {
        val expr = writer.expression
        if (expr.isBlank()) return null
        // Suppress preview while mid-token: trailing operator, opening paren,
        // or unfinished decimal. `!`, `)`, constants, and digits are valid
        // expression terminators and CAN preview.
        if (expr.last() in "$operationSymbols(.") return null
        if (expr.none { it in "$operationSymbols()!πe" || it.isLetter() }) return null
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

    companion object {
        private const val KEY_EXPRESSION = "expression"
        private const val KEY_CURSOR = "cursor"
        private const val KEY_MEMORY = "memory"
    }
}

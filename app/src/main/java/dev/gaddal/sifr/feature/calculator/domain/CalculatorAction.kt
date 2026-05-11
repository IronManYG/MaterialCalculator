package dev.gaddal.sifr.feature.calculator.domain

sealed interface CalculatorAction {
    data class Number(val number: Int): CalculatorAction
    data class Op(val operation: Operation): CalculatorAction
    object Clear: CalculatorAction
    object Delete: CalculatorAction
    object Parentheses: CalculatorAction
    object Calculate: CalculatorAction
    object Decimal: CalculatorAction
    data object SettingsClicked: CalculatorAction
    data object HistoryClicked: CalculatorAction
    data class RestoreExpression(val value: String): CalculatorAction
    data class CursorChanged(val newPosition: Int): CalculatorAction
    // start = anchor, end = caret. When start == end the selection is collapsed
    // (no range). Only dispatched when `rangeSelectionEnabled` is on in
    // AppSettings — otherwise the field collapses ranges via CursorChanged.
    data class SelectionChanged(val start: Int, val end: Int): CalculatorAction
}

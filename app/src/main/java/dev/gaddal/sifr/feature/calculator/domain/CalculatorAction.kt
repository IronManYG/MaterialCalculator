package dev.gaddal.sifr.feature.calculator.domain

sealed interface CalculatorAction {
    data class Number(val number: Int) : CalculatorAction
    data class Op(val operation: Operation) : CalculatorAction
    data class Function(val name: String) : CalculatorAction
    data class Constant(val symbol: ConstantSymbol) : CalculatorAction
    object Factorial : CalculatorAction
    object Clear : CalculatorAction
    object Delete : CalculatorAction
    object Parentheses : CalculatorAction
    object Calculate : CalculatorAction
    object Decimal : CalculatorAction
    data object SettingsClicked : CalculatorAction
    data object HistoryClicked : CalculatorAction
    data class RestoreExpression(val value: String) : CalculatorAction
    data class CursorChanged(val newPosition: Int) : CalculatorAction
    data class SelectionChanged(val start: Int, val end: Int) : CalculatorAction
    data object ToggleMode : CalculatorAction
    data object ToggleAngleUnit : CalculatorAction
    data object MemoryClear : CalculatorAction
    data object MemoryAdd : CalculatorAction
    data object MemorySubtract : CalculatorAction
    data object MemoryRecall : CalculatorAction
    // Internal action: dispatched by the VM from MemoryRecall after it
    // resolves the stored value into a printable string. The writer
    // inserts at the cursor and respects the current selection (range-replace).
    data class InsertText(val text: String) : CalculatorAction
}

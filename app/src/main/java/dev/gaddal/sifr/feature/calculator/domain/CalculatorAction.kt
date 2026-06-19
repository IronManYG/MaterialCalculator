package dev.gaddal.sifr.feature.calculator.domain

sealed interface CalculatorAction {
    data class Number(val number: Int) : CalculatorAction
    data class Op(val operation: Operation) : CalculatorAction
    data class Function(val name: String) : CalculatorAction
    data class Constant(val symbol: ConstantSymbol) : CalculatorAction
    data object Factorial : CalculatorAction
    data object Clear : CalculatorAction
    data object Delete : CalculatorAction
    data object Parentheses : CalculatorAction
    data object Calculate : CalculatorAction
    data object Decimal : CalculatorAction
    data object SettingsClicked : CalculatorAction
    data object HistoryClicked : CalculatorAction
    data object ToolsClicked : CalculatorAction
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
    // v1.5 result actions — shown in the ResultActionsRow right after '='
    data object CopyResult : CalculatorAction
    data object ShareResult : CalculatorAction
    // v1.7 ANS→ : commit the just-evaluated result as the editable working expression
    data object UseAnswer : CalculatorAction
}

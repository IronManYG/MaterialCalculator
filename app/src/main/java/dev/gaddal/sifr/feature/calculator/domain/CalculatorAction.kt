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
}
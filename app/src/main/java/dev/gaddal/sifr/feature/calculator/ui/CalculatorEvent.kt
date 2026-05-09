package dev.gaddal.sifr.feature.calculator.ui

sealed interface CalculatorEvent {
    data object NavigateToSettings : CalculatorEvent
}

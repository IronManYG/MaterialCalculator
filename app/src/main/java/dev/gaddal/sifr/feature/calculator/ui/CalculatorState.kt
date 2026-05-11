package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.ui.util.UiText

data class CalculatorState(
    val expression: String = "",
    val livePreview: String? = null,
    val error: UiText? = null,
)

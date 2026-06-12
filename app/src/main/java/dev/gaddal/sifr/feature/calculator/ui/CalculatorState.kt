package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode

data class CalculatorState(
    val expression: String = "",
    val cursor: Int = 0,
    val selectionStart: Int = 0,
    val livePreview: String? = null,
    val error: UiText? = null,
    val mode: CalculatorMode = CalculatorMode.Basic,
    val angleUnit: AngleUnit = AngleUnit.Degrees,
    val memoryValue: Double? = null,
    val fractionResults: Boolean = false, // v1.5 — mirror of AppSettings.fractionResults
    val justEvaluated: Boolean = false,   // v1.5 — true only right after a successful '='
)

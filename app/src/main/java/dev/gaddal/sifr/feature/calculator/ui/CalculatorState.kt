package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
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
    val evaluatedInput: String? = null,   // v1.7 — input expression kept while justEvaluated (D3); null otherwise
    val keypadLayout: KeypadLayout = KeypadLayout.Classic, // v1.6 — mirror of AppSettings
    val memoryKeysVisible: Boolean = true,                 // v1.6 — mirror of AppSettings
    val recentHistory: List<HistoryEntry> = emptyList(),   // v1.7 — last 3 entries for the Tape receipt
    val restoreTarget: RestoreTarget = RestoreTarget.Result, // v1.7.x — mirror of AppSettings; drives Tape-row restore
)

package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.core.ui.util.UiText

data class CalculatorState(
    val expression: String = "",
    val cursor: Int = 0,
    // Anchor of the current selection — equals `cursor` when no range is
    // selected. When `rangeSelectionEnabled` is on, `selectionStart` and
    // `cursor` can diverge to describe a range that an insertion would
    // replace.
    val selectionStart: Int = 0,
    val livePreview: String? = null,
    val error: UiText? = null,
)

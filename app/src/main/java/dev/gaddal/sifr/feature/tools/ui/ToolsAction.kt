package dev.gaddal.sifr.feature.tools.ui

import dev.gaddal.sifr.feature.tools.domain.UnitCategory
import java.time.LocalDate

sealed interface ToolsAction {
    // Navigation
    data object BackClicked : ToolsAction

    // Tab
    data class SelectTab(val tab: ToolTab) : ToolsAction

    // Numpad (feeds the currently-focused field)
    data class NumKey(val char: Char) : ToolsAction
    data object Backspace : ToolsAction
    data class FocusField(val field: FocusedField) : ToolsAction

    // Units
    data class SelectCategory(val cat: UnitCategory) : ToolsAction
    data class SelectFromUnit(val unit: String) : ToolsAction
    data class SelectToUnit(val unit: String) : ToolsAction

    // Currency
    data class SelectFromCurrency(val code: String) : ToolsAction
    data class SelectToCurrency(val code: String) : ToolsAction
    data object RefreshRates : ToolsAction

    // Tip
    data class SetTipPct(val pct: Int) : ToolsAction
    data object IncrementSplit : ToolsAction
    data object DecrementSplit : ToolsAction

    // Date
    data class SetDate1(val date: LocalDate) : ToolsAction
    data class SetDate2(val date: LocalDate) : ToolsAction
}

package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.domain.ConstantSymbol
import dev.gaddal.sifr.feature.calculator.domain.Operation

val memoryRow: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("MC", HighlightLevel.SemiHighlighted, CalculatorAction.MemoryClear),
    CalculatorUiAction("M+", HighlightLevel.SemiHighlighted, CalculatorAction.MemoryAdd),
    CalculatorUiAction("M−", HighlightLevel.SemiHighlighted, CalculatorAction.MemorySubtract),
    CalculatorUiAction("MR", HighlightLevel.SemiHighlighted, CalculatorAction.MemoryRecall),
)

/**
 * Scientific row cells, compact layout (a 4-column grid).
 *
 * The `deg`/`rad` cells are mutually exclusive — only one is rendered at a
 * time, picked by the current angleUnit. See [buildCalculatorActions].
 */
val scientificCells: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("sin", HighlightLevel.Neutral, CalculatorAction.Function("sin")),
    CalculatorUiAction("cos", HighlightLevel.Neutral, CalculatorAction.Function("cos")),
    CalculatorUiAction("tan", HighlightLevel.Neutral, CalculatorAction.Function("tan")),
    CalculatorUiAction("ln", HighlightLevel.Neutral, CalculatorAction.Function("ln")),
    CalculatorUiAction("log", HighlightLevel.Neutral, CalculatorAction.Function("log")),
    CalculatorUiAction("π", HighlightLevel.Neutral, CalculatorAction.Constant(ConstantSymbol.PI)),
    CalculatorUiAction("e", HighlightLevel.Neutral, CalculatorAction.Constant(ConstantSymbol.E)),
    CalculatorUiAction("√", HighlightLevel.Neutral, CalculatorAction.Function("sqrt")),
    CalculatorUiAction("x^y", HighlightLevel.Neutral, CalculatorAction.Op(Operation.POWER)),
    CalculatorUiAction("x!", HighlightLevel.Neutral, CalculatorAction.Factorial),
    CalculatorUiAction("asin", HighlightLevel.Neutral, CalculatorAction.Function("asin")),
    CalculatorUiAction("acos", HighlightLevel.Neutral, CalculatorAction.Function("acos")),
    CalculatorUiAction("atan", HighlightLevel.Neutral, CalculatorAction.Function("atan")),
    CalculatorUiAction("exp", HighlightLevel.Neutral, CalculatorAction.Function("exp")),
    // Angle-unit toggle — the displayed label is the CURRENT unit. Filter
    // in buildCalculatorActions to only emit the matching cell.
    CalculatorUiAction("deg", HighlightLevel.SemiHighlighted, CalculatorAction.ToggleAngleUnit),
    CalculatorUiAction("rad", HighlightLevel.SemiHighlighted, CalculatorAction.ToggleAngleUnit),
)

val basicRows: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("AC", HighlightLevel.Highlighted, CalculatorAction.Clear),
    CalculatorUiAction("()", HighlightLevel.SemiHighlighted, CalculatorAction.Parentheses),
    CalculatorUiAction("%", HighlightLevel.SemiHighlighted, CalculatorAction.Op(Operation.PERCENT)),
    CalculatorUiAction("÷", HighlightLevel.SemiHighlighted, CalculatorAction.Op(Operation.DIVIDE)),
    CalculatorUiAction("7", HighlightLevel.Neutral, CalculatorAction.Number(7)),
    CalculatorUiAction("8", HighlightLevel.Neutral, CalculatorAction.Number(8)),
    CalculatorUiAction("9", HighlightLevel.Neutral, CalculatorAction.Number(9)),
    CalculatorUiAction("x", HighlightLevel.SemiHighlighted, CalculatorAction.Op(Operation.MULTIPLY)),
    CalculatorUiAction("4", HighlightLevel.Neutral, CalculatorAction.Number(4)),
    CalculatorUiAction("5", HighlightLevel.Neutral, CalculatorAction.Number(5)),
    CalculatorUiAction("6", HighlightLevel.Neutral, CalculatorAction.Number(6)),
    CalculatorUiAction("-", HighlightLevel.SemiHighlighted, CalculatorAction.Op(Operation.SUBTRACT)),
    CalculatorUiAction("1", HighlightLevel.Neutral, CalculatorAction.Number(1)),
    CalculatorUiAction("2", HighlightLevel.Neutral, CalculatorAction.Number(2)),
    CalculatorUiAction("3", HighlightLevel.Neutral, CalculatorAction.Number(3)),
    CalculatorUiAction("+", HighlightLevel.SemiHighlighted, CalculatorAction.Op(Operation.ADD)),
    CalculatorUiAction("0", HighlightLevel.Neutral, CalculatorAction.Number(0)),
    CalculatorUiAction(".", HighlightLevel.Neutral, CalculatorAction.Decimal),
    CalculatorUiAction(
        text = null,
        content = {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        highlightLevel = HighlightLevel.Neutral,
        action = CalculatorAction.Delete,
    ),
    CalculatorUiAction("=", HighlightLevel.StronglyHighlighted, CalculatorAction.Calculate),
)

/**
 * Assemble the visible button list for the current mode + angle unit.
 *
 * Layout (top-to-bottom):
 * 1. Scientific cells (Scientific mode only) — filtered to drop the inactive
 *    deg/rad cell so only the cell matching the current angleUnit appears.
 * 2. Memory row (always — both modes).
 * 3. Basic rows (always).
 */
fun buildCalculatorActions(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
): List<CalculatorUiAction> = buildList {
    if (mode == CalculatorMode.Scientific) {
        addAll(
            scientificCells.filterNot { cell ->
                val text = cell.text
                (text == "deg" && angleUnit == AngleUnit.Radians) ||
                    (text == "rad" && angleUnit == AngleUnit.Degrees)
            }
        )
    }
    addAll(memoryRow)
    addAll(basicRows)
}

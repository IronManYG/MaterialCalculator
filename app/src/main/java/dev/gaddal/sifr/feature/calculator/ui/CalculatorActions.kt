package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ConstantSymbol
import dev.gaddal.sifr.feature.calculator.domain.Operation

val memoryRow: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("MC", SifrKeyRole.Fn, CalculatorAction.MemoryClear),
    CalculatorUiAction("M+", SifrKeyRole.Fn, CalculatorAction.MemoryAdd),
    CalculatorUiAction("M−", SifrKeyRole.Fn, CalculatorAction.MemorySubtract),
    CalculatorUiAction("MR", SifrKeyRole.Fn, CalculatorAction.MemoryRecall),
)

/**
 * Scientific row cells, compact layout (a 4-column grid).
 *
 * The `deg`/`rad` cells are mutually exclusive — only one is rendered at a
 * time, picked by the current angleUnit. The keypad consumer filters them
 * before chunking.
 */
val scientificCells: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("sin", SifrKeyRole.Fn, CalculatorAction.Function("sin")),
    CalculatorUiAction("cos", SifrKeyRole.Fn, CalculatorAction.Function("cos")),
    CalculatorUiAction("tan", SifrKeyRole.Fn, CalculatorAction.Function("tan")),
    CalculatorUiAction("ln", SifrKeyRole.Fn, CalculatorAction.Function("ln")),
    CalculatorUiAction("log", SifrKeyRole.Fn, CalculatorAction.Function("log")),
    CalculatorUiAction("π", SifrKeyRole.Fn, CalculatorAction.Constant(ConstantSymbol.PI)),
    CalculatorUiAction("e", SifrKeyRole.Fn, CalculatorAction.Constant(ConstantSymbol.E)),
    CalculatorUiAction("√", SifrKeyRole.Fn, CalculatorAction.Function("sqrt")),
    CalculatorUiAction("x^y", SifrKeyRole.Fn, CalculatorAction.Op(Operation.POWER)),
    CalculatorUiAction("x!", SifrKeyRole.Fn, CalculatorAction.Factorial),
    CalculatorUiAction("asin", SifrKeyRole.Fn, CalculatorAction.Function("asin")),
    CalculatorUiAction("acos", SifrKeyRole.Fn, CalculatorAction.Function("acos")),
    CalculatorUiAction("atan", SifrKeyRole.Fn, CalculatorAction.Function("atan")),
    CalculatorUiAction("exp", SifrKeyRole.Fn, CalculatorAction.Function("exp")),
    // Angle-unit toggle — the displayed label is the CURRENT unit. The
    // keypad consumer filters to only emit the matching cell.
    CalculatorUiAction("deg", SifrKeyRole.Fn, CalculatorAction.ToggleAngleUnit),
    CalculatorUiAction("rad", SifrKeyRole.Fn, CalculatorAction.ToggleAngleUnit),
)

val basicRows: List<CalculatorUiAction> = listOf(
    CalculatorUiAction("AC", SifrKeyRole.Fn, CalculatorAction.Clear),
    CalculatorUiAction("()", SifrKeyRole.Fn, CalculatorAction.Parentheses),
    CalculatorUiAction("%", SifrKeyRole.Fn, CalculatorAction.Op(Operation.PERCENT)),
    CalculatorUiAction("÷", SifrKeyRole.Op, CalculatorAction.Op(Operation.DIVIDE)),
    CalculatorUiAction("7", SifrKeyRole.Num, CalculatorAction.Number(7)),
    CalculatorUiAction("8", SifrKeyRole.Num, CalculatorAction.Number(8)),
    CalculatorUiAction("9", SifrKeyRole.Num, CalculatorAction.Number(9)),
    CalculatorUiAction("x", SifrKeyRole.Op, CalculatorAction.Op(Operation.MULTIPLY)),
    CalculatorUiAction("4", SifrKeyRole.Num, CalculatorAction.Number(4)),
    CalculatorUiAction("5", SifrKeyRole.Num, CalculatorAction.Number(5)),
    CalculatorUiAction("6", SifrKeyRole.Num, CalculatorAction.Number(6)),
    CalculatorUiAction("-", SifrKeyRole.Op, CalculatorAction.Op(Operation.SUBTRACT)),
    CalculatorUiAction("1", SifrKeyRole.Num, CalculatorAction.Number(1)),
    CalculatorUiAction("2", SifrKeyRole.Num, CalculatorAction.Number(2)),
    CalculatorUiAction("3", SifrKeyRole.Num, CalculatorAction.Number(3)),
    CalculatorUiAction("+", SifrKeyRole.Op, CalculatorAction.Op(Operation.ADD)),
    CalculatorUiAction("0", SifrKeyRole.Num, CalculatorAction.Number(0)),
    CalculatorUiAction(".", SifrKeyRole.Num, CalculatorAction.Decimal),
    CalculatorUiAction(
        text = null,
        content = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = null,
                tint = SifrTokens.colors.keyNum.content,
            )
        },
        role = SifrKeyRole.Num,
        action = CalculatorAction.Delete,
    ),
    CalculatorUiAction("=", SifrKeyRole.Eq, CalculatorAction.Calculate),
)

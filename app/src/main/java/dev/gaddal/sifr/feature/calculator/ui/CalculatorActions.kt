package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.ConstantSymbol
import dev.gaddal.sifr.feature.calculator.domain.Operation

/** A keypad cell with an optional column span (Remix's `0` spans 2). */
data class KeypadCell(
    val action: CalculatorUiAction,
    val span: Int = 1,
)

// ---- Shared basic-key descriptors (one source of truth for every layout) ----
private fun digitKey(n: Int) =
    CalculatorUiAction(n.toString(), SifrKeyRole.Num, CalculatorAction.Number(n))

private val key0 = digitKey(0)
private val key1 = digitKey(1)
private val key2 = digitKey(2)
private val key3 = digitKey(3)
private val key4 = digitKey(4)
private val key5 = digitKey(5)
private val key6 = digitKey(6)
private val key7 = digitKey(7)
private val key8 = digitKey(8)
private val key9 = digitKey(9)

private val keyClear = CalculatorUiAction("AC", SifrKeyRole.Fn, CalculatorAction.Clear)
private val keyParens = CalculatorUiAction("()", SifrKeyRole.Fn, CalculatorAction.Parentheses)
private val keyPercent = CalculatorUiAction("%", SifrKeyRole.Fn, CalculatorAction.Op(Operation.PERCENT))
private val keyDivide = CalculatorUiAction("÷", SifrKeyRole.Op, CalculatorAction.Op(Operation.DIVIDE))
private val keyMultiply = CalculatorUiAction("x", SifrKeyRole.Op, CalculatorAction.Op(Operation.MULTIPLY))
private val keySubtract = CalculatorUiAction("-", SifrKeyRole.Op, CalculatorAction.Op(Operation.SUBTRACT))
private val keyAdd = CalculatorUiAction("+", SifrKeyRole.Op, CalculatorAction.Op(Operation.ADD))
private val keyDecimal = CalculatorUiAction(".", SifrKeyRole.Num, CalculatorAction.Decimal)
private val keyEquals = CalculatorUiAction("=", SifrKeyRole.Eq, CalculatorAction.Calculate)
private val keyDelete = CalculatorUiAction(
    text = null,
    role = SifrKeyRole.Num,
    action = CalculatorAction.Delete,
    content = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = null,
            tint = SifrTokens.colors.keyNum.content,
        )
    },
)

/** Classic basic block as a flat list — still consumed by the landscape screen. */
val basicRows: List<CalculatorUiAction> = listOf(
    keyClear, keyParens, keyPercent, keyDivide,
    key7, key8, key9, keyMultiply,
    key4, key5, key6, keySubtract,
    key1, key2, key3, keyAdd,
    key0, keyDecimal, keyDelete, keyEquals,
)

/** Classic basic block as span-aware rows (all span 1). */
val classicBasicRows: List<List<KeypadCell>> =
    basicRows.chunked(4).map { row -> row.map { KeypadCell(it) } }

/**
 * Remix basic block (spec §5): top row swaps `()` → `⌫`; `0` spans 2 cols;
 * `=` takes the old backspace slot. NO parentheses key (decision #4 — parens
 * remain in Scientific mode and in Classic).
 */
val remixBasicRows: List<List<KeypadCell>> = listOf(
    listOf(KeypadCell(keyClear), KeypadCell(keyPercent), KeypadCell(keyDelete), KeypadCell(keyDivide)),
    listOf(KeypadCell(key7), KeypadCell(key8), KeypadCell(key9), KeypadCell(keyMultiply)),
    listOf(KeypadCell(key4), KeypadCell(key5), KeypadCell(key6), KeypadCell(keySubtract)),
    listOf(KeypadCell(key1), KeypadCell(key2), KeypadCell(key3), KeypadCell(keyAdd)),
    listOf(KeypadCell(key0, span = 2), KeypadCell(keyDecimal), KeypadCell(keyEquals)),
)

/** Arc number pad (3-col, spec §5). Operators + `=` are drawn separately as circles. */
val arcNumberPadRows: List<List<KeypadCell>> = listOf(
    listOf(KeypadCell(key7), KeypadCell(key8), KeypadCell(key9)),
    listOf(KeypadCell(key4), KeypadCell(key5), KeypadCell(key6)),
    listOf(KeypadCell(key1), KeypadCell(key2), KeypadCell(key3)),
    listOf(KeypadCell(key0), KeypadCell(keyDecimal), KeypadCell(keyDelete)),
    listOf(KeypadCell(keyClear, span = 2), KeypadCell(keyPercent)),
)

/** Arc operators (bottom-end circles) + the oversized `=` circle. */
val arcOperators: List<CalculatorUiAction> = listOf(keyDivide, keyMultiply, keySubtract, keyAdd)
val arcEquals: CalculatorUiAction = keyEquals

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
    CalculatorUiAction("deg", SifrKeyRole.Fn, CalculatorAction.ToggleAngleUnit),
    CalculatorUiAction("rad", SifrKeyRole.Fn, CalculatorAction.ToggleAngleUnit),
)

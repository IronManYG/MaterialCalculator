package dev.gaddal.sifr.feature.calculator.domain

import dev.gaddal.sifr.core.domain.util.EmptyResult
import dev.gaddal.sifr.core.domain.util.Result
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

class ExpressionWriter {

    var expression = ""
        private set

    var cursor: Int = 0
        private set

    // Anchor of the current selection. Equals `cursor` when there's no range
    // (collapsed). Diverges from `cursor` only when the UI dispatches a
    // SelectionChanged action — which happens only under `rangeSelectionEnabled`.
    // Insertions and deletions replace `min(cursor, selectionStart) ..
    // max(cursor, selectionStart)` and collapse the selection back.
    var selectionStart: Int = 0
        private set

    // Forwarded to ExpressionEvaluator at calculate() / tryEvaluate() time so
    // degree-mode trig actually produces degree-mode results. Mutated by the
    // VM in response to SettingsRepository observations.
    var angleUnit: AngleUnit = AngleUnit.Degrees

    private val selectionLow get() = minOf(cursor, selectionStart)
    private val selectionHigh get() = maxOf(cursor, selectionStart)
    private val hasRange get() = cursor != selectionStart

    private var lastWasError = false

    fun processAction(action: CalculatorAction): EmptyResult<CalcError> {
        if (lastWasError && action !is CalculatorAction.Calculate) {
            expression = ""
            cursor = 0
            selectionStart = 0
            lastWasError = false
        }
        return when (action) {
            CalculatorAction.Calculate -> calculate()
            CalculatorAction.Clear -> {
                expression = ""
                cursor = 0
                selectionStart = 0
                Result.Success(Unit)
            }
            CalculatorAction.Decimal -> {
                if (canEnterDecimal()) insertAtCursor(".")
                Result.Success(Unit)
            }
            CalculatorAction.Delete -> {
                if (hasRange) {
                    expression = expression.removeRange(selectionLow, selectionHigh)
                    cursor = selectionLow
                    selectionStart = cursor
                } else if (cursor > 0) {
                    val deleteStart = computeSmartDeleteStart()
                    expression = expression.removeRange(deleteStart, cursor)
                    cursor = deleteStart
                    selectionStart = cursor
                }
                Result.Success(Unit)
            }
            is CalculatorAction.Number -> {
                insertAtCursor(action.number.toString())
                Result.Success(Unit)
            }
            is CalculatorAction.Op -> {
                if (canEnterOperation(action.operation)) {
                    insertAtCursor(action.operation.symbol.toString())
                }
                Result.Success(Unit)
            }
            CalculatorAction.Parentheses -> {
                processParentheses()
                Result.Success(Unit)
            }
            is CalculatorAction.CursorChanged -> {
                val clamped = action.newPosition.coerceIn(0, expression.length)
                cursor = clamped
                selectionStart = clamped
                Result.Success(Unit)
            }
            is CalculatorAction.SelectionChanged -> {
                selectionStart = action.start.coerceIn(0, expression.length)
                cursor = action.end.coerceIn(0, expression.length)
                Result.Success(Unit)
            }
            is CalculatorAction.Function -> {
                if (canEnterFactor()) insertAtCursor("${action.name}(")
                Result.Success(Unit)
            }
            is CalculatorAction.Constant -> {
                if (canEnterFactor()) insertAtCursor(action.symbol.symbol.toString())
                Result.Success(Unit)
            }
            CalculatorAction.Factorial -> {
                if (canEnterPostfix()) insertAtCursor("!")
                Result.Success(Unit)
            }
            is CalculatorAction.InsertText -> {
                insertAtCursor(action.text)
                Result.Success(Unit)
            }
            CalculatorAction.ToggleMode,
            CalculatorAction.ToggleAngleUnit,
            CalculatorAction.MemoryClear,
            CalculatorAction.MemoryAdd,
            CalculatorAction.MemorySubtract,
            CalculatorAction.MemoryRecall -> Result.Success(Unit) // handled by VM, no writer mutation
            CalculatorAction.SettingsClicked -> Result.Success(Unit)
            CalculatorAction.HistoryClicked -> Result.Success(Unit)
            is CalculatorAction.RestoreExpression -> {
                expression = action.value
                cursor = expression.length
                selectionStart = cursor
                Result.Success(Unit)
            }
        }
    }

    private fun insertAtCursor(text: String) {
        // Replaces any selected range with `text` and collapses the selection
        // at the new caret position. When the selection is already collapsed
        // (the common typing case), selectionLow == selectionHigh == cursor
        // and this reduces to the original "insert at cursor" behavior.
        val low = selectionLow
        val high = selectionHigh
        expression = expression.substring(0, low) + text + expression.substring(high)
        cursor = low + text.length
        selectionStart = cursor
    }

    private fun calculate(): EmptyResult<CalcError> {
        if (lastWasError) {
            return Result.Error(CalcError.INVALID_EXPRESSION)
        }
        return try {
            val parser = ExpressionParser(prepareForCalculation())
            val result = ExpressionEvaluator(parser.parse(), angleUnit).evaluate()
            if (result.isFinite()) {
                expression = formatResult(result)
                cursor = expression.length
                selectionStart = cursor
                Result.Success(Unit)
            } else {
                lastWasError = true
                Result.Error(CalcError.DIVISION_BY_ZERO)
            }
        } catch (e: DomainErrorException) {
            lastWasError = true
            Result.Error(CalcError.DOMAIN_ERROR)
        } catch (e: OverflowException) {
            lastWasError = true
            Result.Error(CalcError.OVERFLOW)
        } catch (e: SyntaxErrorException) {
            lastWasError = true
            Result.Error(CalcError.SYNTAX_ERROR)
        } catch (_: Exception) {
            lastWasError = true
            Result.Error(CalcError.INVALID_EXPRESSION)
        }
    }

    /**
     * Restores the writer to a previously-captured `(expression, cursor)`
     * pair without going through the action pipeline — used on process-death
     * recovery so the ViewModel can re-hydrate the same writer state that
     * the user left off in. The cursor is clamped to the expression's bounds
     * and any prior error flag is cleared (the writer is treated as a fresh
     * surface on restore; if the saved expression is still invalid, the next
     * Calculate will re-surface the error normally).
     */
    fun restoreState(expression: String, cursor: Int, selectionStart: Int = cursor) {
        this.expression = expression
        this.cursor = cursor.coerceIn(0, expression.length)
        this.selectionStart = selectionStart.coerceIn(0, expression.length)
        this.lastWasError = false
    }

    fun tryEvaluate(): Result<String, CalcError> {
        if (lastWasError) return Result.Error(CalcError.INVALID_EXPRESSION)
        return try {
            val parser = ExpressionParser(prepareForCalculation())
            val result = ExpressionEvaluator(parser.parse(), angleUnit).evaluate()
            if (result.isFinite()) Result.Success(formatResult(result))
            else Result.Error(CalcError.DIVISION_BY_ZERO)
        } catch (e: DomainErrorException) {
            Result.Error(CalcError.DOMAIN_ERROR)
        } catch (e: OverflowException) {
            Result.Error(CalcError.OVERFLOW)
        } catch (e: SyntaxErrorException) {
            Result.Error(CalcError.SYNTAX_ERROR)
        } catch (_: Exception) {
            Result.Error(CalcError.INVALID_EXPRESSION)
        }
    }

    private fun prepareForCalculation(): String {
        val newExpression = expression.dropLastWhile {
            it in "$operationSymbols(."
        }
        if (newExpression.isEmpty()) {
            return "0"
        }
        return newExpression
    }

    private fun processParentheses() {
        val openingCount = expression.count { it == '(' }
        val closingCount = expression.count { it == ')' }
        val effectiveStart = selectionLow
        val prevChar = expression.getOrNull(effectiveStart - 1)
        val toInsert = when {
            prevChar == null || prevChar in "$operationSymbols(" -> "("
            prevChar in "0123456789)" && openingCount == closingCount -> return
            else -> ")"
        }
        insertAtCursor(toInsert)
    }

    private fun canEnterDecimal(): Boolean {
        // Effective insertion point is the start of any selected range — the
        // range will be replaced by the inserted character. Forward walk picks
        // up at selectionHigh to see what would remain after deletion.
        val effectiveStart = selectionLow
        val prevChar = expression.getOrNull(effectiveStart - 1) ?: return false
        if (prevChar in "$operationSymbols.()" || prevChar == 'π' || prevChar == 'e' || prevChar == '!') return false
        var i = effectiveStart - 1
        while (i >= 0 && expression[i] in "0123456789.") {
            if (expression[i] == '.') return false
            i--
        }
        var j = selectionHigh
        while (j < expression.length && expression[j] in "0123456789.") {
            if (expression[j] == '.') return false
            j++
        }
        return true
    }

    private fun canEnterOperation(operation: Operation): Boolean {
        val prevChar = expression.getOrNull(selectionLow - 1)
        if (operation in listOf(Operation.ADD, Operation.SUBTRACT)) {
            return prevChar == null || prevChar in "$operationSymbols()0123456789"
        }
        return prevChar != null && prevChar in "0123456789)"
    }

    private fun canEnterFactor(): Boolean {
        // A factor (function call, constant, or number) can be inserted when
        // the cursor sits at the start, after an operator, after '(', or
        // after a postfix '!' (which already terminates a previous factor).
        val prev = expression.getOrNull(selectionLow - 1) ?: return true
        return prev in "$operationSymbols(" || prev == '!'
    }

    private fun canEnterPostfix(): Boolean {
        // '!' is postfix - only valid after something that produced a value:
        // a digit, ')', a constant symbol (π, e), or another '!' (5!! = 120!).
        val prev = expression.getOrNull(selectionLow - 1) ?: return false
        return prev.isDigit() || prev == ')' || prev == 'π' || prev == 'e' || prev == '!'
    }

    /**
     * Where the next Delete should start removing characters from.
     *
     * `CalculatorAction.Function` inserts `"sin("` in one shot, so Delete
     * should peel that same token off in one shot. Only one smart case:
     *
     *   Cursor right after `(` preceded by function-name letters → remove
     *   the whole `funcname(` prefix.
     *
     * Everything else — closing parens, digits inside args, plain parens,
     * operators, constants, factorial — falls through to the original
     * one-character delete. That keeps the user in control of the args
     * and the closing `)` when they want to edit a sub-expression.
     */
    private fun computeSmartDeleteStart(): Int {
        val prev = expression.getOrNull(cursor - 1) ?: return cursor
        return when (prev) {
            '(' -> {
                val funcStart = findFunctionNameStart(cursor - 1)
                if (funcStart < cursor - 1) funcStart else cursor - 1
            }
            else -> cursor - 1
        }
    }

    /**
     * Walk back from just before [openParenIndex] over identifier letters
     * (a-z A-Z, excluding operation glyphs like `'x'` for multiply that
     * `Char.isLetter()` would otherwise consume). Returns the index where
     * the function name starts; if no letters precede `(`, returns
     * [openParenIndex] (meaning "no function name here").
     */
    private fun findFunctionNameStart(openParenIndex: Int): Int {
        var i = openParenIndex - 1
        while (i >= 0 && expression[i].isLetter() && expression[i] !in operationSymbols) {
            i--
        }
        return i + 1
    }

    private fun formatResult(value: Double): String {
        // Caller already gates on isFinite; defensive in case a future path skips it.
        if (!value.isFinite()) return value.toString()
        if (value == 0.0) return "0"

        val absValue = abs(value)
        return if (absValue >= SCI_UPPER_THRESHOLD || absValue < SCI_LOWER_THRESHOLD) {
            formatScientific(value)
        } else {
            formatFixed(value)
        }
    }

    // Caps total significant digits at the limit, distributing them between integer
    // and fractional parts. For |x| in [SCI_LOWER_THRESHOLD, 1) the leading zeros
    // after the decimal point are not significant digits, so the budget extends to
    // capture every meaningful digit. Trailing zeros and a hanging dot are trimmed.
    private fun formatFixed(value: Double): String {
        val absValue = abs(value)
        val fractionDigits = if (absValue >= 1.0) {
            val integerDigits = floor(log10(absValue)).toInt() + 1
            (MAX_SIG_DIGITS - integerDigits).coerceAtLeast(0)
        } else {
            val leadingZeros = -floor(log10(absValue)).toInt() - 1
            leadingZeros + MAX_SIG_DIGITS
        }
        val raw = String.format(Locale.ROOT, "%.${fractionDigits}f", value)
        return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }

    // Emits "<mantissa>E<exponent>" with up to MAX_SIG_DIGITS significant digits in
    // the mantissa, sign on negative exponents only (Google Calculator style).
    private fun formatScientific(value: Double): String {
        val raw = String.format(Locale.ROOT, "%.${MAX_SIG_DIGITS - 1}e", value)
        val eIndex = raw.indexOfAny(charArrayOf('e', 'E'))
        val mantissaRaw = raw.substring(0, eIndex)
        val exponent = raw.substring(eIndex + 1).toInt()
        val mantissa = if (mantissaRaw.contains('.')) {
            mantissaRaw.trimEnd('0').trimEnd('.')
        } else {
            mantissaRaw
        }
        return "${mantissa}E$exponent"
    }

    companion object {
        // IEEE 754 double reliably represents ~15-17 significant decimal digits;
        // 16 is the honest cap where every printed digit is computable.
        private const val MAX_SIG_DIGITS = 16

        // Beyond 10^16 integers stop being exactly representable in Double, so we
        // switch to scientific to avoid silently emitting garbage low-order digits.
        private const val SCI_UPPER_THRESHOLD = 1e16

        // Below 10^-9 fixed-point gets visually unwieldy (many leading zeros) and
        // ambiguous with rounding noise; scientific is more honest.
        private const val SCI_LOWER_THRESHOLD = 1e-9
    }
}

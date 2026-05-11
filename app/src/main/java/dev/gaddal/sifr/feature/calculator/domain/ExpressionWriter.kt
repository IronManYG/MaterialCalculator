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

    private var lastWasError = false

    fun processAction(action: CalculatorAction): EmptyResult<CalcError> {
        if (lastWasError && action !is CalculatorAction.Calculate) {
            expression = ""
            cursor = 0
            lastWasError = false
        }
        return when (action) {
            CalculatorAction.Calculate -> calculate()
            CalculatorAction.Clear -> {
                expression = ""
                cursor = 0
                Result.Success(Unit)
            }
            CalculatorAction.Decimal -> {
                if (canEnterDecimal()) insertAtCursor(".")
                Result.Success(Unit)
            }
            CalculatorAction.Delete -> {
                if (cursor > 0) {
                    expression = expression.removeRange(cursor - 1, cursor)
                    cursor--
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
                cursor = action.newPosition.coerceIn(0, expression.length)
                Result.Success(Unit)
            }
            CalculatorAction.SettingsClicked -> Result.Success(Unit)
            CalculatorAction.HistoryClicked -> Result.Success(Unit)
            is CalculatorAction.RestoreExpression -> {
                expression = action.value
                cursor = expression.length
                Result.Success(Unit)
            }
        }
    }

    private fun insertAtCursor(text: String) {
        expression = expression.substring(0, cursor) + text + expression.substring(cursor)
        cursor += text.length
    }

    private fun calculate(): EmptyResult<CalcError> {
        if (lastWasError) {
            return Result.Error(CalcError.INVALID_EXPRESSION)
        }
        return try {
            val parser = ExpressionParser(prepareForCalculation())
            val result = ExpressionEvaluator(parser.parse()).evaluate()
            if (result.isFinite()) {
                expression = formatResult(result)
                cursor = expression.length
                Result.Success(Unit)
            } else {
                lastWasError = true
                Result.Error(CalcError.DIVISION_BY_ZERO)
            }
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
    fun restoreState(expression: String, cursor: Int) {
        this.expression = expression
        this.cursor = cursor.coerceIn(0, expression.length)
        this.lastWasError = false
    }

    fun tryEvaluate(): Result<String, CalcError> {
        if (lastWasError) return Result.Error(CalcError.INVALID_EXPRESSION)
        return try {
            val parser = ExpressionParser(prepareForCalculation())
            val result = ExpressionEvaluator(parser.parse()).evaluate()
            if (result.isFinite()) Result.Success(formatResult(result))
            else Result.Error(CalcError.DIVISION_BY_ZERO)
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
        val prevChar = expression.getOrNull(cursor - 1)
        val toInsert = when {
            prevChar == null || prevChar in "$operationSymbols(" -> "("
            prevChar in "0123456789)" && openingCount == closingCount -> return
            else -> ")"
        }
        insertAtCursor(toInsert)
    }

    private fun canEnterDecimal(): Boolean {
        val prevChar = expression.getOrNull(cursor - 1) ?: return false
        if (prevChar in "$operationSymbols.()") return false
        // The current number-group extends both backward and forward from the cursor.
        // Reject if either direction already contains a decimal point.
        var i = cursor - 1
        while (i >= 0 && expression[i] in "0123456789.") {
            if (expression[i] == '.') return false
            i--
        }
        var j = cursor
        while (j < expression.length && expression[j] in "0123456789.") {
            if (expression[j] == '.') return false
            j++
        }
        return true
    }

    private fun canEnterOperation(operation: Operation): Boolean {
        val prevChar = expression.getOrNull(cursor - 1)
        if (operation in listOf(Operation.ADD, Operation.SUBTRACT)) {
            return prevChar == null || prevChar in "$operationSymbols()0123456789"
        }
        return prevChar != null && prevChar in "0123456789)"
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

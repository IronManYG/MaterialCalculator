package dev.gaddal.sifr.feature.calculator.domain

import dev.gaddal.sifr.core.domain.util.EmptyResult
import dev.gaddal.sifr.core.domain.util.Result
import java.util.Locale

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
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return String.format(Locale.ROOT, "%.10f", value).trimEnd('0').trimEnd('.')
    }
}

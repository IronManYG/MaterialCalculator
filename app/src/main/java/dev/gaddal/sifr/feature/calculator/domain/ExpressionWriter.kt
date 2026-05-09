package dev.gaddal.sifr.feature.calculator.domain

import dev.gaddal.sifr.core.domain.util.EmptyResult
import dev.gaddal.sifr.core.domain.util.Result
import java.util.Locale

class ExpressionWriter {

    var expression = ""
        private set

    private var lastWasError = false

    fun processAction(action: CalculatorAction): EmptyResult<CalcError> {
        if (lastWasError && action !is CalculatorAction.Calculate) {
            expression = ""
            lastWasError = false
        }
        return when (action) {
            CalculatorAction.Calculate -> calculate()
            CalculatorAction.Clear -> {
                expression = ""
                Result.Success(Unit)
            }
            CalculatorAction.Decimal -> {
                if (canEnterDecimal()) expression += "."
                Result.Success(Unit)
            }
            CalculatorAction.Delete -> {
                expression = expression.dropLast(1)
                Result.Success(Unit)
            }
            is CalculatorAction.Number -> {
                expression += action.number
                Result.Success(Unit)
            }
            is CalculatorAction.Op -> {
                if (canEnterOperation(action.operation)) expression += action.operation.symbol
                Result.Success(Unit)
            }
            CalculatorAction.Parentheses -> {
                processParentheses()
                Result.Success(Unit)
            }
            CalculatorAction.SettingsClicked -> Result.Success(Unit)
            CalculatorAction.HistoryClicked -> Result.Success(Unit)
            is CalculatorAction.RestoreExpression -> {
                expression = action.value
                Result.Success(Unit)
            }
        }
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
        expression += when {
            expression.isEmpty() ||
                    expression.last() in "$operationSymbols(" -> "("
            expression.last() in "0123456789)" &&
                    openingCount == closingCount -> return
            else -> ")"
        }
    }

    private fun canEnterDecimal(): Boolean {
        if (expression.isEmpty() || expression.last() in "$operationSymbols.()") {
            return false
        }
        return !expression.takeLastWhile {
            it in "0123456789."
        }.contains(".")
    }

    private fun canEnterOperation(operation: Operation): Boolean {
        if (operation in listOf(Operation.ADD, Operation.SUBTRACT)) {
            return expression.isEmpty() || expression.last() in "$operationSymbols()0123456789"
        }
        return expression.isNotEmpty() && expression.last() in "0123456789)"
    }

    private fun formatResult(value: Double): String {
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return String.format(Locale.ROOT, "%.10f", value).trimEnd('0').trimEnd('.')
    }
}

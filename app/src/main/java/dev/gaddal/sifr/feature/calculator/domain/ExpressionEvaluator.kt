package dev.gaddal.sifr.feature.calculator.domain

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal class DomainErrorException : RuntimeException("Function input outside domain")
internal class OverflowException : RuntimeException("Numeric overflow")
internal class SyntaxErrorException : RuntimeException("Syntax error")

/**
 * Recursive-descent evaluator. Grammar (precedence low to high):
 *
 *   expression -> term (+- term)*
 *   term       -> power (x|/|% power)*
 *   power      -> unary (^ power)?            right-associative
 *   unary      -> +-unary | postfix
 *   postfix    -> primary (!)*
 *   primary    -> number | constant | function | (expression)
 */
class ExpressionEvaluator(
    private val expression: List<ExpressionPart>,
    private val angleUnit: AngleUnit = AngleUnit.Radians,
) {

    fun evaluate(): Double {
        return evalExpression(expression).value
    }

    private fun evalExpression(parts: List<ExpressionPart>): ExpressionResult {
        val first = evalTerm(parts)
        var remaining = first.remainingExpression
        var sum = first.value
        while (true) {
            when (remaining.firstOrNull()) {
                ExpressionPart.Op(Operation.ADD) -> {
                    val t = evalTerm(remaining.drop(1))
                    sum += t.value; remaining = t.remainingExpression
                }
                ExpressionPart.Op(Operation.SUBTRACT) -> {
                    val t = evalTerm(remaining.drop(1))
                    sum -= t.value; remaining = t.remainingExpression
                }
                else -> return ExpressionResult(remaining, sum)
            }
        }
    }

    private fun evalTerm(parts: List<ExpressionPart>): ExpressionResult {
        val first = evalPower(parts)
        var remaining = first.remainingExpression
        var sum = first.value
        while (true) {
            when (remaining.firstOrNull()) {
                ExpressionPart.Op(Operation.MULTIPLY) -> {
                    val p = evalPower(remaining.drop(1))
                    sum *= p.value; remaining = p.remainingExpression
                }
                ExpressionPart.Op(Operation.DIVIDE) -> {
                    val p = evalPower(remaining.drop(1))
                    sum /= p.value; remaining = p.remainingExpression
                }
                ExpressionPart.Op(Operation.PERCENT) -> {
                    val p = evalPower(remaining.drop(1))
                    sum *= (p.value / 100.0); remaining = p.remainingExpression
                }
                else -> return ExpressionResult(remaining, sum)
            }
        }
    }

    // Power is right-associative: a^b^c = a^(b^c). Recurse on the right.
    private fun evalPower(parts: List<ExpressionPart>): ExpressionResult {
        val base = evalUnary(parts)
        return if (base.remainingExpression.firstOrNull() == ExpressionPart.Op(Operation.POWER)) {
            val exponent = evalPower(base.remainingExpression.drop(1))
            ExpressionResult(exponent.remainingExpression, base.value.pow(exponent.value))
        } else {
            base
        }
    }

    private fun evalUnary(parts: List<ExpressionPart>): ExpressionResult {
        return evalPostfix(parts)
    }

    /**
     * Handles optional leading unary sign, then a primary, then zero or more postfix ops.
     * The sign is applied to the base value BEFORE postfix ops so that
     * [SUBTRACT, Number(3), FACTORIAL] evaluates factorial(-3) → domain error,
     * not -(factorial(3)) → -6.
     */
    private fun evalPostfix(parts: List<ExpressionPart>): ExpressionResult {
        // Consume an optional leading unary +/-
        val (sign, rest) = when (parts.firstOrNull()) {
            ExpressionPart.Op(Operation.ADD) -> Pair(1.0, parts.drop(1))
            ExpressionPart.Op(Operation.SUBTRACT) -> Pair(-1.0, parts.drop(1))
            else -> Pair(1.0, parts)
        }
        var inner = evalPrimary(rest)
        val signedValue = sign * inner.value
        inner = ExpressionResult(inner.remainingExpression, signedValue)
        while (inner.remainingExpression.firstOrNull() == ExpressionPart.Postfix(PostfixOp.FACTORIAL)) {
            inner = ExpressionResult(inner.remainingExpression.drop(1), factorial(inner.value))
        }
        return inner
    }

    private fun evalPrimary(parts: List<ExpressionPart>): ExpressionResult {
        return when (val part = parts.firstOrNull()) {
            is ExpressionPart.Number -> ExpressionResult(parts.drop(1), part.number)
            is ExpressionPart.Constant -> ExpressionResult(parts.drop(1), part.symbol.value)
            is ExpressionPart.Function -> evalFunctionCall(part.name, parts.drop(1))
            ExpressionPart.Parentheses(ParenthesesType.Opening) -> {
                val inner = evalExpression(parts.drop(1))
                // drop the closing paren
                ExpressionResult(inner.remainingExpression.drop(1), inner.value)
            }
            ExpressionPart.Op(Operation.PERCENT) -> evalTerm(parts.drop(1))
            else -> throw SyntaxErrorException()
        }
    }

    private fun evalFunctionCall(name: String, after: List<ExpressionPart>): ExpressionResult {
        if (after.firstOrNull() != ExpressionPart.Parentheses(ParenthesesType.Opening)) {
            throw SyntaxErrorException()
        }
        val inner = evalExpression(after.drop(1))
        if (inner.remainingExpression.firstOrNull() != ExpressionPart.Parentheses(ParenthesesType.Closing)) {
            throw SyntaxErrorException()
        }
        val value = applyFunction(name, inner.value)
        return ExpressionResult(inner.remainingExpression.drop(1), value)
    }

    private fun applyFunction(name: String, arg: Double): Double {
        // Forward trig: convert input to radians if we're in degree mode.
        // Inverse trig: convert output back to degrees.
        val argRad = if (angleUnit == AngleUnit.Degrees) arg * PI / 180.0 else arg
        return when (name) {
            "sin" -> sin(argRad)
            "cos" -> cos(argRad)
            "tan" -> tan(argRad)
            "asin" -> {
                if (arg < -1.0 || arg > 1.0) throw DomainErrorException()
                asin(arg).let { if (angleUnit == AngleUnit.Degrees) it * 180.0 / PI else it }
            }
            "acos" -> {
                if (arg < -1.0 || arg > 1.0) throw DomainErrorException()
                acos(arg).let { if (angleUnit == AngleUnit.Degrees) it * 180.0 / PI else it }
            }
            "atan" -> atan(arg).let { if (angleUnit == AngleUnit.Degrees) it * 180.0 / PI else it }
            "ln" -> { if (arg <= 0.0) throw DomainErrorException(); ln(arg) }
            "log" -> { if (arg <= 0.0) throw DomainErrorException(); log10(arg) }
            "exp" -> exp(arg)
            "sqrt" -> { if (arg < 0.0) throw DomainErrorException(); sqrt(arg) }
            else -> throw SyntaxErrorException()
        }
    }

    private fun factorial(value: Double): Double {
        // Reject non-integer and negative inputs as DOMAIN_ERROR. Above 170
        // the Double representation overflows; flag as OVERFLOW so the
        // ViewModel can surface a distinct error message.
        val rounded = value.roundToLong()
        if (rounded < 0L || rounded.toDouble() != value) throw DomainErrorException()
        if (rounded > 170L) throw OverflowException()
        var acc = 1.0
        for (k in 2L..rounded) acc *= k.toDouble()
        return acc
    }

    data class ExpressionResult(
        val remainingExpression: List<ExpressionPart>,
        val value: Double
    )
}

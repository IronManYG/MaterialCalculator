package dev.gaddal.sifr.feature.calculator.domain

import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.feature.calculator.domain.DomainErrorException
import dev.gaddal.sifr.feature.calculator.domain.OverflowException
import org.junit.Test

class ExpressionEvaluatorTest {

    private lateinit var evaluator: ExpressionEvaluator

    @Test
    fun `Simple expression properly evaluated`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(4.0),
                ExpressionPart.Op(Operation.ADD),
                ExpressionPart.Number(5.0),
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(3.0),
                ExpressionPart.Op(Operation.MULTIPLY),
                ExpressionPart.Number(5.0),
                ExpressionPart.Op(Operation.DIVIDE),
                ExpressionPart.Number(3.0),
            )
        )

        assertThat(evaluator.evaluate()).isEqualTo(4)
    }

    @Test
    fun `Expression with decimals properly evaluated`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(4.5),
                ExpressionPart.Op(Operation.ADD),
                ExpressionPart.Number(5.5),
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(3.5),
                ExpressionPart.Op(Operation.MULTIPLY),
                ExpressionPart.Number(5.5),
                ExpressionPart.Op(Operation.DIVIDE),
                ExpressionPart.Number(3.5),
            )
        )

        assertThat(evaluator.evaluate()).isEqualTo(4.5)
    }

    @Test
    fun `Simple equation with parentheses properly evaluated`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(4.0),
                ExpressionPart.Op(Operation.ADD),
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Number(5.0),
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(3.0),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
                ExpressionPart.Op(Operation.MULTIPLY),
                ExpressionPart.Number(5.0),
                ExpressionPart.Op(Operation.DIVIDE),
                ExpressionPart.Number(4.0),
            )
        )

        assertThat(evaluator.evaluate()).isEqualTo(6.5)
    }

    @Test
    fun `Power is right-associative`() {
        // 2^3^2 = 2^(3^2) = 2^9 = 512
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(2.0),
                ExpressionPart.Op(Operation.POWER),
                ExpressionPart.Number(3.0),
                ExpressionPart.Op(Operation.POWER),
                ExpressionPart.Number(2.0),
            )
        )
        assertThat(evaluator.evaluate()).isEqualTo(512.0)
    }

    @Test
    fun `Power has higher precedence than multiply`() {
        // 2 * 3^2 = 2 * 9 = 18
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(2.0),
                ExpressionPart.Op(Operation.MULTIPLY),
                ExpressionPart.Number(3.0),
                ExpressionPart.Op(Operation.POWER),
                ExpressionPart.Number(2.0),
            )
        )
        assertThat(evaluator.evaluate()).isEqualTo(18.0)
    }

    @Test
    fun `Factorial of 5 is 120`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(5.0),
                ExpressionPart.Postfix(PostfixOp.FACTORIAL),
            )
        )
        assertThat(evaluator.evaluate()).isEqualTo(120.0)
    }

    @Test
    fun `Factorial of 0 is 1`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(0.0),
                ExpressionPart.Postfix(PostfixOp.FACTORIAL),
            )
        )
        assertThat(evaluator.evaluate()).isEqualTo(1.0)
    }

    @Test
    fun `Pi constant evaluates`() {
        evaluator = ExpressionEvaluator(listOf(ExpressionPart.Constant(ConstantSymbol.PI)))
        assertThat(evaluator.evaluate()).isEqualTo(Math.PI)
    }

    @Test
    fun `Sin in degrees mode`() {
        evaluator = ExpressionEvaluator(
            expression = listOf(
                ExpressionPart.Function("sin"),
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Number(30.0),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
            ),
            angleUnit = AngleUnit.Degrees,
        )
        assertThat(evaluator.evaluate()).isWithin(1e-10).of(0.5)
    }

    @Test
    fun `Sin in radians mode`() {
        evaluator = ExpressionEvaluator(
            expression = listOf(
                ExpressionPart.Function("sin"),
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Constant(ConstantSymbol.PI),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
            ),
            angleUnit = AngleUnit.Radians,
        )
        assertThat(evaluator.evaluate()).isWithin(1e-10).of(0.0)
    }

    @Test
    fun `Log of 0 throws domain error`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Function("log"),
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Number(0.0),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
            )
        )
        val ex = runCatching { evaluator.evaluate() }.exceptionOrNull()
        assertThat(ex).isInstanceOf(DomainErrorException::class.java)
    }

    @Test
    fun `Sqrt of negative throws domain error`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Function("sqrt"),
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(1.0),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
            )
        )
        val ex = runCatching { evaluator.evaluate() }.exceptionOrNull()
        assertThat(ex).isInstanceOf(DomainErrorException::class.java)
    }

    @Test
    fun `Factorial of 200 overflows`() {
        evaluator = ExpressionEvaluator(
            listOf(
                ExpressionPart.Number(200.0),
                ExpressionPart.Postfix(PostfixOp.FACTORIAL),
            )
        )
        val ex = runCatching { evaluator.evaluate() }.exceptionOrNull()
        assertThat(ex).isInstanceOf(OverflowException::class.java)
    }
}
package dev.gaddal.sifr.feature.calculator.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FactorialTest {

    @Test
    fun `1 factorial is 1`() {
        val e = ExpressionEvaluator(
            listOf(ExpressionPart.Number(1.0), ExpressionPart.Postfix(PostfixOp.FACTORIAL))
        )
        assertThat(e.evaluate()).isEqualTo(1.0)
    }

    @Test
    fun `Negative factorial throws domain error`() {
        // (-3)! — with explicit parens so the sign is applied BEFORE the factorial.
        // Without parens, -3! = -(3!) = -6 by standard math precedence.
        val e = ExpressionEvaluator(
            listOf(
                ExpressionPart.Parentheses(ParenthesesType.Opening),
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(3.0),
                ExpressionPart.Parentheses(ParenthesesType.Closing),
                ExpressionPart.Postfix(PostfixOp.FACTORIAL),
            )
        )
        val ex = runCatching { e.evaluate() }.exceptionOrNull()
        assertThat(ex).isInstanceOf(DomainErrorException::class.java)
    }

    @Test
    fun `Bare negative factorial follows standard math precedence`() {
        // -3! = -(3!) = -6, NOT (-3)! which would be a domain error.
        // This guards against regressing back to Casio-style parsing.
        val e = ExpressionEvaluator(
            listOf(
                ExpressionPart.Op(Operation.SUBTRACT),
                ExpressionPart.Number(3.0),
                ExpressionPart.Postfix(PostfixOp.FACTORIAL),
            )
        )
        assertThat(e.evaluate()).isEqualTo(-6.0)
    }

    @Test
    fun `Non-integer factorial throws domain error`() {
        val e = ExpressionEvaluator(
            listOf(ExpressionPart.Number(3.5), ExpressionPart.Postfix(PostfixOp.FACTORIAL))
        )
        val ex = runCatching { e.evaluate() }.exceptionOrNull()
        assertThat(ex).isInstanceOf(DomainErrorException::class.java)
    }

    @Test
    fun `170 factorial is just under Double overflow`() {
        val e = ExpressionEvaluator(
            listOf(ExpressionPart.Number(170.0), ExpressionPart.Postfix(PostfixOp.FACTORIAL))
        )
        assertThat(e.evaluate().isFinite()).isTrue()
    }
}
